package com.smartplate.presentation;

import com.smartplate.domain.Ingredient;
import com.smartplate.domain.NutritionInfo;
import com.smartplate.domain.PantryAction;
import com.smartplate.domain.PantryItem;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.domain.Units;
import com.smartplate.domain.UserProfile;
import com.smartplate.facade.SmartRecipeFacade;
import com.smartplate.plan.MealPlan;
import com.smartplate.plan.PlannedMeal;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Swing desktop interface. Like the CLI it calls only SmartRecipeFacade.
 * AI requests run on a background thread (SwingWorker) so the window never freezes while
 * the model answers.
 */
public class MealPlannerGUI {

    private static final String[] CATEGORIES = {"other", "vegetable", "fruit", "grain", "legume", "meat",
            "seafood", "dairy", "egg", "nut", "oil", "spice", "condiment", "herb", "sweetener"};

    private final SmartRecipeFacade facade;
    private JFrame frame;

    // profile tab
    private final List<JCheckBox> dietBoxes = new ArrayList<>();
    private final JTextField allergiesField = new JTextField(30);
    private final JTextField dislikesField = new JTextField(30);
    private final JSpinner timeSpinner = new JSpinner(new SpinnerNumberModel(60, 0, 600, 5));
    private final JSpinner servingsSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 20, 1));

    // pantry tab
    private final DefaultTableModel pantryModel = new DefaultTableModel(new Object[]{"Ingredient", "Quantity", "Unit"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTextField pantryName = new JTextField(14);
    private final JTextField pantryQty = new JTextField(6);
    private final JComboBox<String> pantryUnit = new JComboBox<>(Units.SUPPORTED.toArray(new String[0]));
    private final JComboBox<String> pantryCategory = new JComboBox<>(CATEGORIES);

    // recipes tab
    private final JTextField searchField = new JTextField(24);
    private final DefaultListModel<Recipe> resultModel = new DefaultListModel<>();
    private final JList<Recipe> resultList = new JList<>(resultModel);
    private final JTextArea recipeDetails = textArea();
    private final JSpinner scaleSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
    private final JComboBox<String> ingredientBox = new JComboBox<>();
    private Recipe selectedRecipe; // working copy shown on the right

    // assistant tab
    private final JTextArea requestArea = new JTextArea(3, 40);
    private final JTextArea assistantOutput = textArea();
    private final JTextArea traceArea = textArea();
    private final DefaultTableModel planModel = new DefaultTableModel(new Object[]{"Day", "Recipe", "Id", "Servings", "Minutes"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JButton undoButton = new JButton("Undo last change");
    private final JComboBox<String> strategyBox = new JComboBox<>(new String[]{"pantry-first", "fastest"});
    private final List<JButton> aiButtons = new ArrayList<>();

    public MealPlannerGUI(SmartRecipeFacade facade) {
        this.facade = facade;
    }

    /** Builds and shows the window on the Swing event thread. */
    public void start() {
        SwingUtilities.invokeLater(this::build);
    }

    private void build() {
        frame = new JFrame("SmartPlate - AI Meal Planning Agent");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("AI Assistant", assistantTab());
        tabs.addTab("Recipes", recipesTab());
        tabs.addTab("Pantry", pantryTab());
        tabs.addTab("Preferences", profileTab());
        frame.add(tabs, BorderLayout.CENTER);
        JLabel status = new JLabel("  AI model: " + facade.getAIModelName());
        status.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        frame.add(status, BorderLayout.SOUTH);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);
        refreshPantry();
        loadProfile();
        frame.setVisible(true);
    }

    // ================================================================== AI assistant (F04, F05, F06, F09, F10)

    private JComponent assistantTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        requestArea.setLineWrap(true);
        requestArea.setWrapStyleWord(true);
        requestArea.setText("Plan dinners for this week, use some of my pantry items, avoid mushrooms, "
                + "keep meals under 30 minutes");
        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.add(new JLabel("Ask the agent (natural language):"), BorderLayout.NORTH);
        top.add(new JScrollPane(requestArea), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton recommend = new JButton("Recommend recipes");
        JButton plan = new JButton("Create weekly plan");
        JButton edit = new JButton("Edit plan");
        aiButtons.addAll(Arrays.asList(recommend, plan, edit));
        recommend.addActionListener(e -> runAI(() -> facade.recommendRecipes(requestArea.getText()), recs -> {
            StringBuilder sb = new StringBuilder("Recommended recipes (validated against your profile):\n");
            for (Recipe r : recs) {
                sb.append("  ").append(r.summary()).append('\n');
            }
            assistantOutput.setText(sb.toString());
        }));
        plan.addActionListener(e -> runAI(() -> facade.createMealPlan(requestArea.getText()), p -> {
            showPlan(p);
            assistantOutput.setText("New plan created and validated.\n" + p);
        }));
        edit.addActionListener(e -> runAI(() -> facade.editMealPlan(requestArea.getText()), p -> {
            showPlan(p);
            assistantOutput.setText("Plan updated. Press Undo to revert.\n" + p);
        }));
        undoButton.addActionListener(e -> attempt(() -> {
            MealPlan p = facade.undoLastChange();
            showPlan(p);
            assistantOutput.setText("Last change undone.\n" + p);
        }));
        undoButton.setEnabled(false);
        strategyBox.addActionListener(e -> attempt(() -> facade.setStrategy((String) strategyBox.getSelectedItem())));
        buttons.add(recommend);
        buttons.add(plan);
        buttons.add(edit);
        buttons.add(undoButton);
        buttons.add(new JLabel("  Strategy:"));
        buttons.add(strategyBox);
        top.add(buttons, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        JTable planTable = new JTable(planModel);
        JPanel planPanel = new JPanel(new BorderLayout());
        planPanel.setBorder(BorderFactory.createTitledBorder("Current meal plan"));
        planPanel.add(new JScrollPane(planTable), BorderLayout.CENTER);
        JPanel planButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton nutrition = new JButton("Plan nutrition");
        JButton shopping = new JButton("Generate shopping list");
        nutrition.addActionListener(e -> attempt(() -> {
            NutritionInfo total = facade.calculatePlanNutrition();
            StringBuilder sb = new StringBuilder("Nutrition for the whole plan:\n  " + total + "\n\nPer day:\n");
            for (PlannedMeal meal : facade.getCurrentPlan().getMeals()) {
                sb.append("  ").append(meal.getDay()).append(": ").append(meal.getNutrition()).append('\n');
            }
            assistantOutput.setText(sb.toString());
        }));
        shopping.addActionListener(e -> attempt(() ->
                assistantOutput.setText("Shopping list (plan minus pantry):\n" + facade.generateShoppingList())));
        planButtons.add(nutrition);
        planButtons.add(shopping);
        planPanel.add(planButtons, BorderLayout.SOUTH);

        JPanel outputs = new JPanel(new GridLayout(1, 2, 8, 8));
        outputs.add(titled(new JScrollPane(assistantOutput), "Result"));
        outputs.add(titled(new JScrollPane(traceArea), "Agent trace (what the agent did)"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, planPanel, outputs);
        split.setResizeWeight(0.45);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void showPlan(MealPlan plan) {
        planModel.setRowCount(0);
        for (PlannedMeal meal : plan.getMeals()) {
            planModel.addRow(new Object[]{meal.getDay(), meal.getRecipe().getName(), meal.getRecipe().getId(),
                    meal.getServings(), meal.getCookingTime()});
        }
        undoButton.setEnabled(facade.canUndo());
    }

    /** Runs an AI request in the background, then shows the result and the agent trace. */
    private <T> void runAI(Callable<T> task, Consumer<T> onSuccess) {
        aiButtons.forEach(b -> b.setEnabled(false));
        assistantOutput.setText("Thinking...");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                aiButtons.forEach(b -> b.setEnabled(true));
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    assistantOutput.setText("Could not complete the request:\n" + cause.getMessage());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                traceArea.setText(String.join("\n", facade.getLastAgentTrace()));
                undoButton.setEnabled(facade.canUndo());
            }
        }.execute();
    }

    // ================================================================== Recipes (F03, F07, F08, F09)

    private JComponent recipesTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton search = new JButton("Search");
        JButton all = new JButton("Show all");
        search.addActionListener(e -> attempt(() -> showResults(facade.searchRecipes(searchField.getText()))));
        searchField.addActionListener(e -> search.doClick());
        all.addActionListener(e -> showResults(facade.getAllRecipes()));
        searchBar.add(new JLabel("Search (name, tag or ingredient):"));
        searchBar.add(searchField);
        searchBar.add(search);
        searchBar.add(all);
        panel.add(searchBar, BorderLayout.NORTH);

        resultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && resultList.getSelectedValue() != null) {
                selectRecipe(resultList.getSelectedValue());
            }
        });

        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.add(new JScrollPane(recipeDetails), BorderLayout.CENTER);
        JPanel tools = new JPanel(new GridLayout(2, 1));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton scale = new JButton("Scale");
        JButton nutrition = new JButton("Nutrition");
        JButton reset = new JButton("Reset");
        scale.addActionListener(e -> withRecipe(r -> selectRecipe(facade.adjustServings(r, (Integer) scaleSpinner.getValue()))));
        nutrition.addActionListener(e -> withRecipe(r -> {
            NutritionInfo total = facade.calculateRecipeNutrition(r);
            info("Nutrition for " + r.getName() + " (" + r.getServings() + " servings):\n" + total
                    + "\nPer serving: " + total.scale(1.0 / r.getServings()));
        }));
        reset.addActionListener(e -> withRecipe(r -> selectRecipe(facade.getRecipe(r.getId()))));
        row1.add(new JLabel("Servings:"));
        row1.add(scaleSpinner);
        row1.add(scale);
        row1.add(nutrition);
        row1.add(reset);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton substitute = new JButton("Substitute");
        substitute.addActionListener(e -> withRecipe(r -> {
            String name = (String) ingredientBox.getSelectedItem();
            Recipe updated = facade.substituteIngredient(r, new Ingredient(name));
            selectRecipe(updated);
            info("Substituted " + facade.getLastSubstitution());
        }));
        row2.add(new JLabel("Ingredient:"));
        row2.add(ingredientBox);
        row2.add(substitute);
        tools.add(row1);
        tools.add(row2);
        right.add(tools, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(resultList), right);
        split.setDividerLocation(330);
        panel.add(split, BorderLayout.CENTER);
        showResults(facade.getAllRecipes());
        return panel;
    }

    private void showResults(List<Recipe> recipes) {
        resultModel.clear();
        for (Recipe r : recipes) {
            resultModel.addElement(r);
        }
        if (recipes.isEmpty()) {
            recipeDetails.setText("No recipes found.");
        }
    }

    private void selectRecipe(Recipe recipe) {
        selectedRecipe = recipe;
        recipeDetails.setText(recipe.describe());
        recipeDetails.setCaretPosition(0);
        scaleSpinner.setValue(recipe.getServings());
        ingredientBox.removeAllItems();
        for (RecipeIngredient ri : recipe.getIngredients()) {
            ingredientBox.addItem(ri.getName());
        }
    }

    private void withRecipe(Consumer<Recipe> action) {
        if (selectedRecipe == null) {
            error("Select a recipe first.");
            return;
        }
        attempt(() -> action.accept(selectedRecipe));
    }

    // ================================================================== Pantry (F02)

    private JComponent pantryTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JTable table = new JTable(pantryModel);
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                pantryName.setText(String.valueOf(pantryModel.getValueAt(row, 0)));
                pantryQty.setText(String.valueOf(pantryModel.getValueAt(row, 1)));
                pantryUnit.setSelectedItem(pantryModel.getValueAt(row, 2));
            }
        });
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(new JLabel("Ingredient:"));
        form.add(pantryName);
        form.add(new JLabel("Qty:"));
        form.add(pantryQty);
        form.add(pantryUnit);
        form.add(new JLabel("Category:"));
        form.add(pantryCategory);
        for (PantryAction action : PantryAction.values()) {
            JButton b = new JButton(action.name().charAt(0) + action.name().substring(1).toLowerCase());
            b.addActionListener(e -> attempt(() -> {
                double qty = action == PantryAction.REMOVE ? 0 : parseQuantity(pantryQty.getText());
                PantryItem item = new PantryItem(new Ingredient(pantryName.getText(),
                        (String) pantryCategory.getSelectedItem()), qty, (String) pantryUnit.getSelectedItem());
                facade.managePantry(action, item);
                refreshPantry();
            }));
            form.add(b);
        }
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshPantry() {
        pantryModel.setRowCount(0);
        for (PantryItem item : facade.getPantry().getItems()) {
            double q = Math.round(item.getQuantity() * 100) / 100.0;
            pantryModel.addRow(new Object[]{item.getName(), q, item.getUnit()});
        }
    }

    private static double parseQuantity(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new SmartPlateException("Quantity must be a number.");
        }
    }

    // ================================================================== Preferences (F01)

    private JComponent profileTab() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        JPanel diets = new JPanel();
        diets.setLayout(new BoxLayout(diets, BoxLayout.X_AXIS));
        for (String diet : UserProfile.SUPPORTED_DIETS) {
            JCheckBox box = new JCheckBox(diet);
            dietBoxes.add(box);
            diets.add(box);
            diets.add(Box.createHorizontalStrut(6));
        }
        addRow(form, c, 0, "Dietary preferences:", diets);
        addRow(form, c, 1, "Allergies (comma separated):", allergiesField);
        addRow(form, c, 2, "Disliked ingredients (comma separated):", dislikesField);
        addRow(form, c, 3, "Maximum cooking time (min):", timeSpinner);
        addRow(form, c, 4, "Default servings:", servingsSpinner);
        JButton save = new JButton("Save preferences");
        save.addActionListener(e -> attempt(() -> {
            List<String> chosen = new ArrayList<>();
            for (JCheckBox box : dietBoxes) {
                if (box.isSelected()) {
                    chosen.add(box.getText());
                }
            }
            facade.setupProfile(new UserProfile(chosen, split(allergiesField.getText()), split(dislikesField.getText()),
                    (Integer) timeSpinner.getValue(), (Integer) servingsSpinner.getValue()));
            info("Preferences saved.\n\n" + facade.getProfile());
        }));
        c.gridx = 1;
        c.gridy = 5;
        form.add(save, c);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(form, BorderLayout.NORTH);
        return wrapper;
    }

    private void loadProfile() {
        UserProfile p = facade.getProfile();
        for (JCheckBox box : dietBoxes) {
            box.setSelected(p.getDietaryPreferences().contains(box.getText()));
        }
        allergiesField.setText(String.join(", ", p.getAllergies()));
        dislikesField.setText(String.join(", ", p.getDislikedIngredients()));
        timeSpinner.setValue(p.getMaxCookingTime());
        servingsSpinner.setValue(p.getDefaultServings());
    }

    private static void addRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0;
        c.gridy = row;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
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

    // ================================================================== helpers

    private void attempt(Runnable action) {
        try {
            action.run();
        } catch (SmartPlateException e) {
            error(e.getMessage());
        }
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(frame, message, "SmartPlate", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(frame, message, "SmartPlate", JOptionPane.WARNING_MESSAGE);
    }

    private static JTextArea textArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    private static JComponent titled(JComponent component, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(component, BorderLayout.CENTER);
        p.setPreferredSize(new Dimension(400, 200));
        return p;
    }
}
