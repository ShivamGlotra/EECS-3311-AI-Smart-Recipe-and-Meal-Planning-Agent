package com.smartplate.domain;

/** An ingredient line of a recipe: ingredient, quantity and unit. Immutable. */
public final class RecipeIngredient {

    private final Ingredient ingredient;
    private final double quantity;
    private final String unit;

    public RecipeIngredient(Ingredient ingredient, double quantity, String unit) {
        if (ingredient == null) {
            throw new SmartPlateException("Recipe ingredient needs an ingredient.");
        }
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

    /** True when the quantity is a positive number and the unit is supported. */
    public boolean hasValidQuantity() {
        return quantity > 0 && !Double.isInfinite(quantity) && Units.isSupported(unit);
    }

    public RecipeIngredient scaled(double factor) {
        return new RecipeIngredient(ingredient, quantity * factor, unit);
    }

    @Override
    public String toString() {
        return Units.format(quantity, unit) + " " + ingredient.getName();
    }
}
