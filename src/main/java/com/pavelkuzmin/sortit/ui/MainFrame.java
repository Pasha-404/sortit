package com.pavelkuzmin.sortit.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    // Поля UI (понадобятся дальше для логики)
    private final JTextField txtSource = new JTextField();
    private final JTextField txtDest = new JTextField();
    private final JButton btnBrowseSource = new JButton("Выбрать...");
    private final JButton btnBrowseDest = new JButton("Выбрать...");

    private final JComboBox<String> cmbFileMask = new JComboBox<>(new String[]{"*.jpg", "*.png", "*.mp4", "*.mov", "*.pdf"});
    private final JCheckBox chkNoRecursion = new JCheckBox("Без подпапок (нет рекурсии)", true);

    private final JTextField txtTemplate = new JTextField("{YYYY}/{MM}/{DD}");
    private final JComboBox<String> cmbDateSource = new JComboBox<>(new String[]{
            "Только из имени файла (по умолчанию)", "Advanced…"
    });
    private final JComboBox<String> cmbIfNoDate = new JComboBox<>(new String[]{"Пропустить файл", "Положить в Unsorted/"}); // default index 0

    private final JRadioButton rbMove = new JRadioButton("Перенос", true);
    private final JRadioButton rbCopy = new JRadioButton("Копирование");
    private final JCheckBox chkDryRun = new JCheckBox("Пробный запуск (без перемещения)", true);
    private final JCheckBox chkWriteLog = new JCheckBox("Писать лог (sort-YYYYMMDD-HHMM.csv рядом с .exe)");

    private final JButton btnPreview = new JButton("Предпросмотр");
    private final JButton btnStart = new JButton("Старт");
    private final JButton btnPause = new JButton("Пауза");
    private final JButton btnStop = new JButton("Стоп");

    private final JLabel lblSummary = new JLabel("Найдено: 0 • К обработке: 0 • Пропущено: 0");
    private final JProgressBar progress = new JProgressBar(0, 100);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Имя файла", "Откуда", "Куда (предпросмотр)", "Действие", "Статус", "Сообщение"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    private final JButton btnOpenDest = new JButton("Открыть назначение");
    private final JButton btnOpenLog = new JButton("Открыть лог");
    private final JLabel lblVersion = new JLabel("v0.1.0");

    // Снимок значений UI (на будущее для логики)
    private Map<String, Object> getUiSnapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("source", txtSource.getText().trim());
        m.put("dest", txtDest.getText().trim());
        m.put("fileMask", (String) cmbFileMask.getSelectedItem());
        m.put("noRecursion", chkNoRecursion.isSelected());
        m.put("template", txtTemplate.getText().trim());
        m.put("dateSource", (String) cmbDateSource.getSelectedItem());
        m.put("ifNoDate", (String) cmbIfNoDate.getSelectedItem());
        m.put("mode", rbMove.isSelected() ? "move" : "copy");
        m.put("dryRun", chkDryRun.isSelected());
        m.put("writeLog", chkWriteLog.isSelected());
        return m;
    }

    // Простая валидация обязательных полей
    private boolean validateInputs() {
        String src = txtSource.getText().trim();
        String dst = txtDest.getText().trim();

        if (src.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Укажите исходную папку.", "SortIt", JOptionPane.WARNING_MESSAGE);
            txtSource.requestFocus();
            return false;
        }
        if (dst.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Укажите папку назначения.", "SortIt", JOptionPane.WARNING_MESSAGE);
            txtDest.requestFocus();
            return false;
        }
        try {
            if (!Files.isDirectory(Path.of(src))) {
                JOptionPane.showMessageDialog(this, "Исходная папка не существует.", "SortIt", JOptionPane.WARNING_MESSAGE);
                txtSource.requestFocus();
                return false;
            }
        } catch (Exception ignored) { }

        // Назначение может не существовать — создадим позже; но путь должен быть валидным
        try { Path.of(dst); } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Некорректный путь назначения.", "SortIt", JOptionPane.WARNING_MESSAGE);
            txtDest.requestFocus();
            return false;
        }
        return true;
    }

    // Переключение кнопок при запуске/остановке
    private void setRunningState(boolean running) {
        btnPreview.setEnabled(!running);
        btnStart.setEnabled(!running);
        btnPause.setEnabled(running);
        btnStop.setEnabled(running);
    }

    // Открыть папку в Проводнике
    private void openFolder(String path) {
        try {
            if (path == null || path.isBlank()) return;
            Desktop.getDesktop().open(new File(path));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Не удалось открыть: " + path, "SortIt", JOptionPane.ERROR_MESSAGE);
        }
    }

    public MainFrame() {
        super("SortIt");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 700));
        setLocationByPlatform(true);

        // Верхняя часть (формы)
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(makeSourceDestPanel());
        top.add(Box.createVerticalStrut(8));
        top.add(makeTemplatePanel());
        top.add(Box.createVerticalStrut(8));
        top.add(makeModePanel());
        top.add(Box.createVerticalStrut(8));
        top.add(makeActionsPanel());

        // Центр — таблица
        JScrollPane center = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);

        // Низ — прогресс и футер
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        progress.setStringPainted(true);
        bottom.add(progress, BorderLayout.NORTH);

        JPanel footer = new JPanel(new BorderLayout());
        JPanel footerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        footerLeft.add(btnOpenDest);
        footerLeft.add(btnOpenLog);
        footer.add(footerLeft, BorderLayout.WEST);
        lblVersion.setForeground(new Color(110, 110, 110));
        footer.add(lblVersion, BorderLayout.EAST);
        bottom.add(footer, BorderLayout.SOUTH);

        // Сборка
        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Начальные состояния
        cmbDateSource.setSelectedIndex(0); // только имя файла
        cmbIfNoDate.setSelectedIndex(0);   // пропустить
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        btnOpenDest.setEnabled(false);
        btnOpenLog.setEnabled(false);

        // Простейшие обработчики "Выбрать..."
        btnBrowseSource.addActionListener(e -> chooseDirInto(txtSource));
        btnBrowseDest.addActionListener(e -> chooseDirInto(txtDest));

