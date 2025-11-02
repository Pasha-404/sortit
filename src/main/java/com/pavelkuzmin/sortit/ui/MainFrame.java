package com.pavelkuzmin.sortit.ui;

import com.pavelkuzmin.sortit.config.AppConfig;
import com.pavelkuzmin.sortit.config.ConfigIO;
import com.pavelkuzmin.sortit.core.MediaDateExtractor;
import com.pavelkuzmin.sortit.i18n.Strings;
import com.pavelkuzmin.sortit.ui.panels.DestPanel;
import com.pavelkuzmin.sortit.ui.panels.SourcePanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainFrame extends JFrame {

    private JComboBox<String> cmbLang;
    private SourcePanel sourcePanel;
    public  DestPanel   destPanel;

    private JButton   btnSortIt;
    private JCheckBox chkShowResults;

    private JLabel       lblStatus;
    private JProgressBar progressBar;

    private AppConfig config;
    private SortWorker worker;
    private boolean suppressLangEvent = false;

    public MainFrame() {
        super("SortIt");

        // 1) Конфиг и язык
        config = ConfigIO.load();
        if (config == null) config = new AppConfig();
        config.normalizeLegacyFields();
        if (config.lang == null || config.lang.isBlank()) config.lang = "en"; // первый запуск — EN
        Strings.setLocale(config.lang);

        // 2) Компоненты ПОСЛЕ setLocale
        initComponents();

        // 2.1) ИКОНКА ОКНА — используем существующие ресурсы (app-icon.png / icons/*.ico)
        setWindowIcon();

        // 3) UI и действия
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(520, 480));
        buildUi();
        wireActions();

        // 4) Восстановить позицию окна из конфига
        restoreWindowPosition();

        // 5) Данные в контролы + стартовое состояние
        loadConfigAndInit();

        // 6) Сохранять координаты при каждом перемещении
        addComponentListener(new ComponentAdapter() {
            @Override public void componentMoved(ComponentEvent e) {
                config.windowX = getX();
                config.windowY = getY();
            }
        });

        // 7) Сохранение при закрытии
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                config.windowX = getX();
                config.windowY = getY();
                saveCurrentUiToConfig();
                ConfigIO.save(config);
            }
        });
    }

    private void initComponents() {
        cmbLang = new JComboBox<>(new String[]{"en", "ru"});
        sourcePanel = new SourcePanel();
        destPanel   = new DestPanel();

        btnSortIt      = new JButton(Strings.get("run.button"));
        chkShowResults = new JCheckBox(Strings.get("run.showResults"), false);

        lblStatus   = new JLabel(Strings.get("status.ready"));
        progressBar = new JProgressBar(0, 100);
    }

    /** Ставим иконку окна строго из тех ресурсов, что у тебя есть:
     *  - приоритет: app-icon.png (в корне resources)
     *  - запасной вариант: icons/app-icon.png
     *  - ещё запасные: icons/app.ico или icons/app-icon.ico (через Toolkit)
     */
    private void setWindowIcon() {
        Image img = null;
        ClassLoader cl = getClass().getClassLoader();

        // 1) PNG в корне ресурсов
        String[] pngCandidates = { "app-icon.png", "icons/app-icon.png" };
        for (String path : pngCandidates) {
            try (var in = cl.getResourceAsStream(path)) {
                if (in != null) {
                    Image png = javax.imageio.ImageIO.read(in);
                    if (png != null) { img = png; break; }
                }
            } catch (Exception ignore) {}
        }

        // 2) Fallback: ICO через Toolkit (ImageIO .ico не понимает)
        if (img == null) {
            String[] icoCandidates = { "icons/app.ico", "icons/app-icon.ico" };
            for (String path : icoCandidates) {
                try {
                    var url = cl.getResource(path);
                    if (url != null) {
                        Image ico = Toolkit.getDefaultToolkit().getImage(url);
                        if (ico != null) { img = ico; break; }
                    }
                } catch (Exception ignore) {}
            }
        }

        if (img != null) {
            setIconImage(img);
        }
    }

    private void buildUi() {
        // Верх: выбор языка
        JPanel header = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        header.add(new JLabel(Strings.get("lang.caption")));
        header.add(cmbLang);

        // Центр: две панели
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(wrap(sourcePanel));
        center.add(Box.createVerticalStrut(8));
        center.add(wrap(destPanel));

        // Кнопка + чекбокс
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        btnSortIt.setFont(btnSortIt.getFont().deriveFont(Font.BOLD, 16f));
        btnSortIt.setPreferredSize(new Dimension(200, 40));
        btnSortIt.setFocusPainted(false);
        actions.add(btnSortIt);
        actions.add(chkShowResults);

        // Низ: статус + прогресс (с отступами)
        JPanel status = new JPanel(new BorderLayout(8, 6));
        status.setBorder(new EmptyBorder(6, 12, 6, 12));
        status.add(lblStatus, BorderLayout.WEST);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        status.add(progressBar, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.add(header, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout(0, 0));
        getContentPane().add(content, BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.PAGE_END);

        pack();
    }

    private static JPanel wrap(JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(6, 6, 6, 6));
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    private void wireActions() {
        // Переключение языка → пересоздаём окно, чтобы все строки точно обновились
        cmbLang.addActionListener(e -> {
            if (suppressLangEvent) return;
            String newLang = Objects.toString(cmbLang.getSelectedItem(), "en");
            if (Objects.equals(newLang, config.lang)) return;

            config.lang = newLang;
            Strings.setLocale(config.lang);
            ConfigIO.save(config);

            Point p = getLocation();
            SwingUtilities.invokeLater(() -> {
                dispose();
                MainFrame nf = new MainFrame();
                nf.setLocation(p);
                nf.setVisible(true);
            });
        });

        btnSortIt.addActionListener(e -> startSortAsync());
    }

    private void loadConfigAndInit() {
        // «тихо» выставляем язык
        suppressLangEvent = true;
        try { cmbLang.setSelectedItem(config.lang); }
        finally { suppressLangEvent = false; }

        // Поля
        sourcePanel.bind(config);
        destPanel.txtDestDir.setText(config.destDir != null ? config.destDir : "");
        destPanel.txtFolderTemplate.setText(config.destTemplate != null ? config.destTemplate : "YYYYMMDD");
        chkShowResults.setSelected(Boolean.TRUE.equals(config.showResults));

        // Стартовый статус: если источник пуст/нет — без сканирования
        if (config.sourceDir == null || config.sourceDir.isBlank()) {
            lblStatus.setText(Strings.get("status.ready"));
            return;
        }
        Path src = safePath(config.sourceDir);
        if (!isRealDir(src)) {
            lblStatus.setText(Strings.get("status.source.missing"));
            return;
        }

        long matched = countMatching(src, safeGlob(sourcePanel.txtPattern.getText()));
        if (matched > 0) {
            lblStatus.setText(MessageFormat.format(Strings.get("status.found.toProcess"), matched));
        } else {
            long any = countAny(src);
            if (any == 0) lblStatus.setText(Strings.get("status.files.none"));
            else lblStatus.setText(MessageFormat.format(Strings.get("status.found.noneMatch"), any));
        }
    }

    private void saveCurrentUiToConfig() {
        sourcePanel.saveTo(config);
        config.destDir      = text(destPanel.txtDestDir);
        config.destTemplate = textOr(destPanel.txtFolderTemplate, "YYYYMMDD");
        config.showResults  = chkShowResults.isSelected();
        if (config.lang == null || config.lang.isBlank())
            config.lang = Strings.langCode();

        // Координаты окна
        config.windowX = getX();
        config.windowY = getY();
    }

    private void startSortAsync() {
        if (worker != null && !worker.isDone()) return;

        saveCurrentUiToConfig();
        ConfigIO.save(config);

        final Path src = safePath(config.sourceDir);
        final Path dst = safePath(config.destDir);
        if (!isRealDir(src)) { lblStatus.setText(Strings.get("status.source.missing")); return; }
        if (!isRealDir(dst)) { lblStatus.setText(Strings.get("status.dest.missing"));   return; }

        setUiEnabled(false);
        lblStatus.setText(Strings.get("status.running"));
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setString("0%");

        worker = new SortWorker(config);
        worker.execute();
    }

    private void setUiEnabled(boolean enabled) {
        btnSortIt.setEnabled(enabled);
        cmbLang.setEnabled(enabled);

        sourcePanel.txtSourceDir.setEnabled(enabled);
        sourcePanel.btnBrowseSource.setEnabled(enabled);
        sourcePanel.txtPattern.setEnabled(enabled);
        sourcePanel.rbDateMetadata.setEnabled(enabled);
        sourcePanel.rbDateFilename.setEnabled(enabled);
        sourcePanel.rbDateCreated.setEnabled(enabled);
        sourcePanel.rbCopy.setEnabled(enabled);
        sourcePanel.rbMove.setEnabled(enabled);
        sourcePanel.rbCopyArchive.setEnabled(enabled);

        destPanel.txtDestDir.setEnabled(enabled);
        destPanel.btnBrowseDest.setEnabled(enabled);
        destPanel.txtFolderTemplate.setEnabled(enabled);

        chkShowResults.setEnabled(enabled);
    }

    // ======== восстановление позиции окна ========

    private void restoreWindowPosition() {
        if (config.windowX != null && config.windowY != null) {
            Point saved = new Point(config.windowX, config.windowY);
            if (isPointOnAnyScreen(saved)) {
                setLocation(saved);
                return;
            }
        }
        setLocationRelativeTo(null);
    }

    private boolean isPointOnAnyScreen(Point p) {
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle b = gd.getDefaultConfiguration().getBounds();
            if (b.contains(p)) return true;
        }
        return false;
    }

    // ======== Worker ========

    private final class SortWorker extends SwingWorker<Integer, Progress> {
        private final AppConfig cfg;
        private final List<Path> files;
        private int processed = 0;
        private int errors = 0;
        private final StringBuilder log = new StringBuilder();

        SortWorker(AppConfig cfg) {
            this.cfg = cfg;
            Path src = safePath(cfg.sourceDir);
            this.files = collectMatching(src, safeGlob(cfg.filenameTemplate));
        }

        @Override
        protected Integer doInBackground() {
            final int total = files.size();
            if (total == 0) {
                publish(new Progress(0, 0, 0));
                return 0;
            }
            final Path destRoot = safePath(cfg.destDir);
            final Path bakRoot  = safePath(cfg.sourceDir).resolve("BAK");

            for (Path srcFile : files) {
                if (isCancelled()) break;
                final String name = srcFile.getFileName().toString();

                try {
                    LocalDate date = resolveDate(srcFile, cfg);
                    if (date == null) {
                        errors++;
                        log.append("- ERROR: ").append(name).append(" — ").append(Strings.get("log.noDate")).append(System.lineSeparator());
                        processed++;
                        if (tick(processed, total)) publish(new Progress(processed, total, errors));
                        continue;
                    }

                    final String sub = formatByTemplate(cfg.destTemplate, date);
                    final Path destFolder = destRoot.resolve(sub);
                    try { Files.createDirectories(destFolder); }
                    catch (IOException ce) {
                        errors++;
                        log.append("- ERROR: ").append(name).append(" — cannot create dest dir: ").append(destFolder).append(System.lineSeparator());
                        processed++;
                        if (tick(processed, total)) publish(new Progress(processed, total, errors));
                        continue;
                    }

                    final Path destPath = destFolder.resolve(name);

                    switch (cfg.mode) {
                        case COPY -> {
                            if (Files.exists(destPath)) {
                                errors++;
                                log.append("- ERROR: ").append(name).append(" — already exists at dest: ").append(destPath).append(System.lineSeparator());
                            } else {
                                Files.copy(srcFile, destPath);
                            }
                        }
                        case MOVE -> {
                            if (Files.exists(destPath)) {
                                errors++;
                                log.append("- ERROR: ").append(name).append(" — already exists at dest: ").append(destPath).append(System.lineSeparator());
                            } else {
                                Files.move(srcFile, destPath);
                            }
                        }
                        case COPY_ARCHIVE -> {
                            if (Files.exists(destPath)) {
                                errors++;
                                log.append("- ERROR: ").append(name).append(" — already exists at dest: ").append(destPath).append(System.lineSeparator());
                            } else {
                                Files.copy(srcFile, destPath);
                                final Path bakFolder = bakRoot.resolve(sub);
                                try {
                                    Files.createDirectories(bakFolder);
                                    final Path bakPath = bakFolder.resolve(name);
                                    if (Files.exists(bakPath)) {
                                        log.append("- WARN: ").append(name).append(" — already exists in BAK, skipped: ").append(bakPath).append(System.lineSeparator());
                                    } else {
                                        Files.move(srcFile, bakPath);
                                    }
                                } catch (IOException be) {
                                    log.append("- WARN: ").append(name).append(" — cannot move to BAK: ").append(bakFolder).append(System.lineSeparator());
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    errors++;
                    log.append("- ERROR: ").append(name).append(" — ").append(ex.getMessage()).append(System.lineSeparator());
                }

                processed++;
                if (tick(processed, total)) publish(new Progress(processed, total, errors));
            }

            log.append(System.lineSeparator())
                    .append("Template: ").append(cfg.destTemplate).append(System.lineSeparator())
                    .append("Mode: ").append(cfg.mode).append(System.lineSeparator())
                    .append("Processed: ").append(processed).append(" / ").append(total)
                    .append(" | Errors: ").append(errors).append(System.lineSeparator());

            return processed;
        }

        @Override
        protected void process(List<Progress> chunks) {
            if (chunks == null || chunks.isEmpty()) return;
            Progress p = chunks.get(chunks.size() - 1);
            if (p.total > 0) {
                int percent = Math.min(100, Math.max(0, (int)Math.round(100.0 * p.processed / p.total)));
                progressBar.setValue(percent);
                progressBar.setString(percent + "%");
            }
            lblStatus.setText(MessageFormat.format(Strings.get("status.progress"), p.processed, p.total, p.errors));
        }

        @Override
        protected void done() {
            setUiEnabled(true);
            progressBar.setVisible(false);
            lblStatus.setText(MessageFormat.format(Strings.get("status.done"), processed, errors));

            Path logPath = Paths.get("sort-" + nowStamp() + ".log").toAbsolutePath();
            try (BufferedWriter bw = Files.newBufferedWriter(logPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                bw.write(log.toString());
            } catch (IOException ignore) { }

            if (Boolean.TRUE.equals(config.showResults)) {
                try { Desktop.getDesktop().open(logPath.toFile()); } catch (Exception ignore) { }
            }
        }

        private boolean tick(int processed, int total) {
            return processed == total || (processed % 5 == 0);
        }
    }

    // ===== util =====

    private static final class Progress {
        final int processed, total, errors;
        Progress(int processed, int total, int errors) {
            this.processed = processed; this.total = total; this.errors = errors;
        }
    }

    private static Path safePath(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Paths.get(s); } catch (Exception e) { return null; }
    }

    private static boolean isRealDir(Path p) {
        try { return p != null && Files.isDirectory(p); }
        catch (Exception ignore) { return false; }
    }

    private static String text(JTextField tf)            { return tf.getText() == null ? "" : tf.getText().trim(); }
    private static String textOr(JTextField tf, String d) { String t = text(tf); return t.isEmpty() ? d : t; }
    private static String safeGlob(String g)              { return (g == null || g.isBlank()) ? "*.*" : g.trim(); }

    private static long countAny(Path dir) {
        if (!isRealDir(dir)) return 0L;
        try (Stream<Path> s = Files.list(dir)) { return s.filter(Files::isRegularFile).count(); }
        catch (IOException e) { return 0L; }
    }

    private static long countMatching(Path dir, String glob) {
        if (!isRealDir(dir)) return 0L;
        try (Stream<Path> s = Files.list(dir)) {
            PathMatcher m = dir.getFileSystem().getPathMatcher("glob:" + glob);
            return s.filter(Files::isRegularFile).filter(p -> m.matches(p.getFileName())).count();
        } catch (IOException e) { return 0L; }
    }

    private static List<Path> collectMatching(Path dir, String glob) {
        if (!isRealDir(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            PathMatcher m = dir.getFileSystem().getPathMatcher("glob:" + glob);
            return s.filter(Files::isRegularFile)
                    .filter(p -> m.matches(p.getFileName()))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) { return List.of(); }
    }

    private static String nowStamp() {
        var z = java.time.ZonedDateTime.now();
        return String.format("%04d%02d%02d-%02d%02d", z.getYear(), z.getMonthValue(), z.getDayOfMonth(), z.getHour(), z.getMinute());
    }

    private static String formatByTemplate(String tmpl, LocalDate d) {
        if (tmpl == null || tmpl.isBlank()) return "UNKNOWN";
        String out = tmpl;
        out = out.replace("YYYY", String.format("%04d", d.getYear()));
        out = out.replace("YY",   String.format("%02d", d.getYear() % 100));
        out = out.replace("MM",   String.format("%02d", d.getMonthValue()));
        out = out.replace("DD",   String.format("%02d", d.getDayOfMonth()));
        out = out.replaceAll("[^0-9._-]", "");
        return out.isBlank() ? "UNKNOWN" : out;
    }

    private static LocalDate resolveDate(Path file, AppConfig cfg) throws IOException {
        switch (cfg.dateSource) {
            case METADATA -> {
                Date dt = MediaDateExtractor.getBestDate(file.toFile());
                if (dt == null) return null;
                return dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            case FILENAME -> {
                String name = file.getFileName().toString();
                var m = java.util.regex.Pattern.compile("(20\\d{2}|19\\d{2})(\\d{2})(\\d{2})").matcher(name);
                if (m.find()) {
                    try {
                        int y = Integer.parseInt(m.group(1));
                        int mm = Integer.parseInt(m.group(2));
                        int dd = Integer.parseInt(m.group(3));
                        return LocalDate.of(y, mm, dd);
                    } catch (Exception ignore) { return null; }
                }
                return null;
            }
            case CREATED -> {
                try {
                    var attr = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class);
                    var inst = attr.creationTime() != null ? attr.creationTime().toInstant() : null;
                    if (inst == null) inst = attr.lastModifiedTime() != null ? attr.lastModifiedTime().toInstant() : null;
                    if (inst == null) return null;
                    return inst.atZone(ZoneId.systemDefault()).toLocalDate();
                } catch (Exception ex) {
                    return null;
                }
            }
        }
        return null;
    }
}
