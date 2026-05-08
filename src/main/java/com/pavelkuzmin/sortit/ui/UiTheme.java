package com.pavelkuzmin.sortit.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class UiTheme {
    public static final Color APP_BG = new Color(244, 247, 251);
    public static final Color CARD_BG = new Color(255, 255, 255);
    public static final Color BORDER = new Color(214, 221, 232);
    public static final Color TEXT = new Color(31, 41, 55);
    public static final Color MUTED = new Color(91, 102, 118);
    public static final Color BLUE = new Color(24, 119, 242);
    public static final Color BLUE_DARK = new Color(9, 83, 202);

    private UiTheme() {
    }

    public static Font uiFont(int style, float size) {
        return new Font("Segoe UI", style, Math.round(size));
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(uiFont(Font.PLAIN, 14f));
        return label;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(uiFont(Font.BOLD, 22f));
        return label;
    }

    public static void styleTextField(JTextField field) {
        field.setFont(uiFont(Font.PLAIN, 15f));
        field.setForeground(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(uiFont(Font.BOLD, 14f));
        button.setForeground(TEXT);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return button;
    }

    public static void styleRadio(AbstractButton button) {
        button.setFont(uiFont(Font.PLAIN, 15f));
        button.setForeground(TEXT);
        button.setOpaque(false);
        button.setFocusPainted(false);
    }

    public static class CardPanel extends JPanel {
        public CardPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(20, 24, 20, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_BG);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
