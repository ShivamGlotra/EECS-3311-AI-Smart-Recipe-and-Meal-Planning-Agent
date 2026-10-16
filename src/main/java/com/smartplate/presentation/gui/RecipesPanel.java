package com.smartplate.presentation.gui;

import com.smartplate.domain.Ingredient;
import com.smartplate.domain.NutritionInfo;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.domain.Substitution;
import com.smartplate.domain.Units;
import com.smartplate.facade.SmartRecipeFacade;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Recipes tab: F03 search, F07 serving adjustment, F08 substitution and F09 recipe nutrition.
 * Scaling and substitution return a changed copy (the design says they are not undoable), so the
 * tab keeps the latest copy of each recipe as a "working copy" until the user presses Reset.
 */
public class RecipesPanel extends JPanel {

    private final SmartRecipeFacade facade;

    private final JTextField query = new JTextField();
    private final JButton searchButton = Ui.primary("Search");
    private final JButton showAllButton = Ui.secondary("Show all");
    private final DefaultTableModel resultsModel = Ui.readOnlyModel("ID", "NAME", "MIN", "SERVES");
    private final JTable resultsTable = Ui.table(resultsModel);
    private final JLabel resultsSubtitle = Ui.muted(" ");
    private final List<Recipe> shown = new ArrayList<>();

    private final CardLayout detailCards = new CardLayout();
    private final JPanel detail = new JPanel(detailCards);
    private final JLabel name = Ui.heading(" ", 18f);
    private final JLabel workingCopyTag = Ui.pill("WORKING COPY", Theme.WARN_BG, Theme.WARN_FG);
    private final JLabel meta = Ui.muted(" ");
    private final JButton resetButton = Ui.secondary("Reset");
    private final JSpinner servings = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
    private final JButton scaleButton = Ui.secondary("Scale");
    private final JComboBox<String> ingredientBox = new JComboBox<>();
    private final JButton substituteButton = Ui.secondary("Substitute");
    private final AlertBox status = new AlertBox();
    private final JLabel ingredientsTitle = Ui.heading("Ingredients", 14f);
    private final JPanel ingredientRows = Ui.stack();
    private final JLabel nutritionTotal = new JLabel();
    private final JLabel nutritionPerServing = new JLabel();
    private final JPanel steps = Ui.stack();

    private final Map<String, Recipe> workingCopies = new HashMap<>();
    private final Map<String, Map<String, String>> substitutionNotes = new HashMap<>();
    private Recipe current;

