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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class MainFrame extends JFrame {
    private static final Path LOG_DIR = Path.of(".").toAbsolutePath().normalize();

    private JTextField txtSourceDir;
    private JButton btnBrowseSource;
    private JButton btnSettings;
    private JButton btnSortIt;
    private JLabel lblStatus;
    private JProgressBar progressBar;

    private JLabel lblDateSourceValue;
    private JLabel lblModeValue;
    private JLabel lblFolderTemplateValue;
    private JLabel lblFilePatternValue;
    private JLabel lblShowResultsValue;
    private JLabel lblDestDirValue;
    private JTextArea txtActionSummary;

    private AppConfig config;
    private SortWorker worker;

    public MainFrame() {
        super("SortIt");

        config = ConfigIO.load();
        if (config == null) config = new AppConfig();
        config.normalizeLegacyFields();
        Strings.setLocale(config.lang);

        initComponents();
        setWindowIcon();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 440));
        setPreferredSize(new Dimension(900, 520));
        buildUi();
        wireActions();
        restoreWindowPosition();
        loadConfigAndInit();
        wireWindowPersistence();
    }

    private void initComponents() {
        txtSourceDir = new JTextField();
        UiTheme.styleTextField(txtSourceDir);
        txtSourceDir.setFont(UiTheme.uiFont(Font.PLAIN, 16f));
        txtSourceDir.setPreferredSize(new Dimension(80, 36));

        btnBrowseSource = UiTheme.secondaryButton("...");
        btnBrowseSource.setPreferredSize(new Dimension(128, 36));

        btnSettings = UiTheme.secondaryButton(Strings.get("settings.button"));
        btnSettings.setPreferredSize(new Dimension(120, 34));

        btnSortIt = UiTheme.primaryButton(Strings.get("run.button"));
        btnSortIt.setPreferredSize(new Dimension(280, 56));

        lblStatus = new JLabel(Strings.get("status.ready"));
        lblStatus.setForeground(UiTheme.MUTED);
        lblStatus.setFont(UiTheme.uiFont(Font.PLAIN, 14f));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(150, 16));

        lblDateSourceValue = valueLabel();
        lblModeValue = valueLabel();
        lblFolderTemplateValue = valueLabel();
        lblFilePatternValue = valueLabel();
        lblShowResultsValue = valueLabel();
        lblDestDirValue = valueLabel();

        txtActionSummary = new JTextArea();
        txtActionSummary.setEditable(false);
        txtActionSummary.setFocusable(false);
        txtActionSummary.setLineWrap(true);
        txtActionSummary.setWrapStyleWord(true);
        txtActionSummary.setRows(2);
        txtActionSummary.setOpaque(false);
        txtActionSummary.setForeground(UiTheme.MUTED);
        txtActionSummary.setFont(UiTheme.uiFont(Font.PLAIN, 14f));
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

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        JLabel icon = new JLabel(loadAppIcon(28));
        JLabel title = new JLabel("SortIt");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.uiFont(Font.BOLD, 22f));
        titleRow.add(icon);
        titleRow.add(title);

        JPanel settingsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        settingsRow.setOpaque(false);
        settingsRow.add(btnSettings);

        header.add(titleRow, BorderLayout.WEST);
        header.add(settingsRow, BorderLayout.EAST);
        return header;
    }

    private JComponent content() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 22, 16, 22));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;
        c.insets = new Insets(0, 0, 12, 0);
        body.add(sourceCard(), c);

        c.gridy = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(workCards(), c);
        return body;
    }

    private JComponent sourceCard() {
        UiTheme.CardPanel card = new UiTheme.CardPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(12, 18, 12, 18));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;

        JLabel title = new JLabel(Strings.get("main.source.title"));
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.uiFont(Font.BOLD, 15f));
        c.gridx = 0;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 14);
        card.add(title, c);

        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 10);
        card.add(txtSourceDir, c);

        c.gridx = 2;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(btnBrowseSource, c);
        return card;
    }

    private JComponent workCards() {
        JPanel cards = new JPanel(new GridLayout(1, 2, 12, 0));
        cards.setOpaque(false);
        cards.add(paramsCard());
        cards.add(actionCard());
        return cards;
    }

    private JComponent paramsCard() {
        UiTheme.CardPanel card = new UiTheme.CardPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 14, 0);
        card.add(sectionTitle(Strings.get("main.params.title")), c);

        c.gridy++;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(summaryRow(Strings.get("src.date.title"), lblDateSourceValue), c);
        c.gridy++;
        card.add(summaryRow(Strings.get("src.mode.title"), lblModeValue), c);
        c.gridy++;
        card.add(summaryRow(Strings.get("dst.template"), lblFolderTemplateValue), c);
        c.gridy++;
        card.add(summaryRow(Strings.get("src.pattern"), lblFilePatternValue), c);
        c.gridy++;
        card.add(summaryRow(Strings.get("main.showResults"), lblShowResultsValue), c);

        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        card.add(Box.createGlue(), c);
        return card;
    }

    private JComponent actionCard() {
        UiTheme.CardPanel card = new UiTheme.CardPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 0);
        card.add(sectionTitle(Strings.get("main.destination.title")), c);

        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        card.add(destinationBox(), c);

        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        card.add(actionSummaryBox(), c);

        c.gridy++;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        card.add(Box.createGlue(), c);

        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(btnSortIt, c);
        return card;
    }

    private JComponent destinationBox() {
        JPanel box = new JPanel(new BorderLayout(12, 0));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER, 1),
                new EmptyBorder(11, 14, 11, 14)
        ));

        JLabel label = UiTheme.label(Strings.get("main.dest.folder"));
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.uiFont(Font.PLAIN, 14f));
        box.add(label, BorderLayout.WEST);
        box.add(lblDestDirValue, BorderLayout.EAST);
        return box;
    }

    private JComponent actionSummaryBox() {
        JPanel box = new JPanel(new BorderLayout(14, 0));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(148, 190, 255), 1),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel(Strings.get("main.what.title"));
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.uiFont(Font.BOLD, 17f));
        text.add(title, BorderLayout.NORTH);
        text.add(txtActionSummary, BorderLayout.CENTER);
        box.add(text, BorderLayout.CENTER);
        return box;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.uiFont(Font.BOLD, 17f));
        return label;
    }

    private JComponent summaryRow(String title, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(7, 0, 7, 0)
        ));

        JLabel label = UiTheme.label(trimTrailingColon(title));
        label.setFont(UiTheme.uiFont(Font.PLAIN, 15f));
        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private JLabel valueLabel() {
        JLabel label = new JLabel();
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.uiFont(Font.PLAIN, 15f));
        return label;
    }

    private JComponent iconTile(Icon icon) {
        JPanel tile = new JPanel(new GridBagLayout());
        tile.setOpaque(false);
        tile.setPreferredSize(new Dimension(56, 56));
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 232, 250), 1),
                new EmptyBorder(8, 8, 8, 8)
        ));
        tile.add(new JLabel(icon));
        return tile;
    }

    private JPanel statusBar() {
        JPanel status = new JPanel(new BorderLayout(12, 0));
        status.setBackground(Color.WHITE);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                new EmptyBorder(10, 18, 10, 18)
        ));
        status.add(lblStatus, BorderLayout.CENTER);
        status.add(progressBar, BorderLayout.EAST);
        return status;
    }

    private void wireActions() {
        txtSourceDir.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                sourceChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                sourceChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                sourceChanged();
            }
        });

        btnBrowseSource.addActionListener(e -> browseSource());
        btnSettings.addActionListener(e -> showSettingsDialog());
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
        txtSourceDir.setText(config.sourceDir != null ? config.sourceDir : "");
        updateSummary();
        updateInitialStatus();
    }

    private void sourceChanged() {
        config.sourceDir = text(txtSourceDir);
        updateSummary();
        updateInitialStatus();
    }

    private void browseSource() {
        File start = startDir(txtSourceDir.getText());
        JFileChooser chooser = new JFileChooser(start != null ? start : new File(System.getProperty("user.home")));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtSourceDir.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void showSettingsDialog() {
        saveCurrentUiToConfig();
        String oldLang = config.lang;

        SettingsDialog dialog = new SettingsDialog(AppConfig.copyOf(config));
        dialog.setVisible(true);
        if (!dialog.isAccepted()) {
            return;
        }

        config = dialog.selectedConfig();
        config.normalizeLegacyFields();
        ConfigIO.save(config);

        if (!Objects.equals(oldLang, config.lang)) {
            Point p = getLocation();
            SwingUtilities.invokeLater(() -> {
                dispose();
                MainFrame next = new MainFrame();
                next.setLocation(p);
                next.setVisible(true);
            });
            return;
        }

        loadConfigAndInit();
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

        long matched = SortService.countMatching(source, SortService.safeGlob(config.filenameTemplate));
        if (matched > 0) {
            lblStatus.setText(MessageFormat.format(Strings.get("status.found.toProcess"), matched));
            return;
        }

        long any = SortService.countAny(source);
        lblStatus.setText(any == 0
                ? Strings.get("status.files.none")
                : MessageFormat.format(Strings.get("status.found.noneMatch"), any));
    }

    private void updateSummary() {
        lblDateSourceValue.setText(dateSourceText(config.dateSource));
        lblModeValue.setText(modeText(config.mode));
        lblFolderTemplateValue.setText(textOrValue(config.destTemplate, "YYYYMMDD"));
        lblFilePatternValue.setText(textOrValue(config.filenameTemplate, "*.*"));
        lblShowResultsValue.setText(Strings.get(config.showResults ? "main.enabled" : "main.disabled"));
        lblDestDirValue.setText(textOrValue(config.destDir, Strings.get("main.common.notSet")));
        txtActionSummary.setText(actionSummaryText(config.mode, textOrValue(config.destTemplate, "YYYYMMDD")));
    }

    private void saveCurrentUiToConfig() {
        config.sourceDir = text(txtSourceDir);
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
        btnSettings.setEnabled(enabled);
        txtSourceDir.setEnabled(enabled);
        btnBrowseSource.setEnabled(enabled);
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

    private String actionSummaryText(AppConfig.OperationMode mode, String template) {
        return MessageFormat.format(Strings.get(summaryKey(mode)), template);
    }

    private String summaryKey(AppConfig.OperationMode mode) {
        return switch (mode.normalized()) {
            case COPY -> "main.summary.copy";
            case MOVE -> "main.summary.move";
            case MOVE_ARCHIVE -> "main.summary.moveArchive";
            case COPY_ARCHIVE -> "main.summary.moveArchive";
        };
    }

    private String dateSourceText(AppConfig.DateSource source) {
        return switch (source) {
            case METADATA -> Strings.get("src.date.metadata");
            case FILENAME -> Strings.get("src.date.filename");
            case CREATED -> Strings.get("src.date.created");
        };
    }

    private String modeText(AppConfig.OperationMode mode) {
        return switch (mode.normalized()) {
            case COPY -> Strings.get("src.mode.copy");
            case MOVE -> Strings.get("src.mode.move");
            case MOVE_ARCHIVE -> Strings.get("src.mode.moveArchive");
            case COPY_ARCHIVE -> Strings.get("src.mode.moveArchive");
        };
    }

    private static String trimTrailingColon(String value) {
        if (value == null) return "";
        return value.endsWith(":") ? value.substring(0, value.length() - 1) : value;
    }

    private static String text(JTextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private static String textOrValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String textOr(JTextField field, String fallback) {
        String value = text(field);
        return value.isEmpty() ? fallback : value;
    }

    private static File startDir(String value) {
        if (value == null || value.isBlank()) return null;
        File file = new File(value.trim());
        return file.isDirectory() ? file : file.getParentFile();
    }

    private final class SettingsDialog extends JDialog {
        private final JComboBox<String> cmbSettingsLang = new JComboBox<>(new String[]{"en", "ru"});
        private final SourcePanel settingsSourcePanel = new SourcePanel(false);
        private final DestPanel settingsDestPanel = new DestPanel();
        private final JCheckBox chkSettingsShowResults = new JCheckBox(Strings.get("run.showResults"));
        private final AppConfig draft;
        private boolean accepted;

        private SettingsDialog(AppConfig draft) {
            super(MainFrame.this, Strings.get("settings.title"), true);
            this.draft = draft;
            buildDialog();
            loadDraft();
        }

        private void buildDialog() {
            cmbSettingsLang.setFont(UiTheme.uiFont(Font.PLAIN, 14f));

            chkSettingsShowResults.setOpaque(false);
            chkSettingsShowResults.setForeground(UiTheme.TEXT);
            chkSettingsShowResults.setFont(UiTheme.uiFont(Font.PLAIN, 13f));

            JPanel body = new JPanel(new GridBagLayout());
            body.setOpaque(false);
            body.setBorder(new EmptyBorder(14, 16, 14, 16));

            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;

            c.gridy = 0;
            c.insets = new Insets(0, 0, 10, 0);
            body.add(settingsSourcePanel, c);

            c.gridy++;
            body.add(settingsDestPanel, c);

            c.gridy++;
            c.insets = new Insets(2, 0, 0, 0);
            body.add(settingsOptions(), c);

            c.gridy++;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            body.add(Box.createGlue(), c);

            JScrollPane scrollPane = new JScrollPane(
                    body,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            );
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getViewport().setBackground(UiTheme.APP_BG);
            scrollPane.getVerticalScrollBar().setUnitIncrement(18);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(UiTheme.APP_BG);
            root.add(scrollPane, BorderLayout.CENTER);
            root.add(settingsFooter(), BorderLayout.SOUTH);
            setContentPane(root);
            setMinimumSize(new Dimension(720, 430));
            setPreferredSize(new Dimension(820, 500));
            pack();
            setLocationRelativeTo(MainFrame.this);
        }

        private JPanel settingsOptions() {
            UiTheme.CardPanel panel = new UiTheme.CardPanel();
            panel.setLayout(new BorderLayout(18, 0));
            panel.add(chkSettingsShowResults, BorderLayout.CENTER);

            JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            langPanel.setOpaque(false);
            langPanel.add(UiTheme.label(Strings.get("lang.caption")));
            langPanel.add(cmbSettingsLang);
            panel.add(langPanel, BorderLayout.EAST);
            return panel;
        }

        private JPanel settingsFooter() {
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            footer.setBackground(Color.WHITE);
            footer.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                    new EmptyBorder(10, 16, 10, 16)
            ));

            JButton cancel = UiTheme.secondaryButton(Strings.get("settings.cancel"));
            cancel.setPreferredSize(new Dimension(110, 34));
            JButton save = UiTheme.primaryButton(Strings.get("settings.save"));
            save.setPreferredSize(new Dimension(120, 36));

            cancel.addActionListener(e -> dispose());
            save.addActionListener(e -> {
                accepted = true;
                dispose();
            });

            footer.add(cancel);
            footer.add(save);
            return footer;
        }

        private void loadDraft() {
            settingsSourcePanel.bind(draft);
            settingsDestPanel.txtDestDir.setText(draft.destDir != null ? draft.destDir : "");
            settingsDestPanel.txtFolderTemplate.setText(draft.destTemplate != null ? draft.destTemplate : "YYYYMMDD");
            chkSettingsShowResults.setSelected(draft.showResults);
            cmbSettingsLang.setSelectedItem(draft.lang);
        }

        private boolean isAccepted() {
            return accepted;
        }

        private AppConfig selectedConfig() {
            settingsSourcePanel.saveTo(draft);
            draft.destDir = text(settingsDestPanel.txtDestDir);
            draft.destTemplate = textOr(settingsDestPanel.txtFolderTemplate, "YYYYMMDD");
            draft.showResults = chkSettingsShowResults.isSelected();
            draft.lang = Objects.toString(cmbSettingsLang.getSelectedItem(), "en");
            draft.windowX = MainFrame.this.getX();
            draft.windowY = MainFrame.this.getY();
            draft.normalizeLegacyFields();
            return AppConfig.copyOf(draft);
        }
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
