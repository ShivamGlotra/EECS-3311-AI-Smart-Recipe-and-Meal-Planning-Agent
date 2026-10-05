package com.smartplate.service;

import com.smartplate.domain.NutritionInfo;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.plan.PlanComponent;

/** F09: deterministic nutrition totals for a recipe or any plan component. */
public class NutritionService {

    /** Total nutrition for the recipe as written (per-serving values times its servings). */
    public NutritionInfo calculateRecipe(Recipe recipe) {
        if (recipe == null) {
            throw new SmartPlateException("No recipe selected.");
        }
        if (recipe.getNutritionPerServing() == null) {
            throw new SmartPlateException("Nutrition data is missing for " + recipe.getName() + ".");
        }
        if (recipe.getServings() < 1) {
            throw new SmartPlateException(recipe.getName() + " has an invalid serving count.");
        }
        return recipe.getNutritionPerServing().scale(recipe.getServings());
    }

    /**
     * Total nutrition of a plan. Works for a MealPlan or a single PlannedMeal without
     * checking which one it is (Composite pattern).
     */
    public NutritionInfo calculatePlan(PlanComponent plan) {
        if (plan == null || plan.getIngredients().isEmpty()) {
            throw new SmartPlateException("The meal plan is empty.");
        }
        return plan.getNutrition();
    }
}
