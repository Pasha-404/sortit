package com.pavelkuzmin.sortit.ui.panels;

import com.pavelkuzmin.sortit.config.AppConfig;
import com.pavelkuzmin.sortit.config.DateSource;
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

    private final JTextField txtFilenameTemplate = new JTextField("*.*");

    // Брать дату из:
    private final JRadioButton rbFromName    = new JRadioButton(Strings.get("source.dateSource.filename"));
    private final JRadioButton rbFromMeta    = new JRadioButton(Strings.get("source.dateSource.metadata"), true);
    private final JRadioButton rbFromCreated = new JRadioButton(Strings.get("source.dateSource.created"));

    // Режим копирования/переноса
    private final JRadioButton rbCopy = new JRadioButton(Strings.get("source.mode.copy"), true);
    private final JRadioButton rbMove = new JRadioButton(Strings.get("source.mode.move"));

    // Колбэки
    private Runnable onSourceChanged;
    private Runnable onTemplateChanged;

    public static class UiState {
        public String sourceDir;
        public String filenameTemplate;
        public boolean copyMode;
        public DateSource dateSource;
    }

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

        // Шаблон имени файла (glob)
        c.gridx = 0; c.gridy = row; c.weightx = 0; add(new JLabel(Strings.get("source.nameTemplate.label")), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; add(txtFilenameTemplate, c);
        row++;

        // ===== Блоки с выравниванием по колонкам =====
        // Подпись слева
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        add(new JLabel(Strings.get("source.dateSource.label")), c);

        // Справа сетка 2 строки x 3 колонки:
        // 1-я строка: [имени файла] [EXIF] [даты создания]
        // 2-я строка: [Копировать]  [Переносить] [пусто]
        JPanel grid = new JPanel(new GridLayout(2, 3, 12, 4));
        grid.setPreferredSize(new Dimension(520, 48)); // даём панели желаемую ширину

        ButtonGroup gDate = new ButtonGroup();
        gDate.add(rbFromName);
        gDate.add(rbFromMeta);
        gDate.add(rbFromCreated);
        rbFromMeta.setSelected(true); // по умолчанию EXIF/metadata

        grid.add(rbFromName);
        grid.add(rbFromMeta);
        grid.add(rbFromCreated);

        ButtonGroup grpMode = new ButtonGroup();
        grpMode.add(rbCopy);
        grpMode.add(rbMove);

        grid.add(rbCopy);
        grid.add(rbMove);
        grid.add(new JLabel("")); // пустая ячейка для выравнивания

        c.gridx = 1; c.gridy = row; c.weightx = 1;
        add(grid, c);
        row++;

        // Иконка на кнопку выбора
        setupFolderIcon(btnBrowseSource, Strings.get("source.dir.choose.tooltip"));

        // Обработчики
        btnBrowseSource.addActionListener(e -> {
            chooseDirInto(txtSource);
            fireSourceChanged();
        });

        txtFilenameTemplate.addActionListener(e -> fireTemplateChanged());
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

    public boolean isCopyMode() { return rbCopy.isSelected(); }
    public DateSource getDateSource() {
        if (rbFromMeta.isSelected())    return DateSource.METADATA;
        if (rbFromCreated.isSelected()) return DateSource.CREATED;
        return DateSource.FILENAME;
    }

    public void setEnabledAll(boolean enabled) {
        txtSource.setEnabled(enabled);
        btnBrowseSource.setEnabled(enabled);
        txtFilenameTemplate.setEnabled(enabled);
        rbFromName.setEnabled(enabled);
        rbFromMeta.setEnabled(enabled);
        rbFromCreated.setEnabled(enabled);
        rbCopy.setEnabled(enabled);
        rbMove.setEnabled(enabled);
    }

    public void applyConfig(AppConfig cfg) {
        setFilenameTemplate(cfg.filenameTemplate == null ? "*.*" : cfg.filenameTemplate);
        txtSource.setText(cfg.sourceDir == null ? "" : cfg.sourceDir);
        switch (cfg.dateSource) {
            case METADATA -> rbFromMeta.setSelected(true);
            case CREATED  -> rbFromCreated.setSelected(true);
            default       -> rbFromName.setSelected(true);
        }
        if (cfg.copyMode) rbCopy.setSelected(true); else rbMove.setSelected(true);
    }

    public void writeToConfig(AppConfig cfg) {
        cfg.sourceDir = getSourceDir();
        cfg.filenameTemplate = getFilenameTemplate().isBlank() ? "*.*" : getFilenameTemplate();
        cfg.dateSource = getDateSource();
        cfg.copyMode = isCopyMode();
    }

    // Колбэки
    public void setOnSourceChanged(Runnable r)   { this.onSourceChanged = r; }
    public void setOnTemplateChanged(Runnable r) { this.onTemplateChanged = r; }

    private void fireSourceChanged()   { if (onSourceChanged != null) onSourceChanged.run(); }
    private void fireTemplateChanged() { if (onTemplateChanged != null) onTemplateChanged.run(); }
}
