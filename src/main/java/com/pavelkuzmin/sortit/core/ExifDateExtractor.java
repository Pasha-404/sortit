package com.pavelkuzmin.sortit.core;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.xmp.XmpDirectory;

import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** Читает дату съёмки из EXIF/XMP и возвращает LocalDate. */
public final class ExifDateExtractor {
    private ExifDateExtractor() {}

    // EXIF 2.31 tag codes (на случай отсутствия констант в библиотеке)
    private static final int TAG_OFFSET_TIME            = 0x9010; // "+03:00" и т.п.
    private static final int TAG_OFFSET_TIME_ORIGINAL   = 0x9011;
    private static final int TAG_OFFSET_TIME_DIGITIZED  = 0x9012;

    // EXIF datetime формат: "yyyy:MM:dd HH:mm:ss"
    private static final DateTimeFormatter EXIF_DT =
            DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    public static Optional<LocalDate> readDate(File file) {
        try {
            Metadata md = ImageMetadataReader.readMetadata(file);

            // 1) EXIF SubIFD — основной источник
            ExifSubIFDDirectory sub = md.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (sub != null) {
                // DateTimeOriginal
                String dto = sub.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (dto != null && !dto.isBlank()) {
                    LocalDate d = parseExifDate(sub, dto);
                    if (d != null) return Optional.of(d);
                }
                // CreateDate / Digitized
                String cd = sub.getString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
                if (cd != null && !cd.isBlank()) {
                    LocalDate d = parseExifDate(sub, cd);
                    if (d != null) return Optional.of(d);
                }
            }

            // 2) IFD0 DateTime
            ExifIFD0Directory ifd0 = md.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {
                String dt = ifd0.getString(ExifIFD0Directory.TAG_DATETIME);
                if (dt != null && !dt.isBlank()) {
                    LocalDate d = parseExifDate(ifd0, dt);
                    if (d != null) return Optional.of(d);
                }
            }

            // 3) XMP (часто у Pixel/редакторов)
            XmpDirectory xmp = md.getFirstDirectoryOfType(XmpDirectory.class);
            if (xmp != null) {
                String[] keys = {
                        "exif:DateTimeOriginal", "xmp:CreateDate", "photoshop:DateCreated"
                };
                for (String k : keys) {
                    String v = xmp.getXmpProperties().get(k);
                    if (v != null && !v.isBlank()) {
                        Optional<LocalDate> d = parseIsoLike(v);
                        if (d.isPresent()) return d;
                    }
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** Парсинг EXIF-строки + учёт OffsetTime*/
    private static LocalDate parseExifDate(Directory dir, String exifDateTime) {
        try {
            LocalDateTime ldt = LocalDateTime.parse(exifDateTime, EXIF_DT);

            // Попробуем учесть смещение (OffsetTimeOriginal/OffsetTime/OffsetTimeDigitized)
            String off = firstNonBlank(
                    safeGetString(dir, TAG_OFFSET_TIME_ORIGINAL),
                    safeGetString(dir, TAG_OFFSET_TIME_DIGITIZED),
                    safeGetString(dir, TAG_OFFSET_TIME)
            );
            if (off != null && !off.isBlank()) {
                try {
                    ZoneOffset zo = ZoneOffset.of(off.trim());
                    return ldt.atOffset(zo).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
                } catch (DateTimeParseException ignored) {
                    // если смещение странное — вернём без него
                    return ldt.toLocalDate();
                }
            }
            return ldt.toLocalDate();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String safeGetString(Directory dir, int tagType) {
        try { return dir.getString(tagType); } catch (Exception ignored) { return null; }
    }

    /** Разные ISO-подобные варианты: 2025-01-04T13:15:48.355+03:00 и т.п. */
    private static Optional<LocalDate> parseIsoLike(String v) {
        String s = v.trim().replace(' ', 'T');
        try {
            // с Z или смещением
            Instant inst = Instant.parse(s);
            return Optional.of(LocalDateTime.ofInstant(inst, ZoneId.systemDefault()).toLocalDate());
        } catch (Exception ignore) {
            try {
                OffsetDateTime odt = OffsetDateTime.parse(s);
                return Optional.of(odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate());
            } catch (Exception ignore2) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(s);
                    return Optional.of(ldt.toLocalDate());
                } catch (Exception ignore3) {
                    return Optional.empty();
                }
            }
        }
    }

    private static String firstNonBlank(String... vals) {
        for (String x : vals) if (x != null && !x.isBlank()) return x;
        return null;
    }
}
