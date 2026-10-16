package com.smartplate.strategy;

import com.smartplate.domain.DietRules;
import com.smartplate.domain.Pantry;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.UserProfile;

import java.util.List;

/**
 * Strategy pattern - Strategy interface.
 *
 * <p>Shortlists and orders the candidate recipes that the agent offers to the AI model.
 * MealPlanningAgent (the Context) calls {@link #selectRecipes} and can switch strategy at
 * run time with {@code setStrategy()}. A new selection rule is a new class.
 */
public interface RecipeSelectionStrategy {

    /** Returns the recipes that fit the profile, best candidates first. */
    List<Recipe> selectRecipes(List<Recipe> recipes, UserProfile profile, Pantry pantry);

    /** Name shown in the UI. */
    String getName();

    /**
     * Shared pre-filter: the recipe is within the time limit and none of its ingredients
     * clashes with the profile. (ConstraintValidator still re-checks the AI's final choice.)
     */
    static boolean fitsProfile(Recipe recipe, UserProfile profile) {
        if (recipe.getCookingTime() > profile.getMaxCookingTime()) {
            return false;
        }
        for (RecipeIngredient ri : recipe.getIngredients()) {
            if (!DietRules.conflicts(ri.getIngredient(), profile).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
