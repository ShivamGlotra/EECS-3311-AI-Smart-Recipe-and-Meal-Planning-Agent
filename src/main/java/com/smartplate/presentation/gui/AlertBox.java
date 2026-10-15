package com.smartplate.presentation.gui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;

/** A coloured message box: success (green), error (red) or info (blue). */
public class AlertBox extends RoundedPanel {

    /** The three message styles. */
    public enum Kind { SUCCESS, ERROR, INFO }

    private final JLabel icon = new JLabel();
    private final JLabel title = new JLabel();
    private final JTextArea body = new JTextArea();
    private Kind kind;

    public AlertBox() {
        super(new BorderLayout(10, 0), Theme.SUCCESS_BG, null, 10, 12);
        icon.setVerticalAlignment(JLabel.TOP);
        icon.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
        title.setFont(Theme.semibold(13f));
        body.setFont(Theme.regular(13f));
        body.setEditable(false);
        body.setFocusable(false);
        body.setOpaque(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setBorder(BorderFactory.createEmptyBorder());
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(title);
        text.add(body);
        add(icon, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        show(Kind.INFO, null, "");
    }

    /** Shows a message. {@code heading} may be null for a one-line message. */
    public void show(Kind kind, String heading, String message) {
        this.kind = kind;
        Color fg;
        switch (kind) {
            case SUCCESS:
                setColors(Theme.SUCCESS_BG, null);
                fg = Theme.SUCCESS_FG;
                icon.setIcon(Icons.check(16, fg));
                break;
            case ERROR:
                setColors(Theme.ERROR_BG, Theme.ERROR_BORDER);
                fg = Theme.ERROR_TEXT;
                icon.setIcon(Icons.alert(16, fg));
                break;
            default:
                setColors(Theme.ACCENT_TINT, null);
                fg = Theme.ACCENT_PRESSED;
                icon.setIcon(Icons.info(16, fg));
        }
        title.setForeground(fg);
        body.setForeground(fg);
        title.setText(heading == null ? "" : heading);
        title.setVisible(heading != null);
        body.setText(message);
        body.setVisible(message != null && !message.isEmpty());
        revalidate();
        repaint();
    }

    public Kind getKind() {
        return kind;
    }

    public String getHeading() {
        return title.isVisible() ? title.getText() : null;
    }

    public String getMessage() {
        return body.getText();
    }
}
