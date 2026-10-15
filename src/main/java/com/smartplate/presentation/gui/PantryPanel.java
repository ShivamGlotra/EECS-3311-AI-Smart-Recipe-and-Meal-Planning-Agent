package com.smartplate.presentation.gui;

import com.formdev.flatlaf.FlatClientProperties;
import com.smartplate.domain.Ingredient;
import com.smartplate.domain.PantryAction;
import com.smartplate.domain.PantryItem;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.domain.Units;
import com.smartplate.facade.SmartRecipeFacade;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;

/** The Pantry tab (F02): add, update and remove items. Calls only SmartRecipeFacade.managePantry(). */
public class PantryPanel extends JPanel {

    static final String[] CATEGORIES = {"other", "vegetable", "fruit", "grain", "legume", "meat", "seafood",
            "dairy", "egg", "nut", "oil", "spice", "condiment", "herb", "sweetener"};

    private final SmartRecipeFacade facade;
    private final DefaultTableModel model = Ui.readOnlyModel("INGREDIENT", "QUANTITY", "UNIT", "CATEGORY");
    private final JTable table = Ui.table(model);
    private final JLabel subtitle = Ui.muted(" ");
    private final JTextField nameField = new JTextField();
    private final JTextField quantityField = new JTextField();
    private final JComboBox<String> unitBox = new JComboBox<>(Units.SUPPORTED.toArray(new String[0]));
    private final JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
    private final JLabel message = Ui.errorLabel();
    private final JButton addButton = Ui.secondary("Add");
    private final JButton updateButton = Ui.primary("Update");
    private final JButton removeButton = Ui.danger("Remove");

    public PantryPanel(SmartRecipeFacade facade) {
        super(new BorderLayout(16, 0));
        this.facade = facade;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        add(buildTable(), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.EAST);
        refresh();
    }

