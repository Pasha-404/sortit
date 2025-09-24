package com.pavelkuzmin.sortit.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Strings {
    private static volatile Locale current = Locale.forLanguageTag("en");
    private static volatile ResourceBundle bundle = ResourceBundle.getBundle("i18n/strings", current);

    private Strings() {}

    /** Получить строку по ключу из активного бандла. Если нет — вернуть !key!. */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }

    /** Установить язык по коду ("ru" / "en" / др. ISO-тег) и перезагрузить бандл. */
    public static void setLanguageCode(String code) {
        if (code == null || code.isBlank()) code = "en";
        Locale loc = Locale.forLanguageTag(code.trim().toLowerCase());
        // если что-то экзотическое, оставим только язык (например, "ru")
        if (loc.getLanguage().isEmpty()) loc = Locale.forLanguageTag("en");

        // менять только если реально другой язык
        if (!loc.equals(current)) {
            current = loc;
            bundle = ResourceBundle.getBundle("i18n/strings", current);
        }
    }

    /** Текущий языковой код (например, "en" или "ru"). */
    public static String getLanguageCode() {
        return current.getLanguage();
    }
}
