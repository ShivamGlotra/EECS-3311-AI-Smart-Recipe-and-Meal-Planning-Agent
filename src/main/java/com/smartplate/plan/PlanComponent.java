package com.smartplate.plan;

import com.smartplate.domain.NutritionInfo;
import com.smartplate.domain.RecipeIngredient;

import java.util.List;

/**
 * Composite pattern - Component.
 *
 * <p>NutritionService and ShoppingListService only know this interface, so they work the
 * same way for a single PlannedMeal (leaf) and for a whole MealPlan (composite).
 */
public interface PlanComponent {

    /** Total nutrition of this component. */
    NutritionInfo getNutrition();

    /** All ingredient lines needed by this component (already scaled to its servings). */
    List<RecipeIngredient> getIngredients();

    /** Total cooking time in minutes. */
    int getCookingTime();
}
