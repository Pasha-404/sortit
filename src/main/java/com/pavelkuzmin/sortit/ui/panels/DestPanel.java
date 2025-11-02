package com.pavelkuzmin.sortit.ui.panels;

import com.pavelkuzmin.sortit.i18n.Strings;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class DestPanel extends JPanel {

    public final JTextField txtDestDir        = new JTextField();
    public final JButton    btnBrowseDest     = new JButton("…");
    public final JTextField txtFolderTemplate = new JTextField();

    public DestPanel() {
        setBorder(BorderFactory.createTitledBorder(Strings.get("dst.title")));
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Row 1: Destination folder
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        add(new JLabel(Strings.get("dst.folder")), c);

        c.gridx = 1; c.gridy = 0; c.weightx = 1.0;
        add(txtDestDir, c);

        c.gridx = 2; c.gridy = 0; c.weightx = 0;
        btnBrowseDest.setFocusPainted(false);
        add(btnBrowseDest, c);

        // Row 2: Folder template
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        add(new JLabel(Strings.get("dst.template")), c);

        c.gridx = 1; c.gridy = 1; c.gridwidth = 2; c.weightx = 1.0;
        txtFolderTemplate.setToolTipText(Strings.get("hint.template"));
        add(txtFolderTemplate, c);
        c.gridwidth = 1;

        btnBrowseDest.addActionListener(e -> browseDest());
    }

    private void browseDest() {
        File start = null;
        String txt = txtDestDir.getText().trim();
        if (!txt.isEmpty()) {
            File f = new File(txt);
            start = f.isDirectory() ? f : f.getParentFile();
        }
        JFileChooser ch = new JFileChooser(start != null ? start : new File(System.getProperty("user.home")));
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = ch.getSelectedFile();
            txtDestDir.setText(dir.getAbsolutePath());
        }
    }
}
