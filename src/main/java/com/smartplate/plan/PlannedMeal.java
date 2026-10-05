package com.smartplate.plan;

import com.smartplate.domain.NutritionInfo;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.SmartPlateException;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite pattern - Leaf. One day's dinner: a recipe cooked for a number of servings.
 */
public final class PlannedMeal implements PlanComponent {

    private final String day;
    private final Recipe recipe;
    private final int servings;

    public PlannedMeal(String day, Recipe recipe, int servings) {
        String normalizedDay = Days.normalize(day);
        if (normalizedDay == null) {
            throw new SmartPlateException("'" + day + "' is not a day of the week.");
        }
        if (recipe == null) {
            throw new SmartPlateException("A planned meal needs a recipe.");
        }
        if (servings < 1) {
            throw new SmartPlateException("Servings must be at least 1.");
        }
        this.day = normalizedDay;
        this.recipe = recipe;
        this.servings = servings;
    }

    public String getDay() {
        return day;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public int getServings() {
        return servings;
    }

    /** Nutrition per serving times the number of servings. */
    @Override
    public NutritionInfo getNutrition() {
        NutritionInfo perServing = recipe.getNutritionPerServing();
        if (perServing == null) {
            throw new SmartPlateException("Nutrition data is missing for " + recipe.getName() + ".");
        }
        return perServing.scale(servings);
    }

    /** Recipe ingredients scaled from the recipe's servings to this meal's servings. */
    @Override
    public List<RecipeIngredient> getIngredients() {
        double factor = (double) servings / recipe.getServings();
        List<RecipeIngredient> scaled = new ArrayList<>();
        for (RecipeIngredient ri : recipe.getIngredients()) {
            scaled.add(ri.scaled(factor));
        }
        return scaled;
    }

    @Override
    public int getCookingTime() {
        return recipe.getCookingTime();
    }

    @Override
    public String toString() {
        return day + ": " + recipe.getName() + " (" + recipe.getId() + ", " + servings
                + " servings, " + recipe.getCookingTime() + " min)";
    }
}
