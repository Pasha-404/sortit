package com.pavelkuzmin.sortit.core;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.xmp.XmpDirectory;

import java.io.File;
import java.time.*;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

/** Универсальный извлекатель даты для фото/видео без жёсткой зависимости от классов директорий. */
public final class MediaDateExtractor {
    private MediaDateExtractor(){}

    /** Возвращает дату съёмки/создания из метаданных (LocalDate), если удалось. */
    public static Optional<LocalDate> readDate(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);

        try {
            Metadata md = ImageMetadataReader.readMetadata(file);

            // ---- Фото → используем наш устойчивый EXIF-парсер
            if (isPhoto(name)) {
                var d = ExifDateExtractor.readDate(file);
                if (d.isPresent()) return d;

                // запасной вариант: IFD0 DateTime
                ExifIFD0Directory ifd0 = md.getFirstDirectoryOfType(ExifIFD0Directory.class);
                if (ifd0 != null && ifd0.getDate(ExifIFD0Directory.TAG_DATETIME) != null) {
                    Date dt = ifd0.getDate(ExifIFD0Directory.TAG_DATETIME);
                    return Optional.of(LocalDateTime.ofInstant(dt.toInstant(), ZoneId.systemDefault()).toLocalDate());
                }
            }

            // ---- Видео (и фото) → XMP (иногда лежит и в видео, и в фото)
            XmpDirectory xmp = md.getFirstDirectoryOfType(XmpDirectory.class);
            if (xmp != null) {
                String[] keys = {
                        "exif:DateTimeOriginal",
                        "xmp:CreateDate",
                        "photoshop:DateCreated",
                        "com.apple.quicktime.creationdate"
                };
                for (String k : keys) {
                    String v = xmp.getXmpProperties().get(k);
                    if (v != null && !v.isBlank()) {
                        var parsed = ExifDateExtractor.readDate(file) // вдруг есть EXIF-дубли
                                .or(() -> parseIsoLike(v));
                        if (parsed.isPresent()) return parsed;
                    }
                }
            }

            // ---- Общий проход по всем директориям/тегам (в т.ч. QuickTime/MP4)
            // Ищем подходящие названия тегов, читаем Date и конвертируем
            for (Directory dir : md.getDirectories()) {
                for (Tag t : dir.getTags()) {
                    String tn = t.getTagName().toLowerCase(Locale.ROOT);
                    if (looksLikeCreationDate(tn)) {
                        try {
                            Date d = dir.getDate(t.getTagType());
                            if (d != null) {
                                boolean isQtOrMp4 = isQuickTimeOrMp4(dir.getClass().getName());
                                ZoneId zone = isQtOrMp4 ? ZoneOffset.UTC : ZoneId.systemDefault();
                                return Optional.of(LocalDateTime.ofInstant(d.toInstant(), zone).toLocalDate());
                            }
                        } catch (Exception ignored) { /* пропускаем нечитабельные */ }
                    }
                }
            }

            // Ничего не нашли
            return Optional.empty();

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static boolean isPhoto(String name) {
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".heic") || name.endsWith(".heif");
    }

    private static boolean looksLikeCreationDate(String tagNameLower) {
        // самые частые варианты названий тегов
        return tagNameLower.contains("creation time")
                || tagNameLower.contains("create date")
                || tagNameLower.contains("created")        // иногда "Track Created", "Media Created"
                || tagNameLower.contains("media creation")
                || tagNameLower.contains("media create");
    }

    private static boolean isQuickTimeOrMp4(String className) {
        String s = className.toLowerCase(Locale.ROOT);
        return s.contains("quicktime") || s.contains("mp4");
    }

    // Парсинг ISO-подобных строк из XMP
    private static Optional<LocalDate> parseIsoLike(String v) {
        String s = v.trim().replace(' ', 'T');
        try { // с Z/offset
            var inst = java.time.Instant.parse(s);
            return Optional.of(LocalDateTime.ofInstant(inst, ZoneId.systemDefault()).toLocalDate());
        } catch (Exception ignore) {
            try {
                var odt = java.time.OffsetDateTime.parse(s);
                return Optional.of(odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate());
            } catch (Exception ignore2) {
                try {
                    var ldt = java.time.LocalDateTime.parse(s);
                    return Optional.of(ldt.toLocalDate());
                } catch (Exception ignore3) { return Optional.empty(); }
            }
        }
    }
}
