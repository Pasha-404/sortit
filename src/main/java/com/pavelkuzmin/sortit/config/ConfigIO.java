package com.pavelkuzmin.sortit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;

public final class ConfigIO {
    private static final String FILE_NAME = "sortit.json";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private ConfigIO() {}

    public static AppConfig load() {
        File f = new File(FILE_NAME);
        if (!f.exists()) {
            // дефолтные значения по ТЗ
            AppConfig cfg = new AppConfig();
            cfg.lang = "en";
            cfg.sourceDir = "";
            cfg.filenameTemplate = "*.*";
            cfg.dateSource = AppConfig.DateSource.METADATA;
            cfg.mode = AppConfig.OperationMode.COPY;
            cfg.destDir = "";
            cfg.destTemplate = "YYYYMMDD";
            cfg.showResults = true;
            return cfg;
        }
        try {
            AppConfig cfg = MAPPER.readValue(f, AppConfig.class);
            if (cfg != null) cfg.normalizeLegacyFields();
            return cfg;
        } catch (Exception e) {
            // если конфиг битый — вернём новый дефолтный
            AppConfig cfg = new AppConfig();
            cfg.normalizeLegacyFields();
            return cfg;
        }
    }

    public static void save(AppConfig cfg) {
        if (cfg == null) return;
        try {
            MAPPER.writeValue(new File(FILE_NAME), cfg);
        } catch (Exception ignore) {}
    }
}
