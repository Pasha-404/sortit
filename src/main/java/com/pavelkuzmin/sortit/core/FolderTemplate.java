package com.pavelkuzmin.sortit.core;

import java.time.LocalDate;

/** Валидирует и форматирует шаблон папок: допускаются только YYYY, YY, MM, DD и разделители - _ . */
public final class FolderTemplate {

    private FolderTemplate() {}

    /** Проверка: только токены YYYY|YY|MM|DD и разделители -_. ; минимум один токен. */
    public static boolean isValid(String template) {
        if (template == null || template.isBlank()) return false;
        int i = 0;
        boolean sawToken = false;
        while (i < template.length()) {
            char ch = template.charAt(i);
            if (ch == 'Y') {
                // YYYY или YY
                int run = countRun(template, i, 'Y');
                if (run == 2 || run == 4) {
                    sawToken = true;
                    i += run;
                } else return false;
            } else if (ch == 'M') {
                int run = countRun(template, i, 'M');
                if (run == 2) { sawToken = true; i += run; } else return false;
            } else if (ch == 'D') {
                int run = countRun(template, i, 'D');
                if (run == 2) { sawToken = true; i += run; } else return false;
            } else if (ch == '-' || ch == '_' || ch == '.') {
                i++;
            } else {
                return false;
            }
        }
        return sawToken;
    }

    private static int countRun(String s, int i, char expected) {
        int j = i;
        while (j < s.length() && s.charAt(j) == expected) j++;
        return j - i;
    }

    /** Применение шаблона к дате. Предполагается, что шаблон уже валиден. */
    public static String apply(String template, LocalDate date) {
        StringBuilder out = new StringBuilder(template.length() + 8);
        int i = 0;
        while (i < template.length()) {
            char ch = template.charAt(i);
            if (ch == 'Y') {
                int run = countRun(template, i, 'Y');
                if (run == 4) out.append(String.format("%04d", date.getYear()));
                else /* run==2 */ out.append(String.format("%02d", date.getYear() % 100));
                i += run;
            } else if (ch == 'M') {
                out.append(String.format("%02d", date.getMonthValue()));
                i += 2;
            } else if (ch == 'D') {
                out.append(String.format("%02d", date.getDayOfMonth()));
                i += 2;
            } else {
                out.append(ch); // '-', '_', '.'
                i++;
            }
        }
        return out.toString();
    }
}
