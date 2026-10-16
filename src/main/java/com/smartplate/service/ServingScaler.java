package com.smartplate.service;

import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.SmartPlateException;

import java.util.ArrayList;
import java.util.List;

/** F07: scales a recipe to a new number of servings. Returns a copy. */
public class ServingScaler {

    public Recipe scale(Recipe recipe, int servings) {
        if (servings < 1) {
            throw new SmartPlateException("Servings must be at least 1.");
        }
        if (recipe.getServings() < 1) {
            throw new SmartPlateException(recipe.getName() + " has an invalid serving count.");
        }
        double factor = (double) servings / recipe.getServings();
        List<RecipeIngredient> scaled = new ArrayList<>();
        for (RecipeIngredient ri : recipe.getIngredients()) {
            if (!ri.hasValidQuantity()) {
                throw new SmartPlateException(recipe.getName() + " has an invalid quantity for "
                        + ri.getName() + ".");
            }
            scaled.add(ri.scaled(factor));
        }
        return recipe.withServingsAndIngredients(servings, scaled);
    }
}
