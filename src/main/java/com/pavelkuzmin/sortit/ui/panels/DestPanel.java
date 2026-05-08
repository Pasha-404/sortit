package com.pavelkuzmin.sortit.ui.panels;

import com.pavelkuzmin.sortit.i18n.Strings;
import com.pavelkuzmin.sortit.ui.UiTheme;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class DestPanel extends UiTheme.CardPanel {

    public final JTextField txtDestDir = new JTextField();
    public final JButton btnBrowseDest = UiTheme.secondaryButton("...");
    public final JTextField txtFolderTemplate = new JTextField();

    public DestPanel() {
        setLayout(new GridBagLayout());
        UiTheme.styleTextField(txtDestDir);
        UiTheme.styleTextField(txtFolderTemplate);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 0, 8, 0);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 3;
        c.weightx = 1;
        add(UiTheme.title(Strings.get("dst.title")), c);

        c.gridy = 1;
        c.gridwidth = 1;
        c.weightx = 0;
        c.insets = new Insets(12, 0, 4, 16);
        add(UiTheme.label(Strings.get("dst.folder")), c);

        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(12, 0, 4, 8);
        add(txtDestDir, c);

        c.gridx = 2;
        c.weightx = 0;
        c.insets = new Insets(12, 0, 4, 0);
        add(btnBrowseDest, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0;
        c.insets = new Insets(4, 0, 0, 16);
        add(UiTheme.label(Strings.get("dst.template")), c);

        c.gridx = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 0, 0);
        txtFolderTemplate.setToolTipText(Strings.get("hint.template"));
        add(txtFolderTemplate, c);

        btnBrowseDest.addActionListener(e -> browseDest());
    }

    private void browseDest() {
        File start = startDir(txtDestDir.getText());
        JFileChooser chooser = new JFileChooser(start != null ? start : new File(System.getProperty("user.home")));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtDestDir.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static File startDir(String value) {
        if (value == null || value.isBlank()) return null;
        File file = new File(value.trim());
        return file.isDirectory() ? file : file.getParentFile();
    }
}
