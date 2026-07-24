package com.pavelkuzmin.sortit.core;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.avi.AviDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mp4.Mp4Directory;

import java.io.File;
import java.util.Date;

/**
 * Универсальный извлекатель даты:
 *  - Фото: EXIF Date/DateOriginal/DateTimeDigitized
 *  - Видео MP4/MOV: Mp4Directory (Creation Time) — если доступно вашей версией библиотеки
 *
 * Возвращает null, если дату достать не удалось.
 */
public final class MediaDateExtractor {
    private MediaDateExtractor() {}

    public static Date getBestDate(File file) {
        if (file == null || !file.exists()) return null;
        try {
            Metadata md = ImageMetadataReader.readMetadata(file);

            // 1) EXIF SubIFD — Date/Time Original (основной для фото)
            ExifSubIFDDirectory sub = md.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            Date date = firstDate(
                    sub,
                    ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL,
                    ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED,
                    ExifSubIFDDirectory.TAG_DATETIME
            );
            if (date != null) return date;

            // 2) EXIF IFD0 — иногда встречается
            ExifIFD0Directory ifd0 = md.getFirstDirectoryOfType(ExifIFD0Directory.class);
            date = firstDate(ifd0, ExifIFD0Directory.TAG_DATETIME);
            if (date != null) return date;

            // 3) MP4 — embedded creation/modification time.
            Mp4Directory mp4 = md.getFirstDirectoryOfType(Mp4Directory.class);
            date = firstDate(mp4, Mp4Directory.TAG_CREATION_TIME, Mp4Directory.TAG_MODIFICATION_TIME);
            if (date != null) return date;

            // 4) MOV/QuickTime and AVI have their own explicit creation tags.
            QuickTimeDirectory quickTime = md.getFirstDirectoryOfType(QuickTimeDirectory.class);
            date = firstDate(quickTime, QuickTimeDirectory.TAG_CREATION_TIME, QuickTimeDirectory.TAG_MODIFICATION_TIME);
            if (date != null) return date;

            AviDirectory avi = md.getFirstDirectoryOfType(AviDirectory.class);
            return firstDate(avi, AviDirectory.TAG_DATETIME_ORIGINAL);

        } catch (Exception e) {
            return null;
        }
    }

    private static Date firstDate(com.drew.metadata.Directory directory, int... tags) {
        if (directory == null) return null;
        for (int tag : tags) {
            try {
                Date date = directory.getDate(tag);
                if (date != null) return date;
            } catch (RuntimeException ignore) {
                // A malformed metadata tag should not block processing other files.
            }
        }
        return null;
    }
}
