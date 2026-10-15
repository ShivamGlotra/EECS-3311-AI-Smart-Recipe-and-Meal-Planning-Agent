package com.smartplate.presentation.gui;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Factory methods that give every screen the same look (cards, buttons, labels, tables). */
public final class Ui {

    private Ui() {
    }

    // ------------------------------------------------------------------ containers

    /** A white card with a rounded 1px border. Content goes in BorderLayout regions. */
    public static RoundedPanel card() {
        return new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER, 12, 1);
    }

    /** A card header: title (and optional subtitle) on the left, actions on the right, line below. */
    public static JPanel cardHeader(String title, String subtitle, JComponent... actions) {
        return cardHeader(title, subtitle == null ? null : muted(subtitle), actions);
    }

    /** Same as above, with a subtitle label the caller can update later. */
    public static JPanel cardHeader(String title, JLabel subtitle, JComponent... actions) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 16, 10, 12)));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(heading(title, 15f));
        if (subtitle != null) {
            titles.add(subtitle);
        }
        header.add(titles, BorderLayout.CENTER);
        if (actions.length > 0) {
            header.add(row(8, actions), BorderLayout.EAST);
        }
        return header;
    }

    /** A transparent left-to-right row of components with a fixed gap. */
    public static JPanel row(int gap, Component... items) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        row.setOpaque(false);
        for (Component c : items) {
            row.add(c);
        }
        return row;
    }

    /** A transparent top-to-bottom stack. */
    public static JPanel stack() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    public static Component gap(int px) {
        return Box.createVerticalStrut(px);
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    // ------------------------------------------------------------------ text

    public static JLabel heading(String text, float size) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.semibold(size));
        l.setForeground(Theme.TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.regular(12f));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    public static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.semibold(12.5f));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    public static JLabel errorLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(Theme.regular(12f));
        l.setForeground(Theme.ERROR_FG);
        return l;
    }

    // ------------------------------------------------------------------ buttons

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background: #2457D6; foreground: #FFFFFF; borderWidth: 0; focusedBorderColor: #2457D6;"
                        + " hoverBackground: #1E4BB8; pressedBackground: #1A3F9E;"
                        + " disabledBackground: #F4F6F9; disabledText: #8A93A5");
        return b;
    }

    public static JButton secondary(String text) {
        JButton b = new JButton(text);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background: #FFFFFF; foreground: #172033; borderColor: #C9D0DB; hoverBackground: #F7F8FA;"
                        + " disabledBackground: #F4F6F9; disabledText: #8A93A5; disabledBorderColor: #DDE2EA");
        return b;
    }

    public static JButton danger(String text) {
        JButton b = new JButton(text);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background: #FFFFFF; foreground: #B42318; borderColor: #F1C6C1; hoverBackground: #FEF1F0");
        return b;
    }

    /** A small rounded button used for example requests. */
    public static JButton chip(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.regular(12f));
        b.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #FFFFFF; foreground: #172033; borderColor: #C9D0DB; margin: 4,12,4,12");
        return b;
    }

    /** A borderless text button (for "Back" links). */
    public static JButton link(String text) {
        JButton b = new JButton(text);
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        b.setForeground(Theme.ACCENT);
        return b;
    }

    // ------------------------------------------------------------------ tables

    /** A read-only table model with these column names. */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setBackground(Theme.SURFACE);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        TableCellRenderer base = header.getDefaultRenderer();
        header.setDefaultRenderer((t, value, selected, focus, row, column) -> {
            Component c = base.getTableCellRendererComponent(t, value, selected, focus, row, column);
            if (c instanceof JLabel) {
                ((JLabel) c).setHorizontalAlignment(JLabel.LEFT);
            }
            return c;
        });
        return table;
    }

    /** Left-aligns the number inside a spinner (FlatLaf right-aligns it by default). */
    public static JSpinner leftAligned(JSpinner spinner) {
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setHorizontalAlignment(JLabel.LEFT);
        }
        return spinner;
    }

    // ------------------------------------------------------------------ pills / badges

    /** A small rounded label, e.g. "CHANGED" or "WORKING COPY". */
    public static JLabel pill(String text, Color background, Color foreground) {
        Pill pill = new Pill(text, background, null);
        pill.setForeground(foreground);
        pill.setFont(Theme.semibold(10.5f));
        return pill;
    }

    /** An outlined rounded label, e.g. the "AI: Mock (offline)" badge. */
    public static JLabel outlinedPill(String text, Font font) {
        Pill pill = new Pill(text, Theme.SURFACE, Theme.BORDER);
        pill.setForeground(Theme.TEXT_MUTED);
        pill.setFont(font);
        return pill;
    }

    private static final class Pill extends JLabel {
        private final Color fill;
        private final Color outline;

        Pill(String text, Color fill, Color outline) {
            super(text);
            this.fill = fill;
            this.outline = outline;
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            if (outline != null) {
                g2.setColor(outline);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
