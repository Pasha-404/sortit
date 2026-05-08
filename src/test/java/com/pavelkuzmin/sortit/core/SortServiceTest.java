package com.pavelkuzmin.sortit.core;

import com.pavelkuzmin.sortit.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class SortServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void copyModeCopiesToDestinationAndKeepsSource() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("IMG_20240506.jpg"), "photo");

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.COPY), tempDir, SortService.Messages.english(), null);

        assertEquals(0, result.errors());
        assertTrue(Files.exists(file));
        assertEquals("photo", Files.readString(destDir.resolve("20240506").resolve(file.getFileName())));
    }

    @Test
    void moveModeMovesToDestinationAndRemovesSource() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("PXL_20240506_100000000.jpg"), "photo");

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.MOVE), tempDir, SortService.Messages.english(), null);

        assertEquals(0, result.errors());
        assertFalse(Files.exists(file));
        assertEquals("photo", Files.readString(destDir.resolve("20240506").resolve(file.getFileName())));
    }

    @Test
    void moveArchiveModeMovesToDestinationAndKeepsBackupCopy() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("VID_20240506_100000.mp4"), "video");

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.MOVE_ARCHIVE), tempDir, SortService.Messages.english(), null);

        assertEquals(0, result.errors());
        assertFalse(Files.exists(file));
        assertEquals("video", Files.readString(destDir.resolve("20240506").resolve(file.getFileName())));
        assertEquals("video", Files.readString(sourceDir.resolve("BAK").resolve("20240506").resolve(file.getFileName())));
    }

    @Test
    void destinationConflictLeavesSourceUntouched() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("IMG_20240506.jpg"), "new");
        Path conflictFolder = Files.createDirectories(destDir.resolve("20240506"));
        Files.writeString(conflictFolder.resolve(file.getFileName()), "existing");

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.MOVE_ARCHIVE), tempDir, SortService.Messages.english(), null);

        assertEquals(1, result.errors());
        assertTrue(Files.exists(file));
        assertEquals("existing", Files.readString(conflictFolder.resolve(file.getFileName())));
        assertFalse(Files.exists(sourceDir.resolve("BAK")));
    }

    @Test
    void runDeletesSortLogsOlderThanOneDay() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path oldLog = Files.writeString(tempDir.resolve("sort-20240101-0000.log"), "old");
        Path freshLog = Files.writeString(tempDir.resolve("sort-20240506-0000.log"), "fresh");
        Files.setLastModifiedTime(oldLog, FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS)));
        Files.setLastModifiedTime(freshLog, FileTime.from(Instant.now()));

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.COPY), tempDir, SortService.Messages.english(), null);

        assertEquals(1, result.deletedOldLogs());
        assertFalse(Files.exists(oldLog));
        assertTrue(Files.exists(freshLog));
    }

    private AppConfig config(Path sourceDir, Path destDir, AppConfig.OperationMode mode) {
        AppConfig cfg = new AppConfig();
        cfg.sourceDir = sourceDir.toString();
        cfg.destDir = destDir.toString();
        cfg.filenameTemplate = "*.*";
        cfg.destTemplate = "YYYYMMDD";
        cfg.dateSource = AppConfig.DateSource.FILENAME;
        cfg.mode = mode;
        cfg.normalizeLegacyFields();
        return cfg;
    }
}
