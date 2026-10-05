package com.smartplate.domain;

import java.util.Locale;

/** Calories (kcal), protein, carbohydrates and fat (grams). Immutable value object. */
public final class NutritionInfo {

    public static final NutritionInfo ZERO = new NutritionInfo(0, 0, 0, 0);

    private final double calories;
    private final double protein;
    private final double carbohydrates;
    private final double fat;

    public NutritionInfo(double calories, double protein, double carbohydrates, double fat) {
        if (calories < 0 || protein < 0 || carbohydrates < 0 || fat < 0) {
            throw new SmartPlateException("Nutrition values cannot be negative.");
        }
        this.calories = calories;
        this.protein = protein;
        this.carbohydrates = carbohydrates;
        this.fat = fat;
    }

    public double getCalories() {
        return calories;
    }

    public double getProtein() {
        return protein;
    }

    public double getCarbohydrates() {
        return carbohydrates;
    }

    public double getFat() {
        return fat;
    }

    public NutritionInfo plus(NutritionInfo other) {
        return new NutritionInfo(calories + other.calories, protein + other.protein,
                carbohydrates + other.carbohydrates, fat + other.fat);
    }

    public NutritionInfo scale(double factor) {
        return new NutritionInfo(calories * factor, protein * factor,
                carbohydrates * factor, fat * factor);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NutritionInfo)) {
            return false;
        }
        NutritionInfo n = (NutritionInfo) o;
        return close(calories, n.calories) && close(protein, n.protein)
                && close(carbohydrates, n.carbohydrates) && close(fat, n.fat);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(Math.round(calories));
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.0f kcal | protein %.1f g | carbs %.1f g | fat %.1f g",
                calories, protein, carbohydrates, fat);
    }
}
