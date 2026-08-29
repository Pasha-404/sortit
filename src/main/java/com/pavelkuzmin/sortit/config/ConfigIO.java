package com.pavelkuzmin.sortit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigIO {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private ConfigIO() {}

    public static AppConfig load() {
        Path configPath = AppPaths.configPath();
        try {
            if (Files.isRegularFile(configPath)) {
                return read(configPath);
            }

            // Preserve settings created before settings moved to the per-user roaming directory.
            for (Path legacyPath : AppPaths.legacyConfigPaths()) {
                if (Files.isRegularFile(legacyPath)) {
                    AppConfig migrated = read(legacyPath);
                    save(migrated);
                    return migrated;
                }
            }
        } catch (Exception ignore) {
            // A broken config must not stop the application from opening.
        }
        return defaults();
    }

    public static boolean save(AppConfig cfg) {
        if (cfg == null) return false;

        Path target = AppPaths.configPath();
        Path temp = null;
        try {
            Path parent = target.getParent();
            if (parent == null) return false;
            Files.createDirectories(parent);
            temp = Files.createTempFile(parent, "sortit-", ".json.tmp");
            MAPPER.writeValue(temp.toFile(), cfg);
            moveTempToFinal(temp, target);
            return true;
        } catch (Exception ignore) {
            return false;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignore) {
                    // A future save can safely replace an abandoned temporary file.
                }
            }
        }
    }

    private static AppConfig read(Path path) throws IOException {
        AppConfig cfg = MAPPER.readValue(path.toFile(), AppConfig.class);
        if (cfg == null) return defaults();
        cfg.normalizeLegacyFields();
        return cfg;
    }

    private static AppConfig defaults() {
        AppConfig cfg = new AppConfig();
        cfg.lang = "en";
        cfg.sourceDir = "";
        cfg.filenameTemplate = "*.*";
        cfg.dateSource = AppConfig.DateSource.METADATA;
        cfg.mode = AppConfig.OperationMode.COPY;
        cfg.destDir = "";
        cfg.destTemplate = "YYYYMMDD";
        cfg.showResults = true;
        return cfg;
    }

    private static void moveTempToFinal(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
