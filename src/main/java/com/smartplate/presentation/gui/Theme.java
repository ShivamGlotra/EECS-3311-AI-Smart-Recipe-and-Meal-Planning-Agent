package com.smartplate.presentation.gui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.io.InputStream;
import java.util.Map;

/**
 * The "Clean Slate" design (Direction B): light mode only. Holds the colour palette and the
 * bundled IBM Plex fonts, and configures FlatLaf so every standard Swing widget matches.
 */
public final class Theme {

    public static final Color BACKGROUND = new Color(0xF4F6F9);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xDDE2EA);
    public static final Color INPUT_BORDER = new Color(0xC9D0DB);
    public static final Color TEXT = new Color(0x172033);
    public static final Color TEXT_MUTED = new Color(0x4A5468);
    public static final Color TEXT_DISABLED = new Color(0x8A93A5);
    public static final Color TRACE_TEXT = new Color(0x39445A);
    public static final Color ACCENT = new Color(0x2457D6);
    public static final Color ACCENT_HOVER = new Color(0x1E4BB8);
    public static final Color ACCENT_PRESSED = new Color(0x1A3F9E);
    public static final Color ACCENT_TINT = new Color(0xEEF3FD);
    public static final Color TABLE_HEADER = new Color(0xF7F8FA);
    public static final Color ROW_LINE = new Color(0xEEF1F5);
    public static final Color SUCCESS_BG = new Color(0xEAF5EF);
    public static final Color SUCCESS_FG = new Color(0x14532D);
    public static final Color ERROR_BG = new Color(0xFEF1F0);
    public static final Color ERROR_BORDER = new Color(0xF1C6C1);
    public static final Color ERROR_FG = new Color(0xB42318);
    public static final Color ERROR_TEXT = new Color(0x8C1D13);
    public static final Color WARN_BG = new Color(0xFDF3E1);
    public static final Color WARN_FG = new Color(0x7A4A0E);

    private static Font regular;
    private static Font medium;
    private static Font semibold;
    private static Font mono;
    private static boolean installed;

    private Theme() {
    }

    /** Installs FlatLaf with SmartPlate's colours and fonts. Safe to call more than once. */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        loadFonts();
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#2457D6"));
        FlatLightLaf.setup();

        UIManager.put("defaultFont", regular(13f));
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.disabledForeground", TEXT_DISABLED);

        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("CheckBox.arc", 4);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.borderColor", INPUT_BORDER);
        UIManager.put("Component.disabledBorderColor", BORDER);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("Component.focusColor", new Color(0x552457D6, true));
        UIManager.put("Component.error.borderColor", ERROR_FG);
        UIManager.put("Component.error.focusedBorderColor", ERROR_FG);

        UIManager.put("Button.margin", new Insets(7, 14, 7, 14));
        UIManager.put("Button.font", semibold(13f));
        UIManager.put("Button.borderColor", INPUT_BORDER);
        UIManager.put("Button.disabledBackground", BACKGROUND);
        UIManager.put("Button.disabledText", TEXT_DISABLED);
        UIManager.put("TextField.margin", new Insets(7, 10, 7, 10));
        UIManager.put("TextArea.margin", new Insets(6, 8, 6, 8));
        UIManager.put("TextArea.inactiveBackground", SURFACE);
        UIManager.put("ComboBox.padding", new Insets(5, 8, 5, 8));
        UIManager.put("Spinner.padding", new Insets(5, 8, 5, 8));

        UIManager.put("TabbedPane.tabHeight", 56);
        UIManager.put("TabbedPane.tabInsets", new Insets(0, 16, 0, 16));
        UIManager.put("TabbedPane.font", medium(14f));
        UIManager.put("TabbedPane.foreground", TEXT_MUTED);
        UIManager.put("TabbedPane.selectedForeground", ACCENT);
        UIManager.put("TabbedPane.underlineColor", ACCENT);
        UIManager.put("TabbedPane.inactiveUnderlineColor", ACCENT);
        UIManager.put("TabbedPane.tabSelectionHeight", 3);
        UIManager.put("TabbedPane.hoverColor", BACKGROUND);
        UIManager.put("TabbedPane.focusColor", SURFACE);
        UIManager.put("TabbedPane.selectedBackground", SURFACE);
        UIManager.put("TabbedPane.contentSeparatorHeight", 1);
        UIManager.put("TabbedPane.contentAreaColor", BORDER);

        UIManager.put("Table.rowHeight", 38);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.gridColor", ROW_LINE);
        UIManager.put("Table.selectionBackground", ACCENT_TINT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.selectionInactiveBackground", ACCENT_TINT);
        UIManager.put("Table.selectionInactiveForeground", TEXT);
        UIManager.put("Table.focusCellHighlightBorder", javax.swing.BorderFactory.createEmptyBorder());
        UIManager.put("TableHeader.background", TABLE_HEADER);
        UIManager.put("TableHeader.foreground", TEXT_MUTED);
        UIManager.put("TableHeader.font", semibold(11.5f));
        UIManager.put("TableHeader.height", 34);
        UIManager.put("TableHeader.separatorColor", TABLE_HEADER);
        UIManager.put("TableHeader.bottomSeparatorColor", BORDER);
        UIManager.put("TableHeader.cellMargins", new Insets(0, 14, 0, 14));

        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.track", SURFACE);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("Viewport.background", SURFACE);
        installed = true;
    }

    public static Font regular(float size) {
        loadFonts();
        return regular.deriveFont(size);
    }

    public static Font medium(float size) {
        loadFonts();
        return medium.deriveFont(size);
    }

    public static Font semibold(float size) {
        loadFonts();
        return semibold.deriveFont(size);
    }

    public static Font mono(float size) {
        loadFonts();
        return mono.deriveFont(size);
    }

    /** Loads the bundled IBM Plex fonts; falls back to the system fonts if a file is missing. */
    private static synchronized void loadFonts() {
        if (regular != null) {
            return;
        }
        regular = load("IBMPlexSans-Regular.ttf", new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        medium = load("IBMPlexSans-Medium.ttf", regular);
        semibold = load("IBMPlexSans-SemiBold.ttf", regular.deriveFont(Font.BOLD));
        mono = load("IBMPlexMono-Regular.ttf", new Font(Font.MONOSPACED, Font.PLAIN, 13));
    }

    private static Font load(String file, Font fallback) {
        try (InputStream in = Theme.class.getResourceAsStream("/fonts/" + file)) {
            if (in == null) {
                return fallback;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font.deriveFont(13f);
        } catch (Exception e) {
            return fallback;
        }
    }
}
