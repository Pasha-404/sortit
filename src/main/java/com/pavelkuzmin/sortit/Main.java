package com.pavelkuzmin.sortit;

import com.pavelkuzmin.sortit.i18n.Strings;
import com.pavelkuzmin.sortit.ui.MainFrame;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Язык и UI
        // (Locale больше не прокидываем в Strings; язык берётся из конфига внутри MainFrame)
        EventQueue.invokeLater(() -> {
            // Набор системной темы, если доступна
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Запуск основного окна
            new MainFrame().setVisible(true);
        });
    }
}
