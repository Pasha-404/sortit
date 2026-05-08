package com.pavelkuzmin.sortit.core;

import java.nio.file.Path;

public record SortRunResult(
        int processed,
        int total,
        int errors,
        Path logPath,
        int deletedOldLogs
) {
}
