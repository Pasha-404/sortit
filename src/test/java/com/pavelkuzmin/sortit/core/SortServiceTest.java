package com.pavelkuzmin.sortit.core;

import com.pavelkuzmin.sortit.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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
    void copyNewOnlySkipsSameNameAndSizeAlreadyInDestination() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("IMG_20240506.jpg"), "photo");
        Path existing = Files.createDirectories(destDir.resolve("20240506")).resolve(file.getFileName());
        Files.writeString(existing, "photo");

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.COPY_NEW_ONLY), tempDir, SortService.Messages.english(), null);

        assertEquals(0, result.errors());
        assertEquals(1, result.skipped());
        assertEquals(0, result.warnings());
        assertTrue(Files.exists(file));
        assertEquals("photo", Files.readString(existing));
        assertTrue(Files.readString(result.logPath()).contains("SKIPPED: IMG_20240506.jpg"));
    }

    @Test
    void copyNewOnlyKeepsBothFilesWhenSameNameHasDifferentSize() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("IMG_20240506.jpg"), "new photo");
        Path destinationFolder = Files.createDirectories(destDir.resolve("20240506"));
        Files.writeString(destinationFolder.resolve(file.getFileName()), "old");
        Files.writeString(destinationFolder.resolve("IMG_20240506 (1).jpg"), "older");

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.COPY_NEW_ONLY), tempDir, SortService.Messages.english(), null);

        assertEquals(0, result.errors());
        assertEquals(0, result.skipped());
        assertEquals(1, result.warnings());
        assertTrue(Files.exists(file));
        assertEquals("old", Files.readString(destinationFolder.resolve(file.getFileName())));
        assertEquals("older", Files.readString(destinationFolder.resolve("IMG_20240506 (1).jpg")));
        assertEquals("new photo", Files.readString(destinationFolder.resolve("IMG_20240506 (2).jpg")));
        assertTrue(Files.readString(result.logPath()).contains("WARNING: IMG_20240506.jpg"));
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
    void moveModeLeavesNoTemporaryFilesInDestination() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("IMG_20240506.jpg"), "photo");

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.MOVE), tempDir, SortService.Messages.english(), null);

        Path destinationFolder = destDir.resolve("20240506");
        assertEquals(0, result.errors());
        assertFalse(Files.exists(file));
        assertEquals("photo", Files.readString(destinationFolder.resolve(file.getFileName())));
        try (var files = Files.list(destinationFolder)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().contains(".sortit-")));
        }
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
    void unsafeFolderTemplateDoesNotWriteOutsideDestination() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("IMG_20240506.jpg"), "photo");
        AppConfig cfg = config(sourceDir, destDir, AppConfig.OperationMode.COPY);
        cfg.destTemplate = "..";

        SortRunResult result = new SortService().run(cfg, tempDir, SortService.Messages.english(), null);

        assertEquals(1, result.errors());
        assertTrue(Files.exists(file));
        assertFalse(Files.exists(tempDir.resolve(file.getFileName())));
        assertFalse(Files.exists(destDir.resolve(file.getFileName())));
    }

    @Test
    void moveArchiveModeContinuesWhenMatchingBackupAlreadyExists() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("IMG_20240506.jpg"), "photo");
        Path backup = Files.createDirectories(sourceDir.resolve("BAK").resolve("20240506"))
                .resolve(file.getFileName());
        Files.writeString(backup, "photo");

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.MOVE_ARCHIVE), tempDir, SortService.Messages.english(), null);

        assertEquals(0, result.errors());
        assertFalse(Files.exists(file));
        assertEquals("photo", Files.readString(destDir.resolve("20240506").resolve(file.getFileName())));
        assertEquals("photo", Files.readString(backup));
    }

    @Test
    void moveArchiveModeRejectsDifferentExistingBackup() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Path file = Files.writeString(sourceDir.resolve("IMG_20240506.jpg"), "new photo");
        Path backup = Files.createDirectories(sourceDir.resolve("BAK").resolve("20240506"))
                .resolve(file.getFileName());
        Files.writeString(backup, "old photo");

        SortRunResult result = new SortService().run(config(sourceDir, destDir, AppConfig.OperationMode.MOVE_ARCHIVE), tempDir, SortService.Messages.english(), null);

        assertEquals(1, result.errors());
        assertTrue(Files.exists(file));
        assertFalse(Files.exists(destDir.resolve("20240506").resolve(file.getFileName())));
        assertEquals("old photo", Files.readString(backup));
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

    @Test
    void runPublishesProgressAtStartAndAfterEveryFile() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path destDir = Files.createDirectory(tempDir.resolve("dest"));
        Files.writeString(sourceDir.resolve("IMG_20240501.jpg"), "one");
        Files.writeString(sourceDir.resolve("IMG_20240502.jpg"), "two");
        Files.writeString(sourceDir.resolve("IMG_20240503.jpg"), "three");
        Files.writeString(sourceDir.resolve("IMG_20240504.jpg"), "four");

        List<SortProgress> progressEvents = new ArrayList<>();
        SortRunResult result = new SortService().run(
                config(sourceDir, destDir, AppConfig.OperationMode.COPY),
                tempDir,
                SortService.Messages.english(),
                progressEvents::add
        );

        assertEquals(0, result.errors());
        assertEquals(List.of(0, 1, 2, 3, 4), progressEvents.stream().map(SortProgress::processed).toList());
        assertTrue(progressEvents.stream().allMatch(progress -> progress.total() == 4));
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
