package com.pavelkuzmin.sortit.ui.panels;

import com.pavelkuzmin.sortit.config.AppConfig;
import com.pavelkuzmin.sortit.i18n.Strings;
import com.pavelkuzmin.sortit.ui.UiTheme;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SourcePanel extends UiTheme.CardPanel {
    public final JTextField txtSourceDir = new JTextField();
    public final JButton btnBrowseSource = UiTheme.secondaryButton("...");

    public final JTextField txtPattern = new JTextField();

    public final JRadioButton rbDateMetadata = new JRadioButton(Strings.get("src.date.metadata"));
    public final JRadioButton rbDateFilename = new JRadioButton(Strings.get("src.date.filename"));
    public final JRadioButton rbDateCreated = new JRadioButton(Strings.get("src.date.created"));

    public final JRadioButton rbCopy = new JRadioButton(Strings.get("src.mode.copy"));
    public final JRadioButton rbMove = new JRadioButton(Strings.get("src.mode.move"));
    public final JRadioButton rbMoveArchive = new JRadioButton(Strings.get("src.mode.moveArchive"));

    public SourcePanel() {
        setLayout(new GridBagLayout());
        UiTheme.styleTextField(txtSourceDir);
        UiTheme.styleTextField(txtPattern);
        UiTheme.styleRadio(rbDateMetadata);
        UiTheme.styleRadio(rbDateFilename);
        UiTheme.styleRadio(rbDateCreated);
        UiTheme.styleRadio(rbCopy);
        UiTheme.styleRadio(rbMove);
        UiTheme.styleRadio(rbMoveArchive);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 4, 0);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.weightx = 1;
        add(UiTheme.title(Strings.get("src.title")), c);

        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0;
        c.insets = new Insets(8, 0, 3, 14);
        add(UiTheme.label(Strings.get("src.folder")), c);

        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(8, 0, 3, 8);
        add(txtSourceDir, c);

        c.gridx = 2;
        c.weightx = 0;
        c.insets = new Insets(8, 0, 3, 0);
        add(btnBrowseSource, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.insets = new Insets(3, 0, 8, 14);
        add(UiTheme.label(Strings.get("src.pattern")), c);

        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(3, 0, 8, 0);
        txtPattern.setToolTipText(Strings.get("hint.pattern"));
        add(txtPattern, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 3;
        c.weightx = 1;
        c.insets = new Insets(2, 0, 0, 0);
        add(optionsPanel(), c);

        btnBrowseSource.addActionListener(e -> browseSource());
    }

    public void bind(AppConfig cfg) {
        txtSourceDir.setText(cfg.sourceDir != null ? cfg.sourceDir : "");
        txtPattern.setText(cfg.filenameTemplate != null ? cfg.filenameTemplate : "*.*");

        switch (cfg.dateSource) {
            case METADATA -> rbDateMetadata.setSelected(true);
            case FILENAME -> rbDateFilename.setSelected(true);
            case CREATED -> rbDateCreated.setSelected(true);
        }
        switch (cfg.mode.normalized()) {
            case COPY -> rbCopy.setSelected(true);
            case MOVE -> rbMove.setSelected(true);
            case MOVE_ARCHIVE -> rbMoveArchive.setSelected(true);
            case COPY_ARCHIVE -> rbMoveArchive.setSelected(true);
        }
    }

    public void saveTo(AppConfig cfg) {
        cfg.sourceDir = txtSourceDir.getText().trim();
        cfg.filenameTemplate = txtPattern.getText().trim().isEmpty() ? "*.*" : txtPattern.getText().trim();
        if (rbDateMetadata.isSelected()) cfg.dateSource = AppConfig.DateSource.METADATA;
        else if (rbDateFilename.isSelected()) cfg.dateSource = AppConfig.DateSource.FILENAME;
        else cfg.dateSource = AppConfig.DateSource.CREATED;

        if (rbCopy.isSelected()) cfg.mode = AppConfig.OperationMode.COPY;
        else if (rbMove.isSelected()) cfg.mode = AppConfig.OperationMode.MOVE;
        else cfg.mode = AppConfig.OperationMode.MOVE_ARCHIVE;
    }

    private JPanel optionsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 24, 0));
        panel.setOpaque(false);
        panel.add(radioColumn(Strings.get("src.date.title"), rbDateMetadata, rbDateFilename, rbDateCreated));
        panel.add(radioColumn(Strings.get("src.mode.title"), rbCopy, rbMove, rbMoveArchive));
        return panel;
    }

    private JPanel radioColumn(String title, JRadioButton first, JRadioButton second, JRadioButton third) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.insets = new Insets(2, 0, 2, 0);

        JLabel header = new JLabel(title);
        header.setForeground(UiTheme.TEXT);
        header.setFont(UiTheme.uiFont(Font.BOLD, 14f));
        c.gridy = 0;
        panel.add(header, c);

        ButtonGroup group = new ButtonGroup();
        group.add(first);
        group.add(second);
        group.add(third);

        c.gridy = 1;
        panel.add(first, c);
        c.gridy = 2;
        panel.add(second, c);
        c.gridy = 3;
        panel.add(third, c);
        return panel;
    }

    private void browseSource() {
        File start = startDir(txtSourceDir.getText());
        JFileChooser chooser = new JFileChooser(start != null ? start : new File(System.getProperty("user.home")));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtSourceDir.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static File startDir(String value) {
        if (value == null || value.isBlank()) return null;
        File file = new File(value.trim());
        return file.isDirectory() ? file : file.getParentFile();
    }
}
