package com.pavelkuzmin.sortit.config;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

/** Resolves the per-user locations required by the Windows installer standard. */
public final class AppPaths {
    private static final String DATA_DIR_PROPERTY = "sortit.dataDir";
    private static final String CONFIG_DIR_PROPERTY = "sortit.configDir";
    private static final String LOG_DIR_PROPERTY = "sortit.logDir";
    private static final String VENDOR_DIRECTORY = "PashaApps";
    private static final String APPLICATION_DIRECTORY = "SortIt";

    private AppPaths() {}

    /** Retained for existing callers and test overrides. Application data now means settings. */
    public static Path dataDirectory() {
        return configDirectory();
    }

    public static Path configDirectory() {
        return configuredDirectory(CONFIG_DIR_PROPERTY, DATA_DIR_PROPERTY, roamingAppDataDirectory());
    }

    public static Path logDirectory() {
        return configuredDirectory(LOG_DIR_PROPERTY, DATA_DIR_PROPERTY, localAppDataDirectory());
    }

    public static Path configPath() {
        return configDirectory().resolve("sortit.json");
    }

    static List<Path> legacyConfigPaths() {
        LinkedHashSet<Path> paths = new LinkedHashSet<>();
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path executable = Path.of(appPath);
            if (executable.getParent() != null) {
                paths.add(executable.getParent().toAbsolutePath().normalize().resolve("sortit.json"));
            }
        }
        paths.add(workingDirectory().resolve("sortit.json"));
        paths.remove(configPath());
        return List.copyOf(paths);
    }

    private static Path configuredDirectory(String primaryProperty, String fallbackProperty, Path defaultDirectory) {
        String configured = System.getProperty(primaryProperty);
        if (configured == null || configured.isBlank()) {
            configured = System.getProperty(fallbackProperty);
        }
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        return defaultDirectory;
    }

    private static Path roamingAppDataDirectory() {
        return windowsDataDirectory("APPDATA", "Roaming");
    }

    private static Path localAppDataDirectory() {
        return windowsDataDirectory("LOCALAPPDATA", "Local");
    }

    private static Path windowsDataDirectory(String environmentVariable, String fallbackLeaf) {
        String configured = System.getenv(environmentVariable);
        Path root = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "AppData", fallbackLeaf)
                : Path.of(configured);
        return root.resolve(VENDOR_DIRECTORY).resolve(APPLICATION_DIRECTORY).toAbsolutePath().normalize();
    }

    private static Path workingDirectory() {
        return Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }
}
