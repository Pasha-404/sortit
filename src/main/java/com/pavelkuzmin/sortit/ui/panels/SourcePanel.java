package com.pavelkuzmin.sortit.ui.panels;

import com.pavelkuzmin.sortit.config.AppConfig;
import com.pavelkuzmin.sortit.i18n.Strings;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SourcePanel extends JPanel {
    public final JTextField txtSourceDir  = new JTextField();
    public final JButton    btnBrowseSource = new JButton("…");

    public final JTextField txtPattern = new JTextField();

    public final JRadioButton rbDateMetadata = new JRadioButton(Strings.get("src.date.metadata"));
    public final JRadioButton rbDateFilename = new JRadioButton(Strings.get("src.date.filename"));
    public final JRadioButton rbDateCreated  = new JRadioButton(Strings.get("src.date.created"));

    public final JRadioButton rbCopy         = new JRadioButton(Strings.get("src.mode.copy"));
    public final JRadioButton rbMove         = new JRadioButton(Strings.get("src.mode.move"));
    public final JRadioButton rbCopyArchive  = new JRadioButton(Strings.get("src.mode.copyArchive"));

    public SourcePanel() {
        setBorder(BorderFactory.createTitledBorder(Strings.get("src.title")));
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: source dir
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        add(new JLabel(Strings.get("src.folder")), c);

        c.gridx = 1; c.gridy = 0; c.weightx = 1.0;
        add(txtSourceDir, c);

        c.gridx = 2; c.gridy = 0; c.weightx = 0;
        btnBrowseSource.setFocusPainted(false);
        add(btnBrowseSource, c);

        // Row 2: pattern
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        add(new JLabel(Strings.get("src.pattern")), c);

        c.gridx = 1; c.gridy = 1; c.gridwidth = 2; c.weightx = 1.0;
        txtPattern.setToolTipText(Strings.get("hint.pattern"));
        add(txtPattern, c);
        c.gridwidth = 1;

        // Row 3: date source + mode in two columns
        JPanel row3 = new JPanel(new GridLayout(1,2,12,0));

        // left (date source)
        JPanel left = new JPanel(new GridBagLayout());
        GridBagConstraints l = new GridBagConstraints();
        l.insets = new Insets(4,4,4,4);
        l.anchor = GridBagConstraints.WEST;
        l.gridx = 0; l.gridy = 0;
        left.add(new JLabel(Strings.get("src.date.title")), l);
        ButtonGroup gDate = new ButtonGroup();
        l.gridy = 1; left.add(rbDateMetadata, l);
        l.gridy = 2; left.add(rbDateFilename, l);
        l.gridy = 3; left.add(rbDateCreated,  l);
        gDate.add(rbDateMetadata); gDate.add(rbDateFilename); gDate.add(rbDateCreated);

        // right (mode)
        JPanel right = new JPanel(new GridBagLayout());
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(4,4,4,4);
        r.anchor = GridBagConstraints.WEST;
        r.gridx = 0; r.gridy = 0;
        right.add(new JLabel(Strings.get("src.mode.title")), r);
        ButtonGroup gMode = new ButtonGroup();
        r.gridy = 1; right.add(rbCopy, r);
        r.gridy = 2; right.add(rbMove, r);
        r.gridy = 3; right.add(rbCopyArchive, r);
        gMode.add(rbCopy); gMode.add(rbMove); gMode.add(rbCopyArchive);

        row3.add(left);
        row3.add(right);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 3; c.weightx = 1.0;
        add(row3, c);
        c.gridwidth = 1;

        btnBrowseSource.addActionListener(e -> browseSource());
    }

    private void browseSource() {
        File start = null;
        String txt = txtSourceDir.getText().trim();
        if (!txt.isEmpty()) {
            File f = new File(txt);
            start = f.isDirectory() ? f : f.getParentFile();
        }
        JFileChooser ch = new JFileChooser(start != null ? start : new File(System.getProperty("user.home")));
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = ch.getSelectedFile();
            txtSourceDir.setText(dir.getAbsolutePath());
        }
    }

    // --- связь с конфигом ---
    public void bind(AppConfig cfg) {
        txtSourceDir.setText(cfg.sourceDir != null ? cfg.sourceDir : "");
        txtPattern.setText(cfg.filenameTemplate != null ? cfg.filenameTemplate : "*.*");

        switch (cfg.dateSource) {
            case METADATA -> rbDateMetadata.setSelected(true);
            case FILENAME -> rbDateFilename.setSelected(true);
            case CREATED  -> rbDateCreated.setSelected(true);
        }
        switch (cfg.mode) {
            case COPY         -> rbCopy.setSelected(true);
            case MOVE         -> rbMove.setSelected(true);
            case COPY_ARCHIVE -> rbCopyArchive.setSelected(true);
        }
    }

    public void saveTo(AppConfig cfg) {
        cfg.sourceDir        = txtSourceDir.getText().trim();
        cfg.filenameTemplate = txtPattern.getText().trim().isEmpty() ? "*.*" : txtPattern.getText().trim();
        if (rbDateMetadata.isSelected()) cfg.dateSource = AppConfig.DateSource.METADATA;
        else if (rbDateFilename.isSelected()) cfg.dateSource = AppConfig.DateSource.FILENAME;
        else cfg.dateSource = AppConfig.DateSource.CREATED;

        if (rbCopy.isSelected()) cfg.mode = AppConfig.OperationMode.COPY;
        else if (rbMove.isSelected()) cfg.mode = AppConfig.OperationMode.MOVE;
        else cfg.mode = AppConfig.OperationMode.COPY_ARCHIVE;
    }
}
