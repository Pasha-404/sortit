package com.pavelkuzmin.sortit.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class UiTheme {
    public static final Color APP_BG = new Color(239, 245, 252);
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
        label.setFont(uiFont(Font.BOLD, 17f));
        return label;
    }

    public static void styleTextField(JTextField field) {
        field.setFont(uiFont(Font.PLAIN, 13f));
        field.setForeground(TEXT);
        field.setPreferredSize(new Dimension(80, 28));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(3, 8, 3, 8)
        ));
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(uiFont(Font.BOLD, 13f));
        button.setForeground(TEXT);
        button.setPreferredSize(new Dimension(40, 28));
        button.setFocusPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        return button;
    }

    public static JButton primaryButton(String text) {
        return new PrimaryButton(text);
    }

    public static void styleRadio(AbstractButton button) {
        button.setFont(uiFont(Font.PLAIN, 13f));
        button.setForeground(TEXT);
        button.setOpaque(false);
        button.setFocusPainted(false);
    }

    public static class CardPanel extends JPanel {
        public CardPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(10, 14, 10, 14));
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
            setFont(uiFont(Font.BOLD, 16f));
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(250, 38));
            setFocusPainted(false);
            setBorder(new EmptyBorder(7, 18, 7, 18));
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color top = isEnabled() ? BLUE : new Color(94, 120, 154);
            Color bottom = isEnabled() ? BLUE_DARK : new Color(68, 92, 128);
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            if (getModel().isPressed() && isEnabled()) {
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }

            g2.setColor(new Color(255, 255, 255, isEnabled() ? 80 : 35));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            g2.setFont(getFont());
            FontMetrics metrics = g2.getFontMetrics();
            String text = getText();
            int x = (getWidth() - metrics.stringWidth(text)) / 2;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.setColor(new Color(255, 255, 255, isEnabled() ? 255 : 220));
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }
}
