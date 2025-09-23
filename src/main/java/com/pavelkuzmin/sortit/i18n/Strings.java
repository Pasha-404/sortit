package com.pavelkuzmin.sortit.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Strings {
    private static Locale current = Locale.getDefault();
    private static ResourceBundle bundle = ResourceBundle.getBundle("i18n/strings", current);

    private Strings() {}

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    /** Установить язык по коду ("ru" / "en"). */
    public static void setLanguageCode(String code) {
        Locale loc = "en".equalsIgnoreCase(code) ? Locale.ENGLISH : new Locale("ru");
        if (!loc.equals(current)) {
            current = loc;
            bundle = ResourceBundle.getBundle("i18n/strings", current);
        }
    }

    public static String getLanguageCode() {
        return current.getLanguage();
    }
}
