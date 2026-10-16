package com.smartplate.presentation.gui;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** A panel with a rounded background and an optional 1px outline (cards, alerts, stat boxes). */
public class RoundedPanel extends JPanel {

    private Color fill;
    private Color outline;
    private final int arc;

    public RoundedPanel(LayoutManager layout, Color fill, Color outline, int arc, int padding) {
        super(layout);
        this.fill = fill;
        this.outline = outline;
        this.arc = arc;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    }

    public void setColors(Color fill, Color outline) {
        this.fill = fill;
        this.outline = outline;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (outline != null) {
            g2.setColor(outline);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
