package com.smartplate.presentation.gui;

import com.formdev.flatlaf.FlatClientProperties;
import com.smartplate.domain.InvalidProfileException;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.domain.UserProfile;
import com.smartplate.facade.SmartRecipeFacade;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Preferences tab (F01). Saving calls SmartRecipeFacade.setupProfile(); if UserProfile.validate()
 * finds problems, they are listed in a banner and next to the matching field, and the previous
 * profile stays active.
 */
public class PreferencesPanel extends JPanel {

    private final SmartRecipeFacade facade;
    private final AlertBox banner = new AlertBox();
    private final Map<String, JCheckBox> diets = new LinkedHashMap<>();
    private final JTextField allergies = new JTextField();
    private final JTextField dislikes = new JTextField();
    private final JSpinner maxTime = new JSpinner(new SpinnerNumberModel(60, 0, 600, 5));
    private final JSpinner servings = new JSpinner(new SpinnerNumberModel(2, 0, 50, 1));
    private final JLabel dietError = Ui.errorLabel();
    private final JLabel allergiesError = Ui.errorLabel();
    private final JLabel dislikesError = hintLabel("Plurals work: “mushrooms” also blocks “mushroom”.");
    private final JLabel timeError = Ui.errorLabel();
    private final JLabel servingsError = Ui.errorLabel();
    private final JButton revertButton = Ui.secondary("Revert");
    private final JButton saveButton = Ui.primary("Save preferences");

