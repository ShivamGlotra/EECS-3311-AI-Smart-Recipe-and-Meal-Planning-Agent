package com.smartplate.strategy;

import com.smartplate.TestData;
import com.smartplate.domain.Pantry;
import com.smartplate.domain.PantryItem;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.UserProfile;
import com.smartplate.service.RecipeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategyTest {

    private final List<Recipe> recipes = RecipeRepository.withSampleRecipes().findAll();

    @Test
    void fastestSortsByCookingTime() {
        List<Recipe> result = new FastestMealStrategy().selectRecipes(recipes, TestData.noRestrictions(), new Pantry());
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).getCookingTime() <= result.get(i).getCookingTime());
        }
        assertEquals(recipes.size(), result.size());
    }

    @Test
    void pantryFirstPrefersRecipesUsingPantryItems() {
        Pantry pantry = new Pantry();
        pantry.addItem(new PantryItem("lentils", 1, "kg"));
        pantry.addItem(new PantryItem("carrot", 4, "pcs"));
        pantry.addItem(new PantryItem("celery", 4, "pcs"));
        pantry.addItem(new PantryItem("cumin", 50, "g"));
        List<Recipe> result = new PantryFirstStrategy().selectRecipes(recipes, TestData.noRestrictions(), pantry);
        assertEquals("r09", result.get(0).getId());
    }

    @Test
    void bothStrategiesDropRecipesThatDoNotFitTheProfile() {
        UserProfile p = TestData.profile(List.of("vegan"), List.of(), List.of(), 20);
        for (RecipeSelectionStrategy s : List.of(new FastestMealStrategy(), new PantryFirstStrategy())) {
            for (Recipe r : s.selectRecipes(recipes, p, new Pantry())) {
                assertTrue(r.getCookingTime() <= 20);
                assertTrue(r.hasTag("vegan"), r.getName());
            }
        }
    }
}
