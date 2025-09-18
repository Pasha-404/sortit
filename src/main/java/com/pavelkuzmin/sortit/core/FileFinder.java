package com.pavelkuzmin.sortit.core;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Ищет файлы в папке-источнике:
 * 1) Сначала по универсальным шаблонам (Android/Samsung/Pixel/Motorola/Nokia).
 * 2) Если ничего не найдено — по пользовательскому шаблону вида "PXL_YYYYMMDD".
 * Расширение файла не учитывается (ищем по имени).
 */
public class FileFinder {

    /** Результат сканирования */
    public static class Result {
        public final int totalFiles;         // всего файлов в папке (без рекурсии)
        public final int matchedFiles;       // сколько подходит под шаблон
        public final String detectedTemplate; // какой шаблон сработал (например "PXL_YYYYMMDD" или кастомный), иначе null
        public final boolean sourceMissing;  // источник не найден/не доступен
        public final boolean emptySource;    // файлов нет вовсе

        public Result(int totalFiles, int matchedFiles, String detectedTemplate,
                      boolean sourceMissing, boolean emptySource) {
            this.totalFiles = totalFiles;
            this.matchedFiles = matchedFiles;
            this.detectedTemplate = detectedTemplate;
            this.sourceMissing = sourceMissing;
            this.emptySource = emptySource;
        }
    }

    /** Предопределённые целевые паттерны */
    private static class Predef {
        final String template;       // человекочитаемый шаблон
        final Pattern regex;         // скомпилированный шаблон поиска
        Predef(String template, String regex) {
            this.template = template;
            this.regex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }
    }

    private static final List<Predef> PREDEFINED = List.of(
            // IMG_YYYYMMDD / VID_YYYYMMDD
            new Predef("IMG_YYYYMMDD", "^IMG_([0-9]{8}).*"),
            new Predef("VID_YYYYMMDD", "^VID_([0-9]{8}).*"),
            // Samsung: YYYYMMDD
            new Predef("YYYYMMDD", "^([0-9]{8}).*"),
            // Pixel: PXL_YYYYMMDD
            new Predef("PXL_YYYYMMDD", "^PXL_([0-9]{8}).*"),
            // Motorola: IMGYYYYMMDD
            new Predef("IMGYYYYMMDD", "^IMG([0-9]{8}).*"),
            // Nokia Lumia: WP_YYYYMMDD
            new Predef("WP_YYYYMMDD", "^WP_([0-9]{8}).*")
    );

    /**
     * Сканирует указанную папку (без рекурсии).
     * @param sourceDir          путь к исходной папке (может быть "G:\")
     * @param customTemplateRaw  пользовательский шаблон (например "PXL_YYYYMMDD"), может быть пустым
     */
    public Result scan(String sourceDir, String customTemplateRaw) {
        if (sourceDir == null || sourceDir.isBlank()) {
            return new Result(0, 0, null, true, false);
        }

        Path dir = Path.of(sourceDir);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return new Result(0, 0, null, true, false);
        }

        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (Files.isRegularFile(p)) {
                    names.add(p.getFileName().toString());
                }
            }
        } catch (IOException e) {
            // если не можем прочитать папку — считаем, что источник недоступен
            return new Result(0, 0, null, true, false);
        }

        if (names.isEmpty()) {
            return new Result(0, 0, null, false, true);
        }

        // 1) Сначала пробуем предопределённые шаблоны
        for (Predef def : PREDEFINED) {
            int m = countMatches(def.regex, names);
            if (m > 0) {
                return new Result(names.size(), m, def.template, false, false);
            }
        }

        // 2) Если не нашли — пробуем пользовательский шаблон
        String customTemplate = (customTemplateRaw == null) ? "" : customTemplateRaw.trim();
        if (!customTemplate.isEmpty()) {
            Pattern rx = buildRegexFromTemplate(customTemplate);
            int m = countMatches(rx, names);
            if (m > 0) {
                return new Result(names.size(), m, customTemplate, false, false);
            }
        }

        // 3) Ничего не подошло
        return new Result(names.size(), 0, null, false, false);
    }

    private static int countMatches(Pattern rx, List<String> names) {
        int c = 0;
        for (String n : names) if (rx.matcher(n).matches()) c++;
        return c;
    }

    /**
     * Преобразует строковый шаблон вида "PXL_YYYYMMDD" в Regex.
     * Сейчас поддерживаем только токен YYYYMMDD → ([0-9]{8}).
     * Остальные символы экранируем как литералы.
     */
    private static Pattern buildRegexFromTemplate(String tpl) {
        String escaped = Pattern.quote(tpl);
        // превратим литеральный текст "YYYYMMDD" в группу цифр
        String regex = escaped.replace("YYYYMMDD", "\\E([0-9]{8})\\Q") + ".*";
        return Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE);
    }
}
