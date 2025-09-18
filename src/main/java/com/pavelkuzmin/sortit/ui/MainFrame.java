package com.pavelkuzmin.sortit.ui;

import com.pavelkuzmin.sortit.config.AppConfig;
import com.pavelkuzmin.sortit.config.ConfigIO;
import com.pavelkuzmin.sortit.core.FileFinder;
import com.pavelkuzmin.sortit.i18n.Strings;
import com.pavelkuzmin.sortit.ui.dialogs.LogViewerDialog;
import com.pavelkuzmin.sortit.ui.panels.DestPanel;
import com.pavelkuzmin.sortit.ui.panels.SourcePanel;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        setMinimumSize(new Dimension(480, 480));
        setLocationByPlatform(true);

        // Иконка окна
        try {
            var iconUrl = getClass().getResource("/app-icon.png");
            if (iconUrl != null) setIconImage(new ImageIcon(iconUrl).getImage());
        } catch (Exception ignored) {}

        // ===== Верх: только язык справа =====
        JPanel header = new JPanel(new BorderLayout(6, 6));
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        cmbLang.setEnabled(false);
        langPanel.add(new JLabel(Strings.get("lang.label")));
        langPanel.add(cmbLang);
        header.add(langPanel, BorderLayout.EAST);

        // ===== Центр: Источник → Назначение → кнопка+чекбокс =====
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

        // Применяем конфиг к UI и положению окна
        applyConfigToUi();
        applyWindowBounds();

        // Колбэки пересканирования
        sourcePanel.setOnSourceChanged(this::runScanUpdate);
        sourcePanel.setOnTemplateChanged(this::runScanUpdate);

        // Слушатели
        btnSortIt.addActionListener(e -> onSortItClicked());

        // Сохранить конфиг и геометрию при закрытии
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                writeUiToConfig();
                captureWindowBounds();
                ConfigIO.save(config);
            }
            @Override public void windowOpened(java.awt.event.WindowEvent e) {
                // Автоскан при старте, если папка существует
                runScanUpdate();
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

    // ===== сканирование без запуска обработки =====
    private void runScanUpdate() {
        String src = sourcePanel.getSourceDir();
        if (src.isBlank() || !Files.exists(Path.of(src)) || !Files.isDirectory(Path.of(src))) {
            lblStatus.setText(Strings.get("scan.source.missing"));
            return;
        }

        var res = finder.scan(src, sourcePanel.getFilenameTemplate());

        if (res.sourceMissing) {
            lblStatus.setText(Strings.get("scan.source.missing"));
            return;
        }
        if (res.emptySource) {
            lblStatus.setText(Strings.get("scan.empty"));
            return;
        }

        if (res.matchedFiles > 0) {
            if (res.detectedTemplate != null) {
                sourcePanel.setFilenameTemplate(res.detectedTemplate);
            }
            lblStatus.setText(MessageFormat.format(Strings.get("scan.found"), res.matchedFiles));
        } else {
            lblStatus.setText(MessageFormat.format(Strings.get("scan.total.zero"), res.totalFiles));
        }
    }

    // ===== запуск обработки (пока демо) =====
    private void onSortItClicked() {
        // повторим мягкую валидацию перед обработкой
        String src = sourcePanel.getSourceDir();
        if (src.isBlank() || !Files.exists(Path.of(src)) || !Files.isDirectory(Path.of(src))) {
            lblStatus.setText(Strings.get("scan.source.missing"));
            return;
        }

        // Скан перед запуском — используем текущий шаблон
        var res = finder.scan(src, sourcePanel.getFilenameTemplate());
        if (res.matchedFiles == 0) {
            if (res.emptySource) lblStatus.setText(Strings.get("scan.empty"));
            else lblStatus.setText(MessageFormat.format(Strings.get("scan.total.zero"), res.totalFiles));
            return;
        } else {
            lblStatus.setText(MessageFormat.format(Strings.get("scan.found"), res.matchedFiles));
        }

        String dst = destPanel.getDestDir();
        if (dst.isBlank()) { warn(Strings.get("warn.dest.empty")); destPanel.focusDest(); return; }

        // Сохраним UI → config
        writeUiToConfig();
        ConfigIO.save(config);

        setBusy(true);
        progress.setValue(0);
        lblStatus.setText(Strings.get("status.running"));

        SwingUtilities.invokeLater(() -> {
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            int processed = res.matchedFiles;
            int errors = 0;
            writeDemoLog(processed, errors);

            new Timer(15, ev -> {
                int v = progress.getValue();
                if (v >= 100) {
                    ((Timer) ev.getSource()).stop();
                    lblStatus.setText(MessageFormat.format(Strings.get("status.done"), processed, errors));
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

    // ===== Временная запись лога (читабельный текст) =====
    private File writeDemoLog(int processed, int errors) {
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
                        sourcePanel.isExifEnabled() ? Strings.get("log.exif.on") : Strings.get("log.exif.off"))); w.newLine();
                w.newLine();
                w.write(Strings.get("log.total")); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.processed"), processed)); w.newLine();
                w.write(MessageFormat.format(Strings.get("log.errors"), errors)); w.newLine();
            }
            return f;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    MessageFormat.format(Strings.get("error.log.write"), ex.getMessage()),
                    Strings.get("app.title"), JOptionPane.ERROR_MESSAGE);
            return null;
        }
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