    public RecipesPanel(SmartRecipeFacade facade) {
        super(new BorderLayout(0, 16));
        this.facade = facade;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        add(buildSearchBar(), BorderLayout.NORTH);
        JPanel main = new JPanel(new BorderLayout(16, 0));
        main.setOpaque(false);
        main.add(buildResults(), BorderLayout.WEST);
        main.add(buildDetail(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        showRecipes(facade.getAllRecipes(), facade.getAllRecipes().size() + " recipes");
    }

    // ================================================================== layout

    private JComponent buildSearchBar() {
        RoundedPanel bar = new RoundedPanel(new BorderLayout(12, 0), Theme.SURFACE, Theme.BORDER, 12, 12);
        JLabel label = Ui.fieldLabel("Search (name, tag or ingredient)");
        label.setLabelFor(query);
        query.setFont(Theme.regular(14f));
        query.putClientProperty("JTextField.placeholderText", "e.g. spinach, vegan, rice");
        query.addActionListener(e -> search());
        searchButton.addActionListener(e -> search());
        showAllButton.addActionListener(e -> {
            query.setText("");
            showRecipes(facade.getAllRecipes(), facade.getAllRecipes().size() + " recipes");
        });
        bar.add(label, BorderLayout.WEST);
        bar.add(query, BorderLayout.CENTER);
        bar.add(Ui.row(8, searchButton, showAllButton), BorderLayout.EAST);
        return bar;
    }

    private JComponent buildResults() {
        RoundedPanel card = Ui.card();
        card.setPreferredSize(new Dimension(440, 10));
        card.add(Ui.cardHeader("Results", resultsSubtitle), BorderLayout.NORTH);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, r, c);
                l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                l.setFont(c == 0 ? Theme.mono(12.5f) : sel && c == 1 ? Theme.semibold(13f) : Theme.regular(13f));
                l.setForeground(c == 0 ? Theme.TEXT_MUTED : Theme.TEXT);
                l.setBackground(sel ? Theme.ACCENT_TINT : Theme.SURFACE);
                return l;
            }
        };
        int[] widths = {56, 230, 52, 74};
        for (int i = 0; i < widths.length; i++) {
            resultsTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            int row = resultsTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < shown.size()) {
                select(shown.get(row).getId());
            }
        });
        card.add(Ui.scroll(resultsTable), BorderLayout.CENTER);
        return card;
    }

    private JComponent buildDetail() {
        RoundedPanel card = Ui.card();
        detail.setOpaque(false);

        JPanel none = new JPanel(new java.awt.GridBagLayout());
        none.setOpaque(false);
        JLabel hint = Ui.muted("Select a recipe to see its ingredients, nutrition and steps.");
        hint.setFont(Theme.regular(14f));
        none.add(hint);
        detail.add(none, "none");

        JPanel view = new JPanel(new BorderLayout());
        view.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 12)));
        JPanel titles = Ui.stack();
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.add(name);
        titleRow.add(workingCopyTag);
        ((FlowLayout) titleRow.getLayout()).setHgap(8);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, -8, 0, 0));
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        titles.add(titleRow);
        titles.add(meta);
        header.add(titles, BorderLayout.CENTER);
        resetButton.setToolTipText("Forget the scaled/substituted copy and show the original recipe");
        resetButton.addActionListener(e -> reset());
        header.add(Ui.row(0, resetButton), BorderLayout.EAST);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setBackground(Theme.TABLE_HEADER);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel servingsLabel = Ui.fieldLabel("Servings");
        servingsLabel.setLabelFor(servings);
        Ui.leftAligned(servings);
        servings.setPreferredSize(new Dimension(80, servings.getPreferredSize().height));
        JLabel ingredientLabel = Ui.fieldLabel("   Ingredient");
        ingredientLabel.setLabelFor(ingredientBox);
        ingredientBox.setPreferredSize(new Dimension(180, ingredientBox.getPreferredSize().height));
        scaleButton.addActionListener(e -> scale());
        substituteButton.addActionListener(e -> substitute());
        toolbar.add(servingsLabel);
        toolbar.add(servings);
        toolbar.add(scaleButton);
        toolbar.add(ingredientLabel);
        toolbar.add(ingredientBox);
        toolbar.add(substituteButton);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(header, BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.CENTER);
        JPanel statusWrap = new JPanel(new BorderLayout());
        statusWrap.setOpaque(false);
        statusWrap.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));
        statusWrap.add(status, BorderLayout.CENTER);
        top.add(statusWrap, BorderLayout.SOUTH);
        status.setVisible(false);
        view.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.58;
        c.weighty = 1;
        JPanel left = Ui.stack();
        ingredientsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        ingredientRows.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(ingredientsTitle);
        left.add(Ui.gap(6));
        left.add(ingredientRows);
        body.add(left, c);

        c.weightx = 0.42;
        c.insets = new Insets(0, 20, 0, 0);
        JPanel right = Ui.stack();
        RoundedPanel nutrition = new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER, 10, 12);
        JPanel nInner = Ui.stack();
        nInner.add(Ui.heading("Nutrition", 14f));
        nInner.add(Ui.gap(6));
        nutritionTotal.setFont(Theme.regular(13f));
        nutritionPerServing.setFont(Theme.mono(11.5f));
        nutritionPerServing.setForeground(Theme.TRACE_TEXT);
        nInner.add(nutritionTotal);
        nInner.add(Ui.gap(4));
        nInner.add(nutritionPerServing);
        nutrition.add(nInner, BorderLayout.CENTER);
        nutrition.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(nutrition);
        right.add(Ui.gap(14));
        JLabel stepsTitle = Ui.heading("Steps", 14f);
        stepsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        steps.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(stepsTitle);
        right.add(Ui.gap(6));
        right.add(steps);
        body.add(right, c);

        view.add(Ui.scroll(body), BorderLayout.CENTER);
        detail.add(view, "recipe");
        card.add(detail, BorderLayout.CENTER);
        return card;
    }

    // ================================================================== actions

    /** F03. */
    void search() {
        try {
            List<Recipe> found = facade.searchRecipes(query.getText());
            showRecipes(found, found.isEmpty() ? "No recipes found." : found.size() + " recipe" + (found.size() == 1 ? "" : "s"));
        } catch (SmartPlateException e) {
            resultsSubtitle.setText(e.getMessage());
            resultsSubtitle.setForeground(Theme.ERROR_FG);
        }
    }

    /** Shows the working copy of a recipe if there is one, otherwise the original. */
    void select(String id) {
        Recipe copy = workingCopies.get(id);
        current = copy != null ? copy : facade.getRecipe(id);
        status.setVisible(false);
        render();
    }

    /** F07. */
    void scale() {
        if (current == null) {
            return;
        }
        int target = (Integer) servings.getValue();
        try {
            int before = current.getServings();
            Recipe scaled = facade.adjustServings(current, target);
            keep(scaled);
            showStatus(AlertBox.Kind.SUCCESS, "Scaled from " + before + " to " + target + " servings.");
        } catch (SmartPlateException e) {
            showStatus(AlertBox.Kind.ERROR, e.getMessage());
        }
    }

    /** F08. */
    void substitute() {
        if (current == null || ingredientBox.getSelectedItem() == null) {
            return;
        }
        String original = (String) ingredientBox.getSelectedItem();
        try {
            Recipe updated = facade.substituteIngredient(current, new Ingredient(original));
            Substitution s = facade.getLastSubstitution();
            substitutionNotes.computeIfAbsent(updated.getId(), k -> new HashMap<>())
                    .put(s.getSubstitute().getName(), "replaces " + noteFor(original, updated.getId()));
            keep(updated);
            showStatus(AlertBox.Kind.SUCCESS, "Substituted " + s.getOriginal().getName() + " → "
                    + s.getSubstitute().getName() + " (×" + trim(s.getConversionFactor()) + ").");
        } catch (SmartPlateException e) {
            showStatus(AlertBox.Kind.ERROR, e.getMessage());
        }
    }

    void reset() {
        if (current == null) {
            return;
        }
        workingCopies.remove(current.getId());
        substitutionNotes.remove(current.getId());
        current = facade.getRecipe(current.getId());
        showStatus(AlertBox.Kind.INFO, "Back to the original recipe.");
        render();
    }

    // ================================================================== rendering

    private void showRecipes(List<Recipe> recipes, String subtitle) {
        shown.clear();
        shown.addAll(recipes);
        resultsModel.setRowCount(0);
        for (Recipe r : recipes) {
            resultsModel.addRow(new Object[]{r.getId(), r.getName(), r.getCookingTime(), r.getServings()});
        }
        resultsSubtitle.setText(subtitle);
        resultsSubtitle.setForeground(Theme.TEXT_MUTED);
    }

    private void keep(Recipe recipe) {
        workingCopies.put(recipe.getId(), recipe);
        current = recipe;
        render();
    }

    private void render() {
        if (current == null) {
            detailCards.show(detail, "none");
            return;
        }
        boolean modified = workingCopies.containsKey(current.getId());
        name.setText(current.getName());
        workingCopyTag.setVisible(modified);
        resetButton.setEnabled(modified);
        meta.setText(current.getId() + " · " + current.getCookingTime() + " min"
                + (current.getDietaryTags().isEmpty() ? "" : " · " + String.join(", ", current.getDietaryTags())));
        servings.setValue(current.getServings());

        ingredientBox.removeAllItems();
        ingredientRows.removeAll();
        Map<String, String> notes = substitutionNotes.getOrDefault(current.getId(), Map.of());
        for (RecipeIngredient ri : current.getIngredients()) {
            ingredientBox.addItem(ri.getName());
            ingredientRows.add(ingredientRow(Units.format(ri.getQuantity(), ri.getUnit()), ri.getName(), notes.get(ri.getName())));
        }
        ingredientsTitle.setText("Ingredients (" + current.getServings() + " servings)");

        try {
            NutritionInfo total = facade.calculateRecipeNutrition(current);
            NutritionInfo per = total.scale(1.0 / current.getServings());
            nutritionTotal.setText(String.format(Locale.ROOT, "<html>Total (%d servings): <b>%,.0f kcal</b></html>",
                    current.getServings(), total.getCalories()));
            nutritionPerServing.setText(String.format(Locale.ROOT, "Per serving: %.0f kcal · P %.0f g · C %.0f g · F %.0f g",
                    per.getCalories(), per.getProtein(), per.getCarbohydrates(), per.getFat()));
        } catch (SmartPlateException e) {
            nutritionTotal.setText(e.getMessage());
            nutritionPerServing.setText(" ");
        }

        steps.removeAll();
        for (int i = 0; i < current.getSteps().size(); i++) {
            JLabel step = new JLabel("<html><div style='width:240px'>" + (i + 1) + ". " + current.getSteps().get(i) + "</div></html>");
            step.setFont(Theme.regular(13f));
            step.setForeground(Theme.TRACE_TEXT);
            step.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
            steps.add(step);
        }
        detailCards.show(detail, "recipe");
        revalidate();
        repaint();
    }

    private JComponent ingredientRow(String qty, String ingredient, String note) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.ROW_LINE),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        JLabel q = new JLabel(qty);
        q.setFont(Theme.mono(12.5f));
        q.setForeground(Theme.TRACE_TEXT);
        q.setPreferredSize(new Dimension(72, 20));
        JLabel n = new JLabel(ingredient);
        n.setFont(Theme.regular(14f));
        row.add(q, BorderLayout.WEST);
        row.add(n, BorderLayout.CENTER);
        if (note != null) {
            JLabel tag = new JLabel(note);
            tag.setFont(Theme.regular(12f));
            tag.setForeground(Theme.WARN_FG);
            row.add(tag, BorderLayout.EAST);
        }
        return row;
    }

    private void showStatus(AlertBox.Kind kind, String message) {
        status.show(kind, null, message);
        status.setVisible(true);
        revalidate();
    }

    /** "butter" or, if butter itself replaced something earlier, the earliest original. */
    private String noteFor(String ingredient, String recipeId) {
        Map<String, String> notes = substitutionNotes.getOrDefault(recipeId, Map.of());
        String earlier = notes.get(ingredient);
        return earlier != null ? earlier.substring("replaces ".length()) : ingredient;
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    // ================================================================== accessors for tests

    JTextField queryField() {
        return query;
    }

    DefaultTableModel resultsModel() {
        return resultsModel;
    }

    Recipe currentRecipe() {
        return current;
    }

    boolean isWorkingCopy() {
        return workingCopyTag.isVisible();
    }

    JSpinner servingsSpinner() {
        return servings;
    }

    JComboBox<String> ingredientBox() {
        return ingredientBox;
    }

    AlertBox status() {
        return status;
    }

    String substitutionNote(String ingredient) {
        return current == null ? null : substitutionNotes.getOrDefault(current.getId(), Map.of()).get(ingredient);
    }

    String resultsSubtitle() {
        return resultsSubtitle.getText();
    }
}
