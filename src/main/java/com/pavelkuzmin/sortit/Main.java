package com.pavelkuzmin.sortit;

import com.pavelkuzmin.sortit.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Системный стиль окон (чтоб выглядело как нативное Windows-приложение)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        // Запуск UI в EDT
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
