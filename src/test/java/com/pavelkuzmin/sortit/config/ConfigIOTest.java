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
    private final String originalAppPath = System.getProperty("jpackage.app-path");
    private final String originalUserDir = System.getProperty("user.dir");

    @AfterEach
    void restoreProperties() {
        restoreProperty("sortit.dataDir", originalDataDir);
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
    void packagedAppPathIsUsedWhenNoOverrideExists() {
        System.clearProperty("sortit.dataDir");
        Path executable = tempDir.resolve("SortIt.exe");
        System.setProperty("jpackage.app-path", executable.toString());

        assertEquals(tempDir.toAbsolutePath().normalize(), AppPaths.dataDirectory());
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
