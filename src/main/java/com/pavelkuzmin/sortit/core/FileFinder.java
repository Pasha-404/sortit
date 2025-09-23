package com.pavelkuzmin.sortit.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Сканирует папку-источник:
 * 1) Пытается распознать предопределённые паттерны (IMG_/VID_/PXL_/IMGYYYYMMDD/WP_/YYYYMMDD).
 *    - если найден Pixel (PXL_), предлагаем шаблон "PXL_*.*"
 *    - если найден "YYYYMMDD" без префикса — шаблон "*.*"
 *    - иначе (другие префиксы) — тоже "*.*"
 * 2) Если по PREDEFINED ничего, используем кастомный шаблон (glob, напр. "*.*" или "PXL_*.*").
 */
public class FileFinder {

    public static class Result {
        public final int totalFiles;
        public final int matchedFiles;
        public final String detectedTemplate; // что подставить в поле шаблона (напр. "PXL_*.*" или "*.*"), иначе null
        public final boolean sourceMissing;
        public final boolean emptySource;

        public Result(int totalFiles, int matchedFiles, String detectedTemplate,
                      boolean sourceMissing, boolean emptySource) {
            this.totalFiles = totalFiles;
            this.matchedFiles = matchedFiles;
            this.detectedTemplate = detectedTemplate;
            this.sourceMissing = sourceMissing;
            this.emptySource = emptySource;
        }
    }

    private static final Pattern P_IMG_UNDERSCORE = Pattern.compile("^IMG_([0-9]{8}).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_VID_UNDERSCORE = Pattern.compile("^VID_([0-9]{8}).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_PXL = Pattern.compile("^PXL_([0-9]{8}).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_IMG = Pattern.compile("^IMG([0-9]{8}).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_WP = Pattern.compile("^WP_([0-9]{8}).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern P_YYYYMMDD = Pattern.compile("^([0-9]{8}).*", Pattern.CASE_INSENSITIVE);

    public Result scan(String sourceDir, String customTemplateRaw) {
        if (sourceDir == null || sourceDir.isBlank()) {
            return new Result(0, 0, null, true, false);
        }
        Path dir = Path.of(sourceDir);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return new Result(0, 0, null, true, false);
        }

        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) names.add(p.getFileName().toString());
            }
        } catch (IOException e) {
            return new Result(0, 0, null, true, false);
        }

        if (names.isEmpty()) return new Result(0, 0, null, false, true);

        // 1) PREDEFINED
        int pxl = countMatches(P_PXL, names);
        if (pxl > 0) return new Result(names.size(), pxl, "PXL_*.*", false, false);

        int any8 = countMatches(P_YYYYMMDD, names);
        if (any8 > 0) return new Result(names.size(), any8, "*.*", false, false);

        int imgU = countMatches(P_IMG_UNDERSCORE, names);
        if (imgU > 0) return new Result(names.size(), imgU, "*.*", false, false);

        int vidU = countMatches(P_VID_UNDERSCORE, names);
        if (vidU > 0) return new Result(names.size(), vidU, "*.*", false, false);

        int img = countMatches(P_IMG, names);
        if (img > 0) return new Result(names.size(), img, "*.*", false, false);

        int wp = countMatches(P_WP, names);
        if (wp > 0) return new Result(names.size(), wp, "*.*", false, false);

        // 2) Кастомный шаблон (glob). Если пустой/null — считаем "*.*"
        String tpl = (customTemplateRaw == null || customTemplateRaw.isBlank()) ? "*.*" : customTemplateRaw.trim();
        Pattern globRx = globToRegex(tpl);
        int matched = 0;
        for (String n : names) if (globRx.matcher(n).matches()) matched++;

        return new Result(names.size(), matched, tpl, false, false);
    }

    private static int countMatches(Pattern p, List<String> names) {
        int c = 0;
        for (String n : names) if (p.matcher(n).matches()) c++;
        return c;
    }

    private static Pattern globToRegex(String glob) {
        // Простой glob: * → .*, ? → ., остальные экранируем
        StringBuilder sb = new StringBuilder("^");
        for (char ch : glob.toCharArray()) {
            switch (ch) {
                case '*': sb.append(".*"); break;
                case '?': sb.append('.'); break;
                case '.': sb.append("\\."); break;
                default:
                    if ("+()^$|[]{}".indexOf(ch) >= 0) sb.append('\\');
                    sb.append(ch);
            }
        }
        sb.append('$');
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
