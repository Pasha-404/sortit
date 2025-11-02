package com.pavelkuzmin.sortit.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Конфиг приложения. Совместим с прошлой версией:
 * - раньше был boolean copyMode (true = Copy, false = Move);
 * - теперь используем enum OperationMode, но при чтении старых конфигов
 *   copyMode будет автоматически транспонироваться в mode.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppConfig {

    public enum DateSource {
        METADATA,     // EXIF/metadata
        FILENAME,     // дата из имени файла
        CREATED       // дата создания файла
    }

    public enum OperationMode {
        COPY,          // просто копировать
        MOVE,          // переносить
        COPY_ARCHIVE   // копировать, затем исходник перенести в BAK
    }

    // ----- i18n -----
    public String lang = "en";

    // ----- Источник -----
    public String sourceDir = "";
    public String filenameTemplate = "*.*";
    public DateSource dateSource = DateSource.METADATA;

    // ----- Режим -----
    // старое поле, оставлено для обратной совместимости:
    // true = COPY, false = MOVE
    @Deprecated
    public Boolean copyMode; // может отсутствовать в новых конфигах

    // новое поле с режимом
    public OperationMode mode = OperationMode.COPY;

    // ----- Назначение -----
    public String destDir = "";
    public String destTemplate = "YYYYMMDD";

    // ----- Прочее -----
    public boolean showResults = false;

    // позиция окна
    public Integer windowX;
    public Integer windowY;

    // ----- Вспомогательное: маппинг со старого copyMode -----
    @JsonIgnore
    public void normalizeLegacyFields() {
        if (mode == null) {
            if (copyMode != null) {
                mode = copyMode ? OperationMode.COPY : OperationMode.MOVE;
            } else {
                mode = OperationMode.COPY;
            }
        }
        if (filenameTemplate == null || filenameTemplate.isBlank()) {
            filenameTemplate = "*.*";
        }
        if (destTemplate == null || destTemplate.isBlank()) {
            destTemplate = "YYYYMMDD";
        }
        if (lang == null || lang.isBlank()) {
            lang = "en";
        }
        if (dateSource == null) {
            dateSource = DateSource.METADATA;
        }
    }
}