    private JComponent buildTable() {
        RoundedPanel card = Ui.card();
        card.add(Ui.cardHeader("Pantry", subtitle), BorderLayout.NORTH);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, r, c);
                l.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
                l.setFont(c == 0 ? Theme.semibold(13f) : c == 1 ? Theme.mono(12.5f) : Theme.regular(13f));
                l.setForeground(c == 3 ? Theme.TEXT_MUTED : Theme.TEXT);
                l.setBackground(sel ? Theme.ACCENT_TINT : Theme.SURFACE);
                return l;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                nameField.setText(String.valueOf(model.getValueAt(row, 0)));
                quantityField.setText(String.valueOf(model.getValueAt(row, 1)));
                unitBox.setSelectedItem(model.getValueAt(row, 2));
                categoryBox.setSelectedItem(model.getValueAt(row, 3));
                clearErrors();
            }
        });
        card.add(Ui.scroll(table), BorderLayout.CENTER);
        return card;
    }

    private JComponent buildForm() {
        JPanel column = Ui.stack();
        column.setPreferredSize(new Dimension(340, 10));

        RoundedPanel form = new RoundedPanel(new BorderLayout(), Theme.SURFACE, Theme.BORDER, 12, 16);
        JPanel fields = Ui.stack();
        JLabel title = Ui.heading("Add, update or remove", 15f);
        JLabel hint = Ui.muted("Select a row to fill the form.");
        fields.add(left(title));
        fields.add(left(hint));
        fields.add(Ui.gap(12));
        fields.add(left(labelled("Ingredient", nameField)));
        fields.add(Ui.gap(10));
        JPanel pair = new JPanel(new GridLayout(1, 2, 10, 0));
        pair.setOpaque(false);
        pair.add(labelled("Quantity", quantityField));
        pair.add(labelled("Unit", unitBox));
        fields.add(left(pair));
        fields.add(Ui.gap(10));
        fields.add(left(labelled("Category", categoryBox)));
        fields.add(Ui.gap(8));
        fields.add(left(message));
        fields.add(Ui.gap(8));
        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        buttons.setOpaque(false);
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(removeButton);
        fields.add(left(buttons));
        form.add(fields, BorderLayout.CENTER);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, form.getPreferredSize().height));
        column.add(form);
        column.add(Ui.gap(16));

        RoundedPanel note = new RoundedPanel(new BorderLayout(), Theme.ACCENT_TINT, null, 10, 12);
        JLabel text = new JLabel("<html><div style='width:240px'>Units: g, kg, ml, l, pcs. Adding something you "
                + "already have adds to it (500 ml + 0.5 l = 1000 ml).</div></html>");
        text.setFont(Theme.regular(12.5f));
        text.setForeground(Theme.ACCENT_PRESSED);
        note.add(text, BorderLayout.CENTER);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, note.getPreferredSize().height));
        column.add(note);

        nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. olive oil");
        quantityField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "e.g. 500");
        addButton.addActionListener(e -> apply(PantryAction.ADD));
        updateButton.addActionListener(e -> apply(PantryAction.UPDATE));
        removeButton.addActionListener(e -> apply(PantryAction.REMOVE));
        return column;
    }

    /** F02: sends the form to the facade and shows the result or the error next to the right field. */
    void apply(PantryAction action) {
        clearErrors();
        String name = nameField.getText().trim();
        double quantity = 0;
        if (action != PantryAction.REMOVE) {
            try {
                quantity = Double.parseDouble(quantityField.getText().trim());
            } catch (NumberFormatException e) {
                showError("Quantity must be a number.", quantityField);
                return;
            }
        }
        try {
            PantryItem item = new PantryItem(new Ingredient(name, (String) categoryBox.getSelectedItem()),
                    quantity, (String) unitBox.getSelectedItem());
            facade.managePantry(action, item);
        } catch (SmartPlateException e) {
            String msg = e.getMessage();
            String lower = msg.toLowerCase(Locale.ROOT);
            JComponent field;
            if (lower.contains("quantity")) {
                field = quantityField;
            } else if (lower.contains("not in the pantry") || lower.contains("name")) {
                field = nameField;
            } else if (lower.contains("unit") || lower.contains("already in the pantry")) {
                field = unitBox;
            } else {
                field = nameField;
            }
            showError(msg, field);
            return;
        }
        refresh();
        if (action == PantryAction.REMOVE) {
            nameField.setText("");
            quantityField.setText("");
        } else {
            selectRow(name.toLowerCase(Locale.ROOT)); // selecting a row clears the form messages, so do it first
        }
        String done = action == PantryAction.ADD ? "Added " : action == PantryAction.UPDATE ? "Updated " : "Removed ";
        message.setForeground(Theme.SUCCESS_FG);
        message.setText(done + name.toLowerCase(Locale.ROOT) + ".");
    }

    /** Reloads the table from the facade's pantry. */
    public void refresh() {
        List<PantryItem> items = facade.getPantry().getItems();
        model.setRowCount(0);
        for (PantryItem item : items) {
            double q = Math.round(item.getQuantity() * 100) / 100.0;
            String qty = q == Math.rint(q) ? String.valueOf((long) q) : String.valueOf(q);
            model.addRow(new Object[]{item.getName(), qty, item.getUnit(), item.getIngredient().getCategory()});
        }
        subtitle.setText(items.size() + " items · used by the pantry-first strategy and subtracted from shopping lists");
    }

    private void selectRow(String name) {
        for (int r = 0; r < model.getRowCount(); r++) {
            if (name.equals(model.getValueAt(r, 0))) {
                table.setRowSelectionInterval(r, r);
                return;
            }
        }
    }

    private void showError(String text, JComponent field) {
        message.setForeground(Theme.ERROR_FG);
        message.setText("<html><div style='width:250px'>" + text + "</div></html>");
        field.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
    }

    private void clearErrors() {
        message.setText(" ");
        for (JComponent c : new JComponent[]{nameField, quantityField, unitBox}) {
            c.putClientProperty(FlatClientProperties.OUTLINE, null);
        }
    }

    private static JComponent labelled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = Ui.fieldLabel(label);
        l.setLabelFor(field);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    // ================================================================== accessors for tests

    JTextField nameField() {
        return nameField;
    }

    JTextField quantityField() {
        return quantityField;
    }

    JComboBox<String> unitBox() {
        return unitBox;
    }

    DefaultTableModel model() {
        return model;
    }

    String messageText() {
        return message.getText();
    }

    boolean hasErrorOutline(JComponent c) {
        return FlatClientProperties.OUTLINE_ERROR.equals(c.getClientProperty(FlatClientProperties.OUTLINE));
    }
}
