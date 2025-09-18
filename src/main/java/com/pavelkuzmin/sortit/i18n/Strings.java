package com.pavelkuzmin.sortit.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Strings {
    private static final String BASENAME = "i18n.strings"; // <-- точки, не слэш
    private static ResourceBundle bundle = loadBundle(Locale.getDefault());

    private Strings() {}

    private static ResourceBundle loadBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle(BASENAME, locale);
        } catch (MissingResourceException e) {
            return ResourceBundle.getBundle(BASENAME, Locale.ENGLISH);
        }
    }

    public static void setLocale(Locale locale) {
        bundle = loadBundle(locale);
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }
}