// Предпросмотр — сначала валидация, потом демо-строки
        btnPreview.addActionListener(e -> {
            if (!validateInputs()) return;

            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{
                    "IMG_20250131_142530.jpg", txtSource.getText().trim(),
                    txtDest.getText().trim() + "\\2025\\01\\31\\",
                    rbMove.isSelected() ? "move" : "copy", "planned", "—"
            });
            tableModel.addRow(new Object[]{
                    "photo.png", txtSource.getText().trim(),
                    "—", "skipped", "no-date", "дата в имени не найдена"
            });
            lblSummary.setText("Найдено: 2 • К обработке: 1 • Пропущено: 1");
            progress.setValue(0);
        });

// Старт — валидация + блокируем/разблокируем
        btnStart.addActionListener(e -> {
            if (!validateInputs()) return;
            setRunningState(true);
            progress.setValue(0);

            Timer t = new Timer(30, ev -> {
                int v = progress.getValue();
                if (v >= 100) {
                    ((Timer) ev.getSource()).stop();
                    setRunningState(false);
                    JOptionPane.showMessageDialog(this, "Готово! 2 файла обработано.", "SortIt", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    progress.setValue(v + 1);
                }
            });
            t.start();

            // Кнопка Стоп останавливает таймер
            btnStop.addActionListener(ev -> {
                t.stop();
                setRunningState(false);
            });
        });

// Открыть назначение/лог
        btnOpenDest.addActionListener(e -> openFolder(txtDest.getText().trim()));
        btnOpenLog.addActionListener(e -> openFolder(new File(".").getAbsolutePath())); // временно: открываем текущую папку

// В конце конструктора, после addActionListener:
        btnOpenDest.setEnabled(true); // теперь активна всегда (мы проверяем путь внутри)
        btnOpenLog.setEnabled(true);  // временно — откроет текущую папку

    }

    private JPanel makeSourceDestPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Источник и назначение", TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Источник
        c.gridx = 0; c.gridy = row; c.weightx = 0; p.add(new JLabel("Исходная папка:"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; p.add(txtSource, c);
        c.gridx = 2; c.gridy = row; c.weightx = 0; p.add(btnBrowseSource, c);
        row++;

        // Назначение
        c.gridx = 0; c.gridy = row; c.weightx = 0; p.add(new JLabel("Папка назначения:"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; p.add(txtDest, c);
        c.gridx = 2; c.gridy = row; c.weightx = 0; p.add(btnBrowseDest, c);
        row++;

        // Тип файла (на сессию) + без рекурсии
        c.gridx = 0; c.gridy = row; c.weightx = 0; p.add(new JLabel("Тип файлов (сессия):"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; p.add(cmbFileMask, c);
        c.gridx = 2; c.gridy = row; c.weightx = 0; p.add(chkNoRecursion, c);

        return p;
    }

    private JPanel makeTemplatePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Шаблон и дата", TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        c.gridx = 0; c.gridy = row; c.weightx = 0; p.add(new JLabel("Шаблон папок:"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; p.add(txtTemplate, c);
        row++;

        c.gridx = 0; c.gridy = row; c.weightx = 0; p.add(new JLabel("Источник даты:"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; p.add(cmbDateSource, c);
        row++;

        c.gridx = 0; c.gridy = row; c.weightx = 0; p.add(new JLabel("Если дата не найдена:"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1; p.add(cmbIfNoDate, c);
        row++;

        JLabel help = new JLabel("Доступно: {YYYY}, {MM}, {DD}, {name}, {ext}");
        help.setForeground(new Color(90,90,90));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        p.add(help, c);

        return p;
    }

    private JPanel makeModePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Режим и безопасность", TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); c.fill = GridBagConstraints.HORIZONTAL;

        ButtonGroup grp = new ButtonGroup();
        grp.add(rbMove); grp.add(rbCopy);

        int row = 0;

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        modePanel.add(new JLabel("Режим:"));
        modePanel.add(rbMove);
        modePanel.add(rbCopy);

        c.gridx = 0; c.gridy = row; c.weightx = 1; p.add(modePanel, c); row++;

        JPanel safetyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        safetyPanel.add(chkDryRun);
        safetyPanel.add(chkWriteLog);

        c.gridx = 0; c.gridy = row; c.weightx = 1; p.add(safetyPanel, c);

        return p;
    }

    private JPanel makeActionsPanel() {
        JPanel p = new JPanel(new BorderLayout());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.add(btnPreview);
        btns.add(btnStart);
        btns.add(btnPause);
        btns.add(btnStop);

        p.add(btns, BorderLayout.WEST);
        p.add(lblSummary, BorderLayout.EAST);

        return p;
    }

    private void chooseDirInto(JTextField targetField) {
        JFileChooser ch = new JFileChooser();
        ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ch.setDialogTitle("Выбор папки");
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = ch.getSelectedFile();
            if (dir != null) targetField.setText(dir.getAbsolutePath());
        }
    }
}
