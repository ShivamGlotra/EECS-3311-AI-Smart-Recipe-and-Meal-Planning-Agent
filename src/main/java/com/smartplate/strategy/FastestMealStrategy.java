package com.smartplate.strategy;

import com.smartplate.domain.Pantry;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.UserProfile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Strategy pattern - Concrete strategy. Prefers the quickest recipes. */
public class FastestMealStrategy implements RecipeSelectionStrategy {

    @Override
    public List<Recipe> selectRecipes(List<Recipe> recipes, UserProfile profile, Pantry pantry) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe r : recipes) {
            if (RecipeSelectionStrategy.fitsProfile(r, profile)) {
                result.add(r);
            }
        }
        result.sort(Comparator.comparingInt(Recipe::getCookingTime).thenComparing(Recipe::getId));
        return result;
    }

    @Override
    public String getName() {
        return "fastest";
    }
}
