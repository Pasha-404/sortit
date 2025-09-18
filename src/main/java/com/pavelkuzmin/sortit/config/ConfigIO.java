package com.pavelkuzmin.sortit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;

public class ConfigIO {
    private static final String FILE_NAME = "sortit.json";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static AppConfig loadOrDefaults() {
        try {
            File f = new File(FILE_NAME);
            if (f.exists()) {
                return MAPPER.readValue(f, AppConfig.class);
            } else {
                AppConfig cfg = new AppConfig(); // с дефолтами
                save(cfg);
                return cfg;
            }
        } catch (Exception e) {
            // В случае ошибки — возвращаем дефолты (не валим приложение)
            return new AppConfig();
        }
    }

    public static void save(AppConfig cfg) {
        try {
            MAPPER.writeValue(new File(FILE_NAME), cfg);
        } catch (Exception ignored) { }
    }
}
