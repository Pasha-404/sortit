package com.pavelkuzmin.sortit.ui.panels;

import com.pavelkuzmin.sortit.config.AppConfig;
import com.pavelkuzmin.sortit.core.FolderTemplate;
import com.pavelkuzmin.sortit.i18n.Strings;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;

public class DestPanel extends JPanel {

    private final JTextField txtDest = new JTextField();
    private final JButton btnBrowseDest = new JButton();

    private final JTextField txtFolderTemplate = new JTextField("YYYYMMDD");
    private final JLabel infoFolderTpl = makeInfoIcon(Strings.get("hint.folderTemplate"));

    public DestPanel() {
        setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        new LineBorder(new Color(0xD0D0D0)),
                        Strings.get("dest.title"),
                        TitledBorder.LEFT, TitledBorder.TOP
                ),
                new EmptyBorder(6, 8, 8, 8)
        ));
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Папка-назначение
        c.gridx = 0; c.gridy = row; c.weightx = 0; add(new JLabel(Strings.get("dest.dir.label")), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; add(txtDest, c);
        c.gridx = 2; c.gridy = row; c.weightx = 0; add(btnBrowseDest, c);
        row++;

        // Шаблон папок + ℹ
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        JPanel tplLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tplLabel.setOpaque(false);
        tplLabel.add(new JLabel(Strings.get("dest.folderTemplate.label")));
        tplLabel.add(infoFolderTpl);
        add(tplLabel, c);

        c.gridx = 1; c.gridy = row; c.weightx = 1; add(txtFolderTemplate, c);
        row++;

        setupFolderIcon(btnBrowseDest, Strings.get("dest.dir.choose.tooltip"));
        btnBrowseDest.addActionListener(e -> chooseDirInto(txtDest));
    }

    private JLabel makeInfoIcon(String tooltip) {
        JLabel lbl = new JLabel(UIManager.getIcon("OptionPane.informationIcon"));
        lbl.setToolTipText(tooltip);
        return lbl;
    }

    private void setupFolderIcon(AbstractButton btn, String tooltip) {
        btn.setText("");
        btn.setToolTipText(tooltip);
        try {
            var url = getClass().getResource("/icons/folder-open.png");
            if (url != null) btn.setIcon(new ImageIcon(url));
            else btn.setText("...");
        } catch (Exception ex) { btn.setText("..."); }
    }

    private void chooseDirInto(JTextField targetField) {
        File startDir = null;
        String current = targetField.getText().trim();
        if (!current.isEmpty()) {
            try {
                File f = new File(current);
                if (f.exists()) startDir = f.isDirectory() ? f : f.getParentFile();
            } catch (Exception ignored) { }
        }
        final JFileChooser ch = (startDir != null) ? new JFileChooser(startDir) : new JFileChooser();
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ch.setDialogTitle(Strings.get("dest.dir.choose.tooltip"));
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = ch.getSelectedFile();
            if (dir != null) targetField.setText(dir.getAbsolutePath());
        }
    }

    // ===== API =====
    public String getDestDir() { return txtDest.getText().trim(); }
    public String getTemplate() { return txtFolderTemplate.getText().trim(); }
    public void focusDest() { txtDest.requestFocus(); }

    public void setEnabledAll(boolean enabled) {
        txtDest.setEnabled(enabled);
        btnBrowseDest.setEnabled(enabled);
        txtFolderTemplate.setEnabled(enabled);
    }

    public void applyConfig(AppConfig cfg) {
        txtDest.setText(cfg.destDir == null ? "" : cfg.destDir);
        String t = (cfg.destTemplate == null || cfg.destTemplate.isBlank()) ? "YYYYMMDD" : cfg.destTemplate;
        txtFolderTemplate.setText(FolderTemplate.isValid(t) ? t : "YYYYMMDD");
    }

    public void writeToConfig(AppConfig cfg) {
        String t = getTemplate().isBlank() ? "YYYYMMDD" : getTemplate();
        cfg.destDir = getDestDir();
        cfg.destTemplate = FolderTemplate.isValid(t) ? t : "YYYYMMDD";
        if (!FolderTemplate.isValid(getTemplate())) {
            txtFolderTemplate.setText(cfg.destTemplate);
        }
    }
}
