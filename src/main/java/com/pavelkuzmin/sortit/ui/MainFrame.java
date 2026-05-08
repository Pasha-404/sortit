package com.pavelkuzmin.sortit.ui;

import com.pavelkuzmin.sortit.config.AppConfig;
import com.pavelkuzmin.sortit.config.ConfigIO;
import com.pavelkuzmin.sortit.core.SortProgress;
import com.pavelkuzmin.sortit.core.SortRunResult;
import com.pavelkuzmin.sortit.core.SortService;
import com.pavelkuzmin.sortit.i18n.Strings;
import com.pavelkuzmin.sortit.ui.panels.DestPanel;
import com.pavelkuzmin.sortit.ui.panels.SourcePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class MainFrame extends JFrame {
    private static final Path LOG_DIR = Path.of(".").toAbsolutePath().normalize();

    private JComboBox<String> cmbLang;
    private SourcePanel sourcePanel;
    private DestPanel destPanel;
    private JButton btnSortIt;
    private JCheckBox chkShowResults;
    private JLabel lblStatus;
    private JProgressBar progressBar;

    private AppConfig config;
    private SortWorker worker;
    private boolean suppressLangEvent = false;

    public MainFrame() {
        super("SortIt");

        config = ConfigIO.load();
        if (config == null) config = new AppConfig();
        config.normalizeLegacyFields();
        Strings.setLocale(config.lang);

        initComponents();
        setWindowIcon();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(680, 430));
        setPreferredSize(new Dimension(760, 480));
        buildUi();
        wireActions();
        restoreWindowPosition();
        loadConfigAndInit();
        wireWindowPersistence();
    }

    private void initComponents() {
        cmbLang = new JComboBox<>(new String[]{"en", "ru"});
        cmbLang.setFont(UiTheme.uiFont(Font.PLAIN, 14f));

        sourcePanel = new SourcePanel();
        destPanel = new DestPanel();

        btnSortIt = UiTheme.primaryButton(Strings.get("run.button"));
        chkShowResults = new JCheckBox(Strings.get("run.showResults"), false);
        chkShowResults.setOpaque(false);
        chkShowResults.setForeground(UiTheme.TEXT);
        chkShowResults.setFont(UiTheme.uiFont(Font.PLAIN, 13f));

        lblStatus = new JLabel(Strings.get("status.ready"));
        lblStatus.setForeground(UiTheme.MUTED);
        lblStatus.setFont(UiTheme.uiFont(Font.PLAIN, 14f));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(150, 18));
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.APP_BG);
        root.add(header(), BorderLayout.NORTH);
        root.add(content(), BorderLayout.CENTER);
        root.add(statusBar(), BorderLayout.SOUTH);
        setContentPane(root);
        pack();
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                new EmptyBorder(8, 18, 8, 18)
        ));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        titleRow.setOpaque(false);
        JLabel icon = new JLabel(loadAppIcon(28));
        JLabel title = new JLabel("SortIt");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.uiFont(Font.BOLD, 22f));
        titleRow.add(icon);
        titleRow.add(title);

        JPanel langRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        langRow.setOpaque(false);
        langRow.add(UiTheme.label(Strings.get("lang.caption")));
        langRow.add(cmbLang);

        header.add(titleRow, BorderLayout.WEST);
        header.add(langRow, BorderLayout.EAST);
        return header;
    }

    private JComponent content() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(12, 18, 12, 18));
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        body.add(sourcePanel);
        body.add(Box.createVerticalStrut(10));
        body.add(destPanel);
        body.add(Box.createVerticalStrut(12));
        body.add(actions());
        return body;
    }

    private JPanel actions() {
        JPanel actions = new JPanel(new BorderLayout(18, 0));
        actions.setOpaque(false);
        actions.add(btnSortIt, BorderLayout.WEST);
        actions.add(chkShowResults, BorderLayout.CENTER);
        return actions;
    }

    private JPanel statusBar() {
        JPanel status = new JPanel(new BorderLayout(12, 0));
        status.setBackground(Color.WHITE);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                new EmptyBorder(8, 18, 8, 18)
        ));
        status.add(lblStatus, BorderLayout.CENTER);
        status.add(progressBar, BorderLayout.EAST);
        return status;
    }

    private void wireActions() {
        cmbLang.addActionListener(e -> {
            if (suppressLangEvent) return;
            String newLang = Objects.toString(cmbLang.getSelectedItem(), "en");
            if (Objects.equals(newLang, config.lang)) return;

            config.lang = newLang;
            ConfigIO.save(config);

            Point p = getLocation();
            SwingUtilities.invokeLater(() -> {
                dispose();
                MainFrame next = new MainFrame();
                next.setLocation(p);
                next.setVisible(true);
            });
        });

        btnSortIt.addActionListener(e -> startSortAsync());
    }

    private void wireWindowPersistence() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                config.windowX = getX();
                config.windowY = getY();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                config.windowX = getX();
                config.windowY = getY();
                saveCurrentUiToConfig();
                ConfigIO.save(config);
            }
        });
    }

    private void loadConfigAndInit() {
        suppressLangEvent = true;
        try {
            cmbLang.setSelectedItem(config.lang);
        } finally {
            suppressLangEvent = false;
        }

        sourcePanel.bind(config);
        destPanel.txtDestDir.setText(config.destDir != null ? config.destDir : "");
        destPanel.txtFolderTemplate.setText(config.destTemplate != null ? config.destTemplate : "YYYYMMDD");
        chkShowResults.setSelected(config.showResults);
        updateInitialStatus();
    }

    private void updateInitialStatus() {
        if (config.sourceDir == null || config.sourceDir.isBlank()) {
            lblStatus.setText(Strings.get("status.ready"));
            return;
        }

        Path source = SortService.safePath(config.sourceDir);
        if (!SortService.isRealDir(source)) {
            lblStatus.setText(Strings.get("status.source.missing"));
            return;
        }

        long matched = SortService.countMatching(source, SortService.safeGlob(sourcePanel.txtPattern.getText()));
        if (matched > 0) {
            lblStatus.setText(MessageFormat.format(Strings.get("status.found.toProcess"), matched));
            return;
        }

        long any = SortService.countAny(source);
        lblStatus.setText(any == 0
                ? Strings.get("status.files.none")
                : MessageFormat.format(Strings.get("status.found.noneMatch"), any));
    }

    private void saveCurrentUiToConfig() {
        sourcePanel.saveTo(config);
        config.destDir = text(destPanel.txtDestDir);
        config.destTemplate = textOr(destPanel.txtFolderTemplate, "YYYYMMDD");
        config.showResults = chkShowResults.isSelected();
        config.lang = Strings.langCode();
        config.windowX = getX();
        config.windowY = getY();
        config.normalizeLegacyFields();
    }

    private void startSortAsync() {
        if (worker != null && !worker.isDone()) return;

        saveCurrentUiToConfig();
        ConfigIO.save(config);

        Path source = SortService.safePath(config.sourceDir);
        Path dest = SortService.safePath(config.destDir);
        if (!SortService.isRealDir(source)) {
            lblStatus.setText(Strings.get("status.source.missing"));
            return;
        }
        if (!SortService.isRealDir(dest)) {
            lblStatus.setText(Strings.get("status.dest.missing"));
            return;
        }

        setUiEnabled(false);
        lblStatus.setText(Strings.get("status.running"));
        progressBar.setVisible(true);
        progressBar.setValue(0);
        progressBar.setString("0%");

        worker = new SortWorker(AppConfig.copyOf(config));
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
        sourcePanel.rbMoveArchive.setEnabled(enabled);
        destPanel.txtDestDir.setEnabled(enabled);
        destPanel.btnBrowseDest.setEnabled(enabled);
        destPanel.txtFolderTemplate.setEnabled(enabled);
        chkShowResults.setEnabled(enabled);
    }

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

    private boolean isPointOnAnyScreen(Point point) {
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            if (device.getDefaultConfiguration().getBounds().contains(point)) return true;
        }
        return false;
    }

    private void setWindowIcon() {
        ImageIcon icon = loadAppIcon(64);
        if (icon.getImage() != null) {
            setIconImage(icon.getImage());
        }
    }

    private ImageIcon loadAppIcon(int size) {
        try (var in = getClass().getClassLoader().getResourceAsStream("app-icon.png")) {
            if (in == null) return new ImageIcon();
            Image image = ImageIO.read(in);
            return new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
        } catch (Exception ignore) {
            return new ImageIcon();
        }
    }

    private static String text(JTextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static String textOr(JTextField field, String fallback) {
        String value = text(field);
        return value.isEmpty() ? fallback : value;
    }

    private final class SortWorker extends SwingWorker<SortRunResult, SortProgress> {
        private final AppConfig runConfig;

        private SortWorker(AppConfig runConfig) {
            this.runConfig = runConfig;
        }

        @Override
        protected SortRunResult doInBackground() {
            SortService service = new SortService();
            return service.run(
                    runConfig,
                    LOG_DIR,
                    new SortService.Messages(Strings.get("log.noDate")),
                    progress -> publish(progress)
            );
        }

        @Override
        protected void process(List<SortProgress> chunks) {
            if (chunks == null || chunks.isEmpty()) return;
            SortProgress progress = chunks.get(chunks.size() - 1);
            if (progress.total() > 0) {
                int percent = Math.min(100, Math.max(0, (int) Math.round(100.0 * progress.processed() / progress.total())));
                progressBar.setValue(percent);
                progressBar.setString(percent + "%");
            }
            lblStatus.setText(MessageFormat.format(
                    Strings.get("status.progress"),
                    progress.processed(),
                    progress.total(),
                    progress.errors()
            ));
        }

        @Override
        protected void done() {
            setUiEnabled(true);
            progressBar.setVisible(false);
            try {
                SortRunResult result = get();
                lblStatus.setText(MessageFormat.format(Strings.get("status.done"), result.processed(), result.errors()));
                if (config.showResults && result.logPath() != null) {
                    Desktop.getDesktop().open(result.logPath().toFile());
                }
            } catch (Exception ex) {
                lblStatus.setText(Strings.get("status.failed"));
            }
        }
    }
}
