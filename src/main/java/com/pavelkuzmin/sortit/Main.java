package com.pavelkuzmin.sortit;

import com.pavelkuzmin.sortit.i18n.Strings;
import com.pavelkuzmin.sortit.ui.MainFrame;

import javax.swing.*;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        // Стиль под Windows
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }

        // Пока фиксируем RU (можно поставить Locale.getDefault())
        Strings.setLocale(Locale.getDefault());

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
