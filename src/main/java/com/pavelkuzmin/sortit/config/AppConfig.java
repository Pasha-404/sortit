package com.pavelkuzmin.sortit.config;

public class AppConfig {
    // Источник
    public String lang = "ru";
    public String sourceDir = "";
    public String filenameTemplate = "*.*";         // настоящий шаблон (glob)
    public DateSource dateSource = DateSource.METADATA; // по умолчанию EXIF/metadata
    public boolean copyMode = true;                 // true = копировать, false = переносить

    // Назначение
    public String destDir = "";
    public String destTemplate = "YYYYMMDD";

    // Поведение
    public boolean showResult = false;

    // Положение окна (если -1 — не задано)
    public int windowX = -1;
    public int windowY = -1;
    public int windowW = -1;
    public int windowH = -1;
}
