package com.pavelkuzmin.sortit.core;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameDateParser {

    // PREDEFINED — сначала проверяем их
    private static final Pattern[] PREDEFINED = new Pattern[]{
            Pattern.compile("^IMG_([0-9]{8}).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^VID_([0-9]{8}).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^PXL_([0-9]{8}).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^IMG([0-9]{8}).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^WP_([0-9]{8}).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^([0-9]{8}).*", Pattern.CASE_INSENSITIVE) // Samsung YYYYMMDD
    };

    // Общий поиск: любая 8-значная последовательность
    private static final Pattern ANY_8_DIGITS = Pattern.compile("([0-9]{8})");

    public static Optional<LocalDate> parse(String name) {
        // 1) PREDEFINED
        for (Pattern p : PREDEFINED) {
            Matcher m = p.matcher(name);
            if (m.matches()) {
                Optional<LocalDate> d = parseYYYYMMDD(m.group(1));
                if (d.isPresent()) return d;
            }
        }
        // 2) Общий поиск
        Matcher m = ANY_8_DIGITS.matcher(name);
        while (m.find()) {
            Optional<LocalDate> d = parseYYYYMMDD(m.group(1));
            if (d.isPresent()) return d;
        }
        return Optional.empty();
    }

    private static Optional<LocalDate> parseYYYYMMDD(String s) {
        try {
            int y = Integer.parseInt(s.substring(0, 4));
            int m = Integer.parseInt(s.substring(4, 6));
            int d = Integer.parseInt(s.substring(6, 8));
            return Optional.of(LocalDate.of(y, m, d));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
