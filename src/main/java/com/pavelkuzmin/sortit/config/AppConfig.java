package com.pavelkuzmin.sortit.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppConfig {

    public enum DateSource {
        METADATA,
        FILENAME,
        CREATED
    }

    public enum OperationMode {
        COPY,
        MOVE,
        MOVE_ARCHIVE,
        @Deprecated
        COPY_ARCHIVE;

        public OperationMode normalized() {
            return this == COPY_ARCHIVE ? MOVE_ARCHIVE : this;
        }
    }

    public String lang = "en";

    public String sourceDir = "";
    public String filenameTemplate = "*.*";
    public DateSource dateSource = DateSource.METADATA;

    @Deprecated
    public Boolean copyMode;

    public OperationMode mode = OperationMode.COPY;

    public String destDir = "";
    public String destTemplate = "YYYYMMDD";

    public boolean showResults = true;

    public Integer windowX;
    public Integer windowY;

    @JsonIgnore
    public void normalizeLegacyFields() {
        if (mode == null) {
            if (copyMode != null) {
                mode = copyMode ? OperationMode.COPY : OperationMode.MOVE;
            } else {
                mode = OperationMode.COPY;
            }
        }
        mode = mode.normalized();
        copyMode = null;
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

    public static AppConfig copyOf(AppConfig src) {
        AppConfig copy = new AppConfig();
        if (src == null) {
            copy.normalizeLegacyFields();
            return copy;
        }
        copy.lang = src.lang;
        copy.sourceDir = src.sourceDir;
        copy.filenameTemplate = src.filenameTemplate;
        copy.dateSource = src.dateSource;
        copy.copyMode = src.copyMode;
        copy.mode = src.mode;
        copy.destDir = src.destDir;
        copy.destTemplate = src.destTemplate;
        copy.showResults = src.showResults;
        copy.windowX = src.windowX;
        copy.windowY = src.windowY;
        copy.normalizeLegacyFields();
        return copy;
    }
}
