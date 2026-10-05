package com.smartplate.domain;

/** One ingredient in the pantry with its quantity and unit. Immutable. */
public final class PantryItem {

    private final Ingredient ingredient;
    private final double quantity;
    private final String unit;

    public PantryItem(Ingredient ingredient, double quantity, String unit) {
        if (ingredient == null) {
            throw new SmartPlateException("Pantry item needs an ingredient.");
        }
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = Units.normalize(unit);
    }

    public PantryItem(String name, double quantity, String unit) {
        this(new Ingredient(name), quantity, unit);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public String getName() {
        return ingredient.getName();
    }

    public double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return ingredient.getName() + ": " + Units.format(quantity, unit);
    }
}
