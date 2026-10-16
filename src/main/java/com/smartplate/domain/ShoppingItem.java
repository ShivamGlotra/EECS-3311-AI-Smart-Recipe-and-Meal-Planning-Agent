package com.smartplate.domain;

/** One line of the shopping list. Immutable. */
public final class ShoppingItem {

    private final Ingredient ingredient;
    private final double quantity;
    private final String unit;

    public ShoppingItem(Ingredient ingredient, double quantity, String unit) {
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = Units.normalize(unit);
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
