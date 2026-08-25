package com.pavelkuzmin.sortit.core;

import java.nio.file.Path;

public record SortRunResult(
        int processed,
        int total,
        int errors,
        int skipped,
        int warnings,
        Path logPath,
        int deletedOldLogs
) {
}
