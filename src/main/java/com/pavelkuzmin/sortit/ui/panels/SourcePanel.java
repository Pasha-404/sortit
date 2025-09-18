package com.pavelkuzmin.sortit.ui.panels;

import com.pavelkuzmin.sortit.config.AppConfig;
import com.pavelkuzmin.sortit.i18n.Strings;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;

public class SourcePanel extends JPanel {

    private final JTextField txtSource = new JTextField();
    private final JButton btnBrowseSource = new JButton();

    private final JTextField txtFilenameTemplate = new JTextField("");

    private final JCheckBox chkUseExif = new JCheckBox(Strings.get("source.exif.checkbox"), false);

    private final JRadioButton rbCopy = new JRadioButton(Strings.get("source.mode.copy"), true);
    private final JRadioButton rbMove = new JRadioButton(Strings.get("source.mode.move"));

    // Колбэки для MainFrame
    private Runnable onSourceChanged;
    private Runnable onTemplateChanged;

    public SourcePanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                Strings.get("source.title"),
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Папка-источник
        c.gridx = 0; c.gridy = row; c.weightx = 0; add(new JLabel(Strings.get("source.dir.label")), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; add(txtSource, c);
        c.gridx = 2; c.gridy = row; c.weightx = 0; add(btnBrowseSource, c);
        row++;

        // Шаблон имени файла
        c.gridx = 0; c.gridy = row; c.weightx = 0; add(new JLabel(Strings.get("source.nameTemplate.label")), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; add(txtFilenameTemplate, c);
        row++;

        // EXIF
        c.gridx = 1; c.gridy = row; c.weightx = 1; add(chkUseExif, c);
        row++;

        // Режим: копировать/переносить
        ButtonGroup grp = new ButtonGroup();
        grp.add(rbCopy);
        grp.add(rbMove);

        JPanel mode = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        mode.add(new JLabel(Strings.get("source.mode.label")));
        mode.add(rbCopy);
        mode.add(rbMove);

        c.gridx = 1; c.gridy = row; c.weightx = 1; add(mode, c);

        // Иконка на кнопку выбора
        setupFolderIcon(btnBrowseSource, Strings.get("source.dir.choose.tooltip"));

        // Обработчики
        btnBrowseSource.addActionListener(e -> {
            chooseDirInto(txtSource);
            fireSourceChanged();
        });

        // ENTER в шаблоне → перескан
        txtFilenameTemplate.addActionListener(e -> fireTemplateChanged());

        // Потеря фокуса у шаблона → перескан
        txtFilenameTemplate.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { fireTemplateChanged(); }
        });
    }

    private void setupFolderIcon(AbstractButton btn, String tooltip) {
        btn.setText("");
        btn.setToolTipText(tooltip);
        try {
            var url = getClass().getResource("/icons/folder-open.png");
            if (url != null) btn.setIcon(new ImageIcon(url));
            else btn.setText("...");
        } catch (Exception ex) {
            btn.setText("...");
        }
    }

    private void chooseDirInto(JTextField targetField) {
        // берём стартовую директорию из поля
        File startDir = null;
        String current = targetField.getText().trim();
        if (!current.isEmpty()) {
            try {
                File f = new File(current);
                if (f.exists()) {
                    startDir = f.isDirectory() ? f : f.getParentFile();
                }
            } catch (Exception ignored) { }
        }

        // создаём JFileChooser с начальной директорией (если есть)
        final JFileChooser ch = (startDir != null) ? new JFileChooser(startDir) : new JFileChooser();
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ch.setDialogTitle(Strings.get("source.dir.choose.tooltip"));

        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = ch.getSelectedFile();
            if (dir != null) targetField.setText(dir.getAbsolutePath());
        }
    }


    // ===== API =====
    public String getSourceDir() { return txtSource.getText().trim(); }
    public String getFilenameTemplate() { return txtFilenameTemplate.getText().trim(); }
    public void   setFilenameTemplate(String tpl) { txtFilenameTemplate.setText(tpl == null ? "" : tpl); }

    public boolean isExifEnabled() { return chkUseExif.isSelected(); }
    public boolean isCopyMode() { return rbCopy.isSelected(); }

    public void focusSource() { txtSource.requestFocus(); }

    public UiState exportState() {
        UiState s = new UiState();
        s.sourceDir = getSourceDir();
        s.filenameTemplate = getFilenameTemplate();
        s.useExif = isExifEnabled();
        s.copyMode = isCopyMode();
        return s;
    }

    public void setEnabledAll(boolean enabled) {
        txtSource.setEnabled(enabled);
        btnBrowseSource.setEnabled(enabled);
        txtFilenameTemplate.setEnabled(enabled);
        chkUseExif.setEnabled(enabled);
        rbCopy.setEnabled(enabled);
        rbMove.setEnabled(enabled);
    }

    public void applyConfig(AppConfig cfg) {
        setFilenameTemplate(cfg.filenameTemplate);
        txtSource.setText(cfg.sourceDir == null ? "" : cfg.sourceDir);
        chkUseExif.setSelected(cfg.useExif);
        if (cfg.copyMode) rbCopy.setSelected(true); else rbMove.setSelected(true);
    }

    public void writeToConfig(AppConfig cfg) {
        cfg.sourceDir = getSourceDir();
        cfg.filenameTemplate = getFilenameTemplate();
        cfg.useExif = isExifEnabled();
        cfg.copyMode = isCopyMode();
    }

    // Регистрация колбэков
    public void setOnSourceChanged(Runnable r)   { this.onSourceChanged = r; }
    public void setOnTemplateChanged(Runnable r) { this.onTemplateChanged = r; }

    private void fireSourceChanged()   { if (onSourceChanged != null) onSourceChanged.run(); }
    private void fireTemplateChanged() { if (onTemplateChanged != null) onTemplateChanged.run(); }
}
