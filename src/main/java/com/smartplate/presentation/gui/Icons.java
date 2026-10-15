package com.smartplate.presentation.gui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/** Small line icons painted with Java2D, so no image files or extra libraries are needed. */
public final class Icons {

    private Icons() {
    }

    /** Two concentric rings: the SmartPlate logo. */
    public static Icon logo(int size, Color color) {
        return new Painted(size, (g, s) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(s / 14f));
            float pad = s * 0.12f;
            g.draw(new java.awt.geom.Ellipse2D.Float(pad, pad, s - 2 * pad, s - 2 * pad));
            float inner = s * 0.31f;
            g.draw(new java.awt.geom.Ellipse2D.Float(inner, inner, s - 2 * inner, s - 2 * inner));
        });
    }

    public static Icon check(int size, Color color) {
        return new Painted(size, (g, s) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(s / 8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.2, s * 0.52);
            p.lineTo(s * 0.42, s * 0.72);
            p.lineTo(s * 0.8, s * 0.3);
            g.draw(p);
        });
    }

    /** A circle with an exclamation mark. */
    public static Icon alert(int size, Color color) {
        return new Painted(size, (g, s) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(s / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float pad = s * 0.1f;
            g.draw(new java.awt.geom.Ellipse2D.Float(pad, pad, s - 2 * pad, s - 2 * pad));
            g.draw(new java.awt.geom.Line2D.Float(s / 2f, s * 0.3f, s / 2f, s * 0.55f));
            g.draw(new java.awt.geom.Line2D.Float(s / 2f, s * 0.7f, s / 2f, s * 0.71f));
        });
    }

    public static Icon info(int size, Color color) {
        return new Painted(size, (g, s) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(s / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float pad = s * 0.1f;
            g.draw(new java.awt.geom.Ellipse2D.Float(pad, pad, s - 2 * pad, s - 2 * pad));
            g.draw(new java.awt.geom.Line2D.Float(s / 2f, s * 0.45f, s / 2f, s * 0.7f));
            g.draw(new java.awt.geom.Line2D.Float(s / 2f, s * 0.3f, s / 2f, s * 0.31f));
        });
    }

    public static Icon arrowRight(int size, Color color) {
        return new Painted(size, (g, s) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(s / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.2, s * 0.5);
            p.lineTo(s * 0.8, s * 0.5);
            p.moveTo(s * 0.55, s * 0.25);
            p.lineTo(s * 0.8, s * 0.5);
            p.lineTo(s * 0.55, s * 0.75);
            g.draw(p);
        });
    }

    /** A calendar, used by the empty plan state. */
    public static Icon calendar(int size, Color color) {
        return new Painted(size, (g, s) -> {
            g.setColor(color);
            g.setStroke(new BasicStroke(s / 20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(s * 0.12f, s * 0.2f, s * 0.76f, s * 0.68f, s * 0.12f, s * 0.12f));
            g.draw(new java.awt.geom.Line2D.Float(s * 0.12f, s * 0.4f, s * 0.88f, s * 0.4f));
            g.draw(new java.awt.geom.Line2D.Float(s * 0.33f, s * 0.1f, s * 0.33f, s * 0.27f));
            g.draw(new java.awt.geom.Line2D.Float(s * 0.67f, s * 0.1f, s * 0.67f, s * 0.27f));
        });
    }

    private interface Painter {
        void paint(Graphics2D g, int size);
    }

    private static final class Painted implements Icon {
        private final int size;
        private final Painter painter;

        Painted(int size, Painter painter) {
            this.size = size;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            painter.paint(g2, size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
