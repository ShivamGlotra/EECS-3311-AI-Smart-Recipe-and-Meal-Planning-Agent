package com.smartplate.presentation;

import com.formdev.flatlaf.FlatClientProperties;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.facade.SmartRecipeFacade;
import com.smartplate.presentation.gui.AssistantPanel;
import com.smartplate.presentation.gui.Icons;
import com.smartplate.presentation.gui.PantryPanel;
import com.smartplate.presentation.gui.PreferencesPanel;
import com.smartplate.presentation.gui.RecipesPanel;
import com.smartplate.presentation.gui.Theme;
import com.smartplate.presentation.gui.Ui;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Swing desktop interface ("Clean Slate" design, light mode). Like the CLI it calls only
 * SmartRecipeFacade; each tab is its own panel in {@code com.smartplate.presentation.gui}:
 * AI Assistant (F04–F06, F09/F10 for the plan), Recipes (F03, F07–F09), Pantry (F02) and
 * Preferences (F01).
 */
public class MealPlannerGUI {

    private final SmartRecipeFacade facade;

    public MealPlannerGUI(SmartRecipeFacade facade) {
        this.facade = facade;
    }

    /** Builds and shows the window on the Swing event thread. */
    public void start() {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            JFrame frame = new JFrame("SmartPlate - AI Meal Planning Agent");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(buildContent());
            frame.setMinimumSize(new Dimension(1100, 720));
            frame.setSize(1280, 820);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /** The whole window content: a tab bar with the logo, the four tabs, the strategy and AI badge. */
    public JComponent buildContent() {
        Theme.install();
        AssistantPanel assistant = new AssistantPanel(facade);
        RecipesPanel recipes = new RecipesPanel(facade);
        PantryPanel pantry = new PantryPanel(facade);
        PreferencesPanel preferences = new PreferencesPanel(facade);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.SURFACE);
        tabs.addTab("AI Assistant", assistant);
        tabs.addTab("Recipes", recipes);
        tabs.addTab("Pantry", pantry);
        tabs.addTab("Preferences", preferences);
        tabs.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_AREA_ALIGNMENT,
                FlatClientProperties.TABBED_PANE_ALIGN_LEADING);
        tabs.putClientProperty(FlatClientProperties.TABBED_PANE_LEADING_COMPONENT, logo());
        tabs.putClientProperty(FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT, statusArea());
        tabs.addChangeListener(e -> {
            // other tabs may have changed the pantry or profile; re-read them from the facade
            if (tabs.getSelectedComponent() == pantry) {
                pantry.refresh();
            } else if (tabs.getSelectedComponent() == preferences) {
                preferences.load();
            } else if (tabs.getSelectedComponent() == assistant) {
                assistant.refreshPlan();
            }
        });

        JPanel root = new JPanel(new java.awt.BorderLayout());
        root.setBackground(Theme.SURFACE);
        root.add(tabs, java.awt.BorderLayout.CENTER);
        return root;
    }

    private JComponent logo() {
        JLabel logo = new JLabel("SmartPlate", Icons.logo(26, Theme.ACCENT), JLabel.LEFT);
        logo.setIconTextGap(10);
        logo.setFont(Theme.semibold(17f));
        logo.setForeground(Theme.TEXT);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 28));
        logo.setOpaque(true);
        logo.setBackground(Theme.SURFACE);
        return logo;
    }

    /** Strategy selector (Strategy pattern, switchable at run time) and the AI model badge. */
    private JComponent statusArea() {
        JLabel label = Ui.fieldLabel("Strategy");
        JComboBox<String> strategy = new JComboBox<>(new String[]{"pantry-first", "fastest"});
        strategy.setSelectedItem(facade.getStrategyName());
        strategy.setToolTipText("How candidate recipes are shortlisted before the AI chooses");
        label.setLabelFor(strategy);
        strategy.addActionListener(e -> {
            try {
                facade.setStrategy((String) strategy.getSelectedItem());
            } catch (SmartPlateException ex) {
                strategy.setSelectedItem(facade.getStrategyName());
            }
        });
        JLabel badge = Ui.outlinedPill("AI: " + shortModelName(), Theme.mono(12f));
        badge.setToolTipText(facade.getAIModelName());
        JPanel area = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        area.setOpaque(false);
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        area.add(label);
        area.add(strategy);
        area.add(badge);
        JPanel wrap = new JPanel(new java.awt.GridBagLayout());
        wrap.setBackground(Theme.SURFACE); // FlatLaf stretches this over the rest of the tab bar
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.anchor = java.awt.GridBagConstraints.EAST;
        c.weightx = 1;
        wrap.add(area, c); // right-aligned and vertically centred in the tab bar
        return wrap;
    }

    private String shortModelName() {
        String name = facade.getAIModelName();
        return name.startsWith("Mock") ? "Mock (offline)" : name;
    }
}
