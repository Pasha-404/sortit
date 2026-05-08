package com.pavelkuzmin.sortit.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class UiTheme {
    public static final Color APP_BG = new Color(241, 245, 250);
    public static final Color CARD_BG = new Color(255, 255, 255);
    public static final Color BORDER = new Color(205, 216, 230);
    public static final Color TEXT = new Color(17, 24, 39);
    public static final Color MUTED = new Color(71, 85, 105);
    public static final Color BLUE = new Color(0, 106, 230);
    public static final Color BLUE_DARK = new Color(0, 73, 180);

    private UiTheme() {
    }

    public static Font uiFont(int style, float size) {
        return new Font("Segoe UI", style, Math.round(size));
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(uiFont(Font.PLAIN, 13f));
        return label;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(uiFont(Font.BOLD, 18f));
        return label;
    }

    public static void styleTextField(JTextField field) {
        field.setFont(uiFont(Font.PLAIN, 14f));
        field.setForeground(TEXT);
        field.setPreferredSize(new Dimension(80, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(4, 9, 4, 9)
        ));
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(uiFont(Font.BOLD, 14f));
        button.setForeground(TEXT);
        button.setPreferredSize(new Dimension(42, 30));
        button.setFocusPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        return button;
    }

    public static JButton primaryButton(String text) {
        return new PrimaryButton(text);
    }

    public static void styleRadio(AbstractButton button) {
        button.setFont(uiFont(Font.PLAIN, 14f));
        button.setForeground(TEXT);
        button.setOpaque(false);
        button.setFocusPainted(false);
    }

    public static class CardPanel extends JPanel {
        public CardPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(12, 16, 12, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_BG);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class PrimaryButton extends JButton {
        private PrimaryButton(String text) {
            super(text);
            setFont(uiFont(Font.BOLD, 18f));
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(250, 42));
            setFocusPainted(false);
            setBorder(new EmptyBorder(8, 18, 8, 18));
            setContentAreaFilled(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color top = isEnabled() ? BLUE : new Color(150, 163, 180);
            Color bottom = isEnabled() ? BLUE_DARK : new Color(118, 132, 150);
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            if (getModel().isPressed() && isEnabled()) {
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }

            g2.setColor(new Color(255, 255, 255, isEnabled() ? 80 : 35));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
