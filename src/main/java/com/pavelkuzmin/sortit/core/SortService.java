package com.pavelkuzmin.sortit.core;

import com.pavelkuzmin.sortit.config.AppConfig;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SortService {
    private static final Duration LOG_RETENTION = Duration.ofDays(1);
    private static final Pattern FILE_DATE = Pattern.compile("(20\\d{2}|19\\d{2})(\\d{2})(\\d{2})");

    public interface ProgressListener {
        void onProgress(SortProgress progress);
    }

    public record Messages(String noDate) {
        public static Messages english() {
            return new Messages("No date in metadata / file name / creation time");
        }
    }

    public SortRunResult run(AppConfig cfg, Path logDir, Messages messages, ProgressListener listener) {
        AppConfig runConfig = AppConfig.copyOf(cfg);
        Path sourceRoot = safePath(runConfig.sourceDir);
        Path destRoot = safePath(runConfig.destDir);
        Path resolvedLogDir = logDir != null ? logDir : Path.of(".").toAbsolutePath().normalize();
        Messages logMessages = messages != null ? messages : Messages.english();

        int deletedOldLogs = deleteOldLogs(resolvedLogDir, LOG_RETENTION);
        List<Path> files = collectMatching(sourceRoot, safeGlob(runConfig.filenameTemplate));
        int total = files.size();
        int processed = 0;
        int errors = 0;
        StringBuilder log = new StringBuilder();

        if (deletedOldLogs > 0) {
            log.append("Deleted old logs: ").append(deletedOldLogs).append(System.lineSeparator());
        }

        if (total == 0) {
            notifyProgress(listener, new SortProgress(0, 0, 0));
            Path logPath = writeLog(resolvedLogDir, finishLog(log, runConfig, processed, total, errors));
            return new SortRunResult(processed, total, errors, logPath, deletedOldLogs);
        }

        notifyProgress(listener, new SortProgress(0, total, 0));

        Path bakRoot = sourceRoot.resolve("BAK");
        for (Path srcFile : files) {
            String name = srcFile.getFileName().toString();
            try {
                LocalDate date = resolveDate(srcFile, runConfig);
                if (date == null) {
                    errors++;
                    logError(log, name, logMessages.noDate());
                } else {
                    String subFolder = formatByTemplate(runConfig.destTemplate, date);
                    errors += processFile(runConfig.mode, srcFile, destRoot.resolve(subFolder), bakRoot.resolve(subFolder), log);
                }
            } catch (Exception ex) {
                errors++;
                logError(log, name, messageOf(ex));
            }

            processed++;
            notifyProgress(listener, new SortProgress(processed, total, errors));
        }

        Path logPath = writeLog(resolvedLogDir, finishLog(log, runConfig, processed, total, errors));
        return new SortRunResult(processed, total, errors, logPath, deletedOldLogs);
    }

    public static long countAny(Path dir) {
        if (!isRealDir(dir)) return 0L;
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return 0L;
        }
    }

    public static long countMatching(Path dir, String glob) {
        if (!isRealDir(dir)) return 0L;
        try (Stream<Path> s = Files.list(dir)) {
            PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:" + safeGlob(glob));
            return s.filter(Files::isRegularFile)
                    .filter(path -> matcher.matches(path.getFileName()))
                    .count();
        } catch (IOException | IllegalArgumentException e) {
            return 0L;
        }
    }

    public static List<Path> collectMatching(Path dir, String glob) {
        if (!isRealDir(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:" + safeGlob(glob));
            return s.filter(Files::isRegularFile)
                    .filter(path -> matcher.matches(path.getFileName()))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException | IllegalArgumentException e) {
            return List.of();
        }
    }

    public static Path safePath(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Path.of(value);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isRealDir(Path path) {
        try {
            return path != null && Files.isDirectory(path);
        } catch (Exception ignore) {
            return false;
        }
    }

    public static String safeGlob(String glob) {
        return (glob == null || glob.isBlank()) ? "*.*" : glob.trim();
    }

    static int deleteOldLogs(Path logDir, Duration retention) {
        if (!isRealDir(logDir)) return 0;
        Instant cutoff = Instant.now().minus(retention);
        int deleted = 0;
        try (Stream<Path> files = Files.list(logDir)) {
            for (Path file : files.toList()) {
                if (!Files.isRegularFile(file)) continue;
                if (!file.getFileName().toString().matches("sort-\\d{8}-\\d{4,6}\\.log")) continue;
                try {
                    if (Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                        deleted++;
                    }
                } catch (IOException ignore) {
                    // Log cleanup should never block sorting.
                }
            }
        } catch (IOException ignore) {
            return deleted;
        }
        return deleted;
    }

    static LocalDate resolveDate(Path file, AppConfig cfg) {
        return switch (cfg.dateSource) {
            case METADATA -> {
                Date date = MediaDateExtractor.getBestDate(file.toFile());
                yield date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            case FILENAME -> parseDateFromFilename(file.getFileName().toString());
            case CREATED -> {
                try {
                    var attr = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class);
                    Instant instant = attr.creationTime() != null ? attr.creationTime().toInstant() : null;
                    if (instant == null && attr.lastModifiedTime() != null) {
                        instant = attr.lastModifiedTime().toInstant();
                    }
                    yield instant == null ? null : instant.atZone(ZoneId.systemDefault()).toLocalDate();
                } catch (Exception ex) {
                    yield null;
                }
            }
        };
    }

    static String formatByTemplate(String template, LocalDate date) {
        if (template == null || template.isBlank()) return "UNKNOWN";
        String out = template;
        out = out.replace("YYYY", String.format("%04d", date.getYear()));
        out = out.replace("YY", String.format("%02d", date.getYear() % 100));
        out = out.replace("MM", String.format("%02d", date.getMonthValue()));
        out = out.replace("DD", String.format("%02d", date.getDayOfMonth()));
        out = out.replaceAll("[^0-9._-]", "");
        return out.isBlank() ? "UNKNOWN" : out;
    }

    private static int processFile(
            AppConfig.OperationMode mode,
            Path source,
            Path destFolder,
            Path bakFolder,
            StringBuilder log
    ) throws IOException {
        String name = source.getFileName().toString();
        Path destPath = destFolder.resolve(name);

        if (Files.exists(destPath)) {
            logError(log, name, "already exists at dest: " + destPath);
            return 1;
        }

        Files.createDirectories(destFolder);

        switch (mode.normalized()) {
            case COPY -> copyToNewFile(source, destPath);
            case MOVE -> Files.move(source, destPath);
            case MOVE_ARCHIVE -> {
                Path bakPath = bakFolder.resolve(name);
                if (Files.exists(bakPath)) {
                    logError(log, name, "already exists in BAK: " + bakPath);
                    return 1;
                }

                Files.createDirectories(bakFolder);
                copyToNewFile(source, bakPath);
                Files.move(source, destPath);
            }
            case COPY_ARCHIVE -> throw new IllegalStateException("Unexpected legacy mode");
        }

        return 0;
    }

    private static void copyToNewFile(Path source, Path target) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".sortit-" + UUID.randomUUID() + ".tmp");
        try {
            Files.copy(source, temp, StandardCopyOption.COPY_ATTRIBUTES);
            moveTempToFinal(temp, target);
        } catch (IOException | RuntimeException ex) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignore) {
                // The next run will ignore temporary files because they do not match the final target name.
            }
            throw ex;
        }
    }

    private static void moveTempToFinal(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, target);
        } catch (FileAlreadyExistsException ex) {
            throw ex;
        }
    }

    private static LocalDate parseDateFromFilename(String name) {
        var matcher = FILE_DATE.matcher(name);
        if (!matcher.find()) return null;
        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            return LocalDate.of(year, month, day);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static void notifyProgress(ProgressListener listener, SortProgress progress) {
        if (listener != null) listener.onProgress(progress);
    }

    private static StringBuilder finishLog(StringBuilder log, AppConfig cfg, int processed, int total, int errors) {
        log.append(System.lineSeparator())
                .append("Template: ").append(cfg.destTemplate).append(System.lineSeparator())
                .append("Mode: ").append(cfg.mode.normalized()).append(System.lineSeparator())
                .append("Processed: ").append(processed).append(" / ").append(total)
                .append(" | Errors: ").append(errors).append(System.lineSeparator());
        return log;
    }

    private static Path writeLog(Path logDir, StringBuilder log) {
        try {
            Files.createDirectories(logDir);
            Path logPath = logDir.resolve("sort-" + nowStamp() + ".log").toAbsolutePath();
            try (BufferedWriter writer = Files.newBufferedWriter(
                    logPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                writer.write(log.toString());
            }
            return logPath;
        } catch (IOException ignore) {
            return null;
        }
    }

    private static String nowStamp() {
        ZonedDateTime now = ZonedDateTime.now();
        return String.format(
                "%04d%02d%02d-%02d%02d%02d",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour(),
                now.getMinute(),
                now.getSecond()
        );
    }

    private static void logError(StringBuilder log, String fileName, String message) {
        log.append("- ERROR: ").append(fileName).append(" - ").append(message).append(System.lineSeparator());
    }

    private static String messageOf(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
