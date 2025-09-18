package com.pavelkuzmin.sortit.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;

public class LogViewerDialog extends JDialog {

    private LogViewerDialog(Frame owner, File logFile) throws Exception {
        super(owner, "Результат обработки (лог)", true);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(owner);

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        String text = Files.readString(logFile.toPath());
        area.setText(text);
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);

        JButton btnOpenInNotepad = new JButton("Открыть в Блокноте");
        JButton btnClose = new JButton("Закрыть");

        btnOpenInNotepad.addActionListener(e -> {
            try {
                Desktop.getDesktop().edit(logFile);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Не удалось открыть лог в Блокноте.", "SortIt", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnClose.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        footer.add(btnOpenInNotepad);
        footer.add(btnClose);

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(footer, BorderLayout.SOUTH);
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public static void showLog(Frame owner, File logFile) throws Exception {
        LogViewerDialog d = new LogViewerDialog(owner, logFile);
        d.setVisible(true);
    }
}
