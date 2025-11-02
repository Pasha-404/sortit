package com.pavelkuzmin.sortit.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Strings {
    // Всегда начинаем с EN, чтобы не зависеть от локали ОС
    private static Locale current = Locale.ENGLISH;
    private static ResourceBundle bundle = loadBundle(current);

    private Strings() {}

    private static ResourceBundle loadBundle(Locale loc) {
        // наши файлы лежат как i18n/strings_en.properties и i18n/strings_ru.properties
        return ResourceBundle.getBundle("i18n/strings", loc);
    }

    /** Установить локаль по коду ("en" / "ru"). Любой другой код → en. */
    public static void setLocale(String code) {
        Locale loc = "ru".equalsIgnoreCase(code) ? new Locale("ru") : Locale.ENGLISH;
        current = loc;
        bundle = loadBundle(current);
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /** Удобный геттер с дефолтом, если ключа нет. */
    public static String getOr(String key, String def) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return def;
        }
    }

    /** Текущий код языка ("en" / "ru"). */
    public static String langCode() {
        return "ru".equalsIgnoreCase(current.getLanguage()) ? "ru" : "en";
    }
}
