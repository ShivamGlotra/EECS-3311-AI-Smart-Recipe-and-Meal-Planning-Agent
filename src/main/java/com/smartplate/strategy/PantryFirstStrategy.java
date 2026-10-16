package com.smartplate.strategy;

import com.smartplate.domain.Pantry;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.UserProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Strategy pattern - Concrete strategy. Prefers recipes that use the most ingredients the
 * user already has (reduces shopping and waste). Ties go to the faster recipe.
 */
public class PantryFirstStrategy implements RecipeSelectionStrategy {

    @Override
    public List<Recipe> selectRecipes(List<Recipe> recipes, UserProfile profile, Pantry pantry) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe r : recipes) {
            if (RecipeSelectionStrategy.fitsProfile(r, profile)) {
                result.add(r);
            }
        }
        result.sort(Comparator.comparingInt((Recipe r) -> -pantryMatches(r, pantry))
                .thenComparingInt(Recipe::getCookingTime)
                .thenComparing(Recipe::getId));
        return result;
    }

    /** Number of the recipe's ingredients that are in the pantry. */
    public static int pantryMatches(Recipe recipe, Pantry pantry) {
        int count = 0;
        for (RecipeIngredient ri : recipe.getIngredients()) {
            if (pantry != null && pantry.contains(ri.getName())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String getName() {
        return "pantry-first";
    }
}
