package com.pavelkuzmin.sortit.core;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

public class ExifDateExtractor {

    private static final String[] EXIF_EXT = {
            "jpg","jpeg","tif","tiff","dng","nef","cr2","arw","rw2"
    };

    public static boolean isExifCandidate(File f) {
        String name = f.getName().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = name.substring(dot + 1);
        for (String ok : EXIF_EXT) if (ok.equals(ext)) return true;
        return false;
    }

    public static Optional<LocalDate> readDate(File f) {
        if (!isExifCandidate(f)) return Optional.empty();
        try {
            // TODO: подключить metadata-extractor
            // Пока возвращаем empty, чтобы не падать.
            return Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
