package com.pavelkuzmin.sortit.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigIOTest {
    @TempDir
    Path tempDir;

    private final String originalDataDir = System.getProperty("sortit.dataDir");
    private final String originalConfigDir = System.getProperty("sortit.configDir");
    private final String originalLogDir = System.getProperty("sortit.logDir");
    private final String originalAppPath = System.getProperty("jpackage.app-path");
    private final String originalUserDir = System.getProperty("user.dir");

    @AfterEach
    void restoreProperties() {
        restoreProperty("sortit.dataDir", originalDataDir);
        restoreProperty("sortit.configDir", originalConfigDir);
        restoreProperty("sortit.logDir", originalLogDir);
        restoreProperty("jpackage.app-path", originalAppPath);
        restoreProperty("user.dir", originalUserDir);
    }

    @Test
    void saveUsesConfiguredStableDirectory() throws Exception {
        Path dataDir = tempDir.resolve("app-data");
        System.setProperty("sortit.dataDir", dataDir.toString());

        AppConfig config = new AppConfig();
        config.lang = "ru";

        assertTrue(ConfigIO.save(config));
        assertTrue(Files.isRegularFile(dataDir.resolve("sortit.json")));
        assertEquals("ru", ConfigIO.load().lang);
    }

    @Test
    void loadMigratesLegacyWorkingDirectoryConfig() throws Exception {
        Path dataDir = tempDir.resolve("app-data");
        Path legacyDir = Files.createDirectories(tempDir.resolve("legacy"));
        System.setProperty("sortit.dataDir", dataDir.toString());
        System.setProperty("user.dir", legacyDir.toString());
        Files.writeString(legacyDir.resolve("sortit.json"), "{\"lang\":\"ru\",\"showResults\":true}");

        AppConfig loaded = ConfigIO.load();

        assertEquals("ru", loaded.lang);
        assertTrue(Files.isRegularFile(dataDir.resolve("sortit.json")));
    }

    @Test
    void packagedAppConfigIsMigratedToConfiguredDirectory() throws Exception {
        Path dataDir = tempDir.resolve("app-data");
        Path executable = tempDir.resolve("old-install").resolve("SortIt.exe");
        Files.createDirectories(executable.getParent());
        Files.writeString(executable.getParent().resolve("sortit.json"), "{\"lang\":\"ru\",\"showResults\":true}");
        System.setProperty("sortit.dataDir", dataDir.toString());
        System.setProperty("jpackage.app-path", executable.toString());

        assertEquals("ru", ConfigIO.load().lang);
        assertTrue(Files.isRegularFile(dataDir.resolve("sortit.json")));
    }

    @Test
    void dataDirectoryOverrideKeepsLogsAndSettingsTogetherForTests() {
        Path dataDir = tempDir.resolve("app-data");
        System.setProperty("sortit.dataDir", dataDir.toString());

        assertEquals(dataDir.toAbsolutePath().normalize(), AppPaths.configDirectory());
        assertEquals(dataDir.toAbsolutePath().normalize(), AppPaths.logDirectory());
    }

    @Test
    void defaultDirectoriesUseThePashaAppsConvention() {
        System.clearProperty("sortit.dataDir");
        System.clearProperty("sortit.configDir");
        System.clearProperty("sortit.logDir");

        Path expectedSuffix = Path.of("PashaApps", "SortIt");
        assertTrue(AppPaths.configDirectory().endsWith(expectedSuffix));
        assertTrue(AppPaths.logDirectory().endsWith(expectedSuffix));
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
