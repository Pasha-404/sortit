package com.pavelkuzmin.sortit.ui;

import com.pavelkuzmin.sortit.config.AppConfig;
import com.pavelkuzmin.sortit.config.ConfigIO;
import com.pavelkuzmin.sortit.config.DateSource;
import com.pavelkuzmin.sortit.core.ExifDateExtractor;
import com.pavelkuzmin.sortit.core.FileFinder;
import com.pavelkuzmin.sortit.core.FilenameDateParser;
import com.pavelkuzmin.sortit.i18n.Strings;
import com.pavelkuzmin.sortit.ui.dialogs.LogViewerDialog;
import com.pavelkuzmin.sortit.ui.panels.DestPanel;
import com.pavelkuzmin.sortit.ui.panels.SourcePanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainFrame extends JFrame {

    private final JComboBox<String> cmbLang = new JComboBox<>(new String[]{"RU", "EN"}); // пока выключено
    private final SourcePanel sourcePanel = new SourcePanel();
    private final DestPanel destPanel = new DestPanel();

    private final JButton btnSortIt = new JButton(Strings.get("run.button"));
    private final JCheckBox chkShowResults = new JCheckBox(Strings.get("run.showResult"), false);

    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JLabel lblStatus = new JLabel(Strings.get("status.ready"));

    private final FileFinder finder = new FileFinder();
    private AppConfig config = ConfigIO.loadOrDefaults();

    public MainFrame() {
        super(Strings.get("app.title"));

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 520)); // было 480x480
        setLocationByPlatform(true);

        try {
            var iconUrl = getClass().getResource("/app-icon.png");
            if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());
        } catch (Exception ignored) {}

        // ===== Верх =====
        JPanel header = new JPanel(new BorderLayout(6, 6));
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        cmbLang.setEnabled(false);
        langPanel.add(new JLabel(Strings.get("lang.label")));
        langPanel.add(cmbLang);
        header.add(langPanel, BorderLayout.EAST);

        // ===== Центр =====
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(header);
        center.add(Box.createVerticalStrut(6));
        center.add(sourcePanel);
        center.add(Box.createVerticalStrut(6));
        center.add(destPanel);
        center.add(Box.createVerticalStrut(8));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        btnSortIt.setFont(btnSortIt.getFont().deriveFont(Font.BOLD, 16f));
        btnSortIt.setPreferredSize(new Dimension(180, 40));
        actionRow.add(btnSortIt);
        actionRow.add(chkShowResults);
        center.add(actionRow);

        JPanel statusBar = new JPanel(new BorderLayout(6, 6));
        progress.setStringPainted(true);
        statusBar.add(progress, BorderLayout.NORTH);
        statusBar.add(lblStatus, BorderLayout.SOUTH);

        var content = getContentPane();
        content.setLayout(new BorderLayout(8, 8));
        content.add(center, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Применить конфиг
        applyConfigToUi();
        applyWindowBounds();

        // Колбэки
        sourcePanel.setOnSourceChanged(this::runScanUpdate);
        sourcePanel.setOnTemplateChanged(this::runScanUpdate);

        btnSortIt.addActionListener(e -> onSortItClicked());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                writeUiToConfig();
                captureWindowBounds();
                ConfigIO.save(config);
            }
            @Override public void windowOpened(java.awt.event.WindowEvent e) {
                runScanUpdate(); // автоскан при старте
            }
        });
    }

    // ===== конфиг/геометрия =====
    private void applyConfigToUi() {
        sourcePanel.applyConfig(config);
        destPanel.applyConfig(config);
        chkShowResults.setSelected(config.showResult);
        lblStatus.setText(Strings.get("status.ready"));
    }

    private void writeUiToConfig() {
        sourcePanel.writeToConfig(config);
        destPanel.writeToConfig(config);
        config.showResult = chkShowResults.isSelected();
    }

    private void applyWindowBounds() {
        if (config.windowW > 0 && config.windowH > 0) setSize(config.windowW, config.windowH);
        if (config.windowX >= 0 && config.windowY >= 0) setLocation(config.windowX, config.windowY);
    }

    private void captureWindowBounds() {
        Point p = getLocationOnScreen();
        Dimension d = getSize();
        config.windowX = p.x;
        config.windowY = p.y;
        config.windowW = d.width;
        config.windowH = d.height;
    }

    // ===== сканирование без обработки =====
    private void runScanUpdate() {
        String src = sourcePanel.getSourceDir();
        if (src.isBlank() || !Files.exists(Path.of(src)) || !Files.isDirectory(Path.of(src))) {
            lblStatus.setText(Strings.get("scan.source.missing"));
            return;
        }

        var res = finder.scan(src, sourcePanel.getFilenameTemplate());

        if (res.sourceMissing) { lblStatus.setText(Strings.get("scan.source.missing")); return; }
        if (res.emptySource)   { lblStatus.setText(Strings.get("scan.empty")); return; }

        // Подставить шаблон в поле (по правилам)
        if (res.detectedTemplate != null) {
            sourcePanel.setFilenameTemplate(res.detectedTemplate);
        }

        if (res.matchedFiles > 0) {
            lblStatus.setText(MessageFormat.format(Strings.get("scan.found"), res.matchedFiles));
        } else {
            lblStatus.setText(MessageFormat.format(Strings.get("scan.total.zero"), res.totalFiles));
        }
    }

    // ===== запуск обработки =====
    private void onSortItClicked() {
        String src = sourcePanel.getSourceDir();
        if (src.isBlank() || !Files.exists(Path.of(src)) || !Files.isDirectory(Path.of(src))) {
            lblStatus.setText(Strings.get("scan.source.missing"));
            return;
        }

        // Скан по текущему шаблону
        var res = finder.scan(src, sourcePanel.getFilenameTemplate());
        if (res.emptySource) {
            lblStatus.setText(Strings.get("scan.empty"));
            return;
        }
        if (res.matchedFiles == 0) {
            lblStatus.setText(MessageFormat.format(Strings.get("scan.total.zero"), res.totalFiles));
            return;
        } else {
            lblStatus.setText(MessageFormat.format(Strings.get("scan.found"), res.matchedFiles));
        }

        String dst = destPanel.getDestDir();
        if (dst.isBlank()) { warn(Strings.get("warn.dest.empty")); destPanel.focusDest(); return; }

        writeUiToConfig();
        ConfigIO.save(config);

        setBusy(true);
        progress.setValue(0);
        lblStatus.setText(Strings.get("status.running"));

        // Сама "обработка": проходим по файлам, согласно шаблону, валидируем дату.
        SwingUtilities.invokeLater(() -> {
            List<String> errors = new ArrayList<>();
            int processed = 0;

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(Path.of(src))) {
                // соберём regex из glob-шаблона
                var rx = globToRegex(sourcePanel.getFilenameTemplate());
                for (Path p : ds) {
                    if (!Files.isRegularFile(p)) continue;
                    String name = p.getFileName().toString();
                    if (!rx.matcher(name).matches()) continue;

                    processed++;

                    LocalDate date = null;
                    switch (sourcePanel.getDateSource()) {
                        case METADATA -> {
                            var d = ExifDateExtractor.readDate(p.toFile());
                            if (d.isPresent()) date = d.get();
                            else errors.add(MessageFormat.format(Strings.get("error.no.metadata"), name));
                        }
                        case CREATED -> {
                            try {
                                var attrs = Files.readAttributes(p, java.nio.file.attribute.BasicFileAttributes.class);
                                var ct = attrs.creationTime(); // может совпадать с lastModified на некоторых ФС
                                if (ct != null) {
                                    date = ct.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                                } else {
                                    errors.add(MessageFormat.format(Strings.get("error.no.created"), name));
                                }
                            } catch (Exception ex) {
                                errors.add(MessageFormat.format(Strings.get("error.no.created"), name));
                            }
                        }
                        case FILENAME -> {
                            var d = FilenameDateParser.parse(name);
                            if (d.isPresent()) date = d.get();
                            else errors.add(MessageFormat.format(Strings.get("error.no.date.in.name"), name));
                        }
                    }

                    // здесь позже будет реальное копирование/перенос по шаблону назначения на основе date
                }
            } catch (Exception e) {
                errors.add("Internal error: " + e.getMessage());
            }

            final int processedCount = processed;      // <- делаем final
            final int errorsCount = errors.size();     // <- делаем final
            writeProcessLog(processedCount, errors);

            new Timer(15, ev -> {
                int v = progress.getValue();
                if (v >= 100) {
                    ((Timer) ev.getSource()).stop();
                    lblStatus.setText(MessageFormat.format(Strings.get("status.done"), processedCount, errorsCount));
                    setBusy(false);
                    if (chkShowResults.isSelected()) showLatestLog();
                } else {
                    progress.setValue(v + 2);
                }
            }).start();
        });
    }

    private void setBusy(boolean busy) {
        btnSortIt.setEnabled(!busy);
        sourcePanel.setEnabledAll(!busy);
        destPanel.setEnabledAll(!busy);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, Strings.get("app.title"), JOptionPane.WARNING_MESSAGE);
    }

    // ===== Запись лога обработки (с ошибками) =====
    private File writeProcessLog(int processed, List<String> errors) {
        try {
            String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File f = new File("sortit-" + ts + ".log");
            try (BufferedWriter w = new BufferedWriter(new FileWriter(f, false))) {
                w.write(Strings.get("log.header")); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.datetime"), new Date())); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.source"), sourcePanel.getSourceDir())); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.dest"), destPanel.getDestDir())); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.template"), destPanel.getTemplate())); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.mode"),
                        sourcePanel.isCopyMode() ? Strings.get("log.mode.copy") : Strings.get("log.mode.move"))); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.exif"),
                        (sourcePanel.getDateSource() == DateSource.METADATA) ? Strings.get("log.exif.on") : Strings.get("log.exif.off"))); w.newLine();
                w.newLine();
                w.write(Strings.get("log.total")); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.processed"), processed)); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.errors"), errors.size())); w.newLine();
                if (!errors.isEmpty()) {
                    w.newLine();
                    w.write(Strings.get("log.errors.header")); w.newLine();
                    for (int i = 0; i < errors.size(); i++) {
                        w.write((i + 1) + ") " + errors.get(i)); w.newLine();
                    }
                }
            }
            return f;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    MessageFormat.format(Strings.get("error.log.write"), ex.getMessage()),
                    Strings.get("app.title"), JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    // === утилита: glob -> regex (копия из FileFinder, чтобы не тянуть приватный) ===
    private static java.util.regex.Pattern globToRegex(String glob) {
        String g = (glob == null || glob.isBlank()) ? "*.*" : glob.trim();
        StringBuilder sb = new StringBuilder("^");
        for (char ch : g.toCharArray()) {
            switch (ch) {
                case '*': sb.append(".*"); break;
                case '?': sb.append('.'); break;
                case '.': sb.append("\\."); break;
                default:
                    if ("+()^$|[]{}".indexOf(ch) >= 0) sb.append('\\');
                    sb.append(ch);
            }
        }
        sb.append('$');
        return java.util.regex.Pattern.compile(sb.toString(), java.util.regex.Pattern.CASE_INSENSITIVE);
    }

    private void showLatestLog() {
        File dir = new File(".");
        File[] files = dir.listFiles((d, name) -> name.startsWith("sortit-") && name.endsWith(".log"));
        if (files == null || files.length == 0) {
            JOptionPane.showMessageDialog(this, Strings.get("warn.log.notfound"), Strings.get("app.title"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        File latest = files[0];
        for (File f : files) if (f.lastModified() > latest.lastModified()) latest = f;

        try {
            LogViewerDialog.showLog(this, latest);
        } catch (Exception ex) {
            try { Desktop.getDesktop().edit(latest); }
            catch (Exception ex2) {
                JOptionPane.showMessageDialog(this,
                        MessageFormat.format(Strings.get("error.log.open"), latest.getAbsolutePath()),
                        Strings.get("app.title"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
