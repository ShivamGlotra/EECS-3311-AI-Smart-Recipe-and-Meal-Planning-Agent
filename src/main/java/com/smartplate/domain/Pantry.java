package com.smartplate.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The user's pantry (F02). Rejects non-positive quantities, unsupported units and
 * updates/removals of items that are not there.
 */
public class Pantry {

    private final List<PantryItem> items = new ArrayList<>();

    /**
     * Adds an item. If the ingredient is already in the pantry with a compatible unit,
     * the quantities are added together (kept in the existing unit).
     */
    public void addItem(PantryItem item) {
        validate(item);
        PantryItem existing = findItem(item.getName());
        if (existing == null) {
            items.add(item);
            return;
        }
        if (!Units.compatible(existing.getUnit(), item.getUnit())) {
            throw new SmartPlateException("'" + item.getName() + "' is already in the pantry in "
                    + existing.getUnit() + "; cannot add " + item.getUnit() + ". Use UPDATE instead.");
        }
        double added = Units.convert(item.getQuantity(), item.getUnit(), existing.getUnit());
        replace(existing, new PantryItem(existing.getIngredient(),
                existing.getQuantity() + added, existing.getUnit()));
    }

    /** Replaces the quantity and unit of an existing item. */
    public void updateItem(PantryItem item) {
        validate(item);
        PantryItem existing = findItem(item.getName());
        if (existing == null) {
            throw new SmartPlateException("'" + item.getName() + "' is not in the pantry.");
        }
        replace(existing, item);
    }

    public void removeItem(String name) {
        PantryItem existing = findItem(name);
        if (existing == null) {
            throw new SmartPlateException("'" + name + "' is not in the pantry.");
        }
        items.remove(existing);
    }

    /** Returns the item with this ingredient name (case-insensitive), or null. */
    public PantryItem findItem(String name) {
        if (name == null) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        for (PantryItem item : items) {
            if (item.getName().equals(key)) {
                return item;
            }
        }
        return null;
    }

    public boolean contains(String name) {
        return findItem(name) != null;
    }

    public List<PantryItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private void replace(PantryItem oldItem, PantryItem newItem) {
        items.set(items.indexOf(oldItem), newItem);
    }

    private static void validate(PantryItem item) {
        if (item == null) {
            throw new SmartPlateException("No pantry item given.");
        }
        if (!(item.getQuantity() > 0)) {
            throw new SmartPlateException("Quantity must be greater than zero.");
        }
        if (!Units.isSupported(item.getUnit())) {
            throw new SmartPlateException("Unsupported unit '" + item.getUnit()
                    + "'. Use one of " + Units.SUPPORTED + ".");
        }
    }

    @Override
    public String toString() {
        if (items.isEmpty()) {
            return "(pantry is empty)";
        }
        StringBuilder sb = new StringBuilder();
        for (PantryItem item : items) {
            sb.append("- ").append(item).append('\n');
        }
        return sb.toString().trim();
    }
}
