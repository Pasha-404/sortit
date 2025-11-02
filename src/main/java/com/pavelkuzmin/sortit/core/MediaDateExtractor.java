package com.pavelkuzmin.sortit.core;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
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
            if (sub != null) {
                Date d =
                        sub.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (d == null) d = sub.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
                if (d == null) d = sub.getDate(ExifSubIFDDirectory.TAG_DATETIME);
                if (d != null) return d;
            }

            // 2) EXIF IFD0 — иногда встречается
            ExifIFD0Directory ifd0 = md.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                Date d = ifd0.getDate(ExifIFD0Directory.TAG_DATETIME);
                if (d != null) return d;
            }

            // 3) MP4/MOV — через Mp4Directory (если доступен). Часто теги: Creation Time / Modification Time
            Mp4Directory mp4 = md.getFirstDirectoryOfType(Mp4Directory.class);
            if (mp4 != null) {
                // Попробуем стандартные теги
                // В разных версиях lib могут быть разные константы: используем getDate(int) перебором известных.
                int[] candidates = {
                        // Creation/Modification — часто встречаются
                        Mp4Directory.TAG_CREATION_TIME,
                        Mp4Directory.TAG_MODIFICATION_TIME
                };
                for (int tag : candidates) {
                    try {
                        Date d = mp4.getDate(tag);
                        if (d != null) return d;
                    } catch (Throwable ignore) {}
                }
            }

            // 4) Последняя надежда: пройтись по всем директориям и вытянуть первый Date
            for (Directory dir : md.getDirectories()) {
                for (com.drew.metadata.Tag t : dir.getTags()) {
                    if (t.getTagName() != null && t.getTagName().toLowerCase().contains("date")) {
                        try {
                            Date d = dir.getDate(t.getTagType());
                            if (d != null) return d;
                        } catch (Throwable ignore) {}
                    }
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
