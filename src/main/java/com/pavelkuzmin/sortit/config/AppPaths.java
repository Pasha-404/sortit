package com.pavelkuzmin.sortit.config;

import java.nio.file.Path;

/** Resolves one stable location for SortIt settings and result logs. */
public final class AppPaths {
    private static final String DATA_DIR_PROPERTY = "sortit.dataDir";

    private AppPaths() {}

    public static Path dataDirectory() {
        String configured = System.getProperty(DATA_DIR_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }

        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path executable = Path.of(appPath);
            if (executable.getParent() != null) {
                return executable.getParent().toAbsolutePath().normalize();
            }
        }

        return workingDirectory();
    }

    public static Path configPath() {
        return dataDirectory().resolve("sortit.json");
    }

    public static Path logDirectory() {
        return dataDirectory();
    }

    static Path legacyConfigPath() {
        return workingDirectory().resolve("sortit.json");
    }

    private static Path workingDirectory() {
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }
}