    public PreferencesPanel(SmartRecipeFacade facade) {
        super(new BorderLayout());
        this.facade = facade;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel column = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(820, d.height); // fixed width, height follows the content
            }
        };
        column.setOpaque(false);
        column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setVisible(false);
        column.add(banner);
        column.add(Ui.gap(16));
        JComponent form = buildForm();
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(form);

        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        holder.add(column, c);
        add(Ui.scroll(holder), BorderLayout.CENTER);
        ((javax.swing.JScrollPane) getComponent(0)).getViewport().setBackground(Theme.BACKGROUND);
        load();
    }

    private JComponent buildForm() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER, 12, 20);
        JPanel content = Ui.stack();

        JLabel dietTitle = Ui.heading("Dietary preferences", 14f);
        content.add(left(dietTitle));
        content.add(Ui.gap(6));
        JPanel dietRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dietRow.setOpaque(false);
        for (String diet : UserProfile.SUPPORTED_DIETS) {
            JCheckBox box = new JCheckBox(diet);
            box.setOpaque(false);
            box.setFont(Theme.regular(14f));
            box.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 20));
            diets.put(diet, box);
            dietRow.add(box);
        }
        content.add(left(dietRow));
        content.add(left(dietError));
        content.add(Ui.gap(14));

        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 14));
        grid.setOpaque(false);
        grid.add(field("Allergies (comma separated)", allergies, allergiesError));
        grid.add(field("Disliked ingredients (comma separated)", dislikes, dislikesError));
        grid.add(field("Maximum cooking time (minutes)", maxTime, timeError));
        grid.add(field("Default servings", servings, servingsError));
        content.add(left(grid));
        content.add(Ui.gap(18));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.ROW_LINE),
                BorderFactory.createEmptyBorder(14, 0, 0, 0)));
        footer.add(Ui.muted("Nothing changes until you save. Every AI suggestion is checked against these."), BorderLayout.CENTER);
        footer.add(Ui.row(8, revertButton, saveButton), BorderLayout.EAST);
        content.add(left(footer));

        Ui.leftAligned(maxTime);
        Ui.leftAligned(servings);
        allergies.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. peanuts, shellfish");
        dislikes.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. mushrooms, olives");
        revertButton.addActionListener(e -> {
            load();
            banner.show(AlertBox.Kind.INFO, null, "Reverted to your saved preferences.");
            banner.setVisible(true);
        });
        saveButton.addActionListener(e -> save());
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /** F01. */
    void save() {
        clearErrors();
        List<String> chosen = new ArrayList<>();
        for (Map.Entry<String, JCheckBox> e : diets.entrySet()) {
            if (e.getValue().isSelected()) {
                chosen.add(e.getKey());
            }
        }
        UserProfile profile = new UserProfile(chosen, split(allergies.getText()), split(dislikes.getText()),
                (Integer) maxTime.getValue(), (Integer) servings.getValue());
        try {
            facade.setupProfile(profile);
        } catch (InvalidProfileException e) {
            StringBuilder lines = new StringBuilder();
            for (String problem : e.getProblems()) {
                lines.append("• ").append(problem).append('\n');
                markField(problem);
            }
            banner.show(AlertBox.Kind.ERROR, "The profile was not saved. Your previous preferences are still active.",
                    lines.toString().trim());
            banner.setVisible(true);
            revalidate();
            return;
        } catch (SmartPlateException e) {
            banner.show(AlertBox.Kind.ERROR, "The profile was not saved.", e.getMessage());
            banner.setVisible(true);
            return;
        }
        banner.show(AlertBox.Kind.SUCCESS, null, "Preferences saved. New recommendations and plans will use them.");
        banner.setVisible(true);
        load();
        revalidate();
    }

    /** Shows the facade's current profile in the form. */
    public void load() {
        clearErrors();
        UserProfile p = facade.getProfile();
        for (Map.Entry<String, JCheckBox> e : diets.entrySet()) {
            e.getValue().setSelected(p.getDietaryPreferences().contains(e.getKey()));
        }
        allergies.setText(String.join(", ", p.getAllergies()));
        dislikes.setText(String.join(", ", p.getDislikedIngredients()));
        maxTime.setValue(p.getMaxCookingTime());
        servings.setValue(p.getDefaultServings());
    }

    /** Puts a problem from UserProfile.validate() next to the field it is about. */
    private void markField(String problem) {
        String p = problem.toLowerCase(Locale.ROOT);
        if (p.contains("allergy")) {
            showFieldError(allergiesError, allergies, problem);
        } else if (p.contains("disliked")) {
            showFieldError(dislikesError, dislikes, problem);
        } else if (p.contains("servings")) {
            showFieldError(servingsError, servings, problem);
        } else if (p.contains("cooking time")) {
            showFieldError(timeError, maxTime, problem);
        } else if (p.contains("dietary")) {
            dietError.setText(problem);
        }
    }

    private static void showFieldError(JLabel label, JComponent field, String text) {
        label.setForeground(Theme.ERROR_FG);
        label.setText("<html><div style='width:360px'>" + text + "</div></html>");
        field.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
    }

    private void clearErrors() {
        for (JLabel l : new JLabel[]{dietError, allergiesError, timeError, servingsError}) {
            l.setText(" ");
        }
        dislikesError.setForeground(Theme.TEXT_MUTED);
        dislikesError.setText("Plurals work: “mushrooms” also blocks “mushroom”.");
        for (JComponent c : new JComponent[]{allergies, dislikes, maxTime, servings}) {
            c.putClientProperty(FlatClientProperties.OUTLINE, null);
        }
    }

    private static JComponent field(String label, JComponent input, JLabel help) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = Ui.fieldLabel(label);
        l.setLabelFor(input);
        p.add(l, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        p.add(help, BorderLayout.SOUTH);
        return p;
    }

    private static JLabel hintLabel(String text) {
        JLabel l = Ui.muted(text);
        return l;
    }

    private static List<String> split(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        for (String s : text.split(",")) {
            out.add(s.trim());
        }
        return out;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    // ================================================================== accessors for tests

    AlertBox banner() {
        return banner;
    }

    JTextField allergiesField() {
        return allergies;
    }

    JSpinner servingsSpinner() {
        return servings;
    }

    JCheckBox dietBox(String diet) {
        return diets.get(diet);
    }

    boolean hasErrorOutline(JComponent c) {
        return FlatClientProperties.OUTLINE_ERROR.equals(c.getClientProperty(FlatClientProperties.OUTLINE));
    }
}
