package com.smartplate.facade;

import com.smartplate.ai.MockAIModel;
import com.smartplate.domain.Ingredient;
import com.smartplate.domain.InvalidProfileException;
import com.smartplate.domain.PantryAction;
import com.smartplate.domain.PantryItem;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.domain.UserProfile;
import com.smartplate.plan.MealPlan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end tests of all ten features through the facade, using the offline Mock AI. */
class SmartRecipeFacadeTest {

    private final SmartRecipeFacade facade = SmartRecipeFacade.create(new MockAIModel());

    @Test
    void f01InvalidProfileKeepsPreviousProfile() {
        UserProfile good = new UserProfile(List.of("vegetarian"), List.of(), List.of(), 30, 2);
        assertSame(good, facade.setupProfile(good));
        InvalidProfileException e = assertThrows(InvalidProfileException.class,
                () -> facade.setupProfile(new UserProfile(List.of(), List.of(), List.of(), 30, 0)));
        assertEquals(1, e.getProblems().size());
        assertSame(good, facade.getProfile());
    }

    @Test
    void f02PantryActions() {
        facade.managePantry(PantryAction.ADD, new PantryItem("rice", 1, "kg"));
        facade.managePantry(PantryAction.UPDATE, new PantryItem("rice", 2, "kg"));
        assertEquals(2, facade.getPantry().findItem("rice").getQuantity(), 1e-9);
        facade.managePantry(PantryAction.REMOVE, new PantryItem("rice", 0, "g"));
        assertTrue(facade.getPantry().isEmpty());
        assertThrows(SmartPlateException.class,
                () -> facade.managePantry(PantryAction.REMOVE, new PantryItem("rice", 0, "g")));
    }

    @Test
    void f03SearchRejectsEmptyQuery() {
        assertThrows(SmartPlateException.class, () -> facade.searchRecipes(" "));
        assertFalse(facade.searchRecipes("salmon").isEmpty());
    }

    @Test
    void f04ToF10FullWorkflow() {
        facade.setupProfile(new UserProfile(List.of("vegetarian"), List.of("peanuts"), List.of("mushrooms"), 40, 2));
        facade.managePantry(PantryAction.ADD, new PantryItem("rice", 1, "kg"));

        List<Recipe> recs = facade.recommendRecipes("3 dinners using rice");
        assertEquals(3, recs.size());

        MealPlan plan = facade.createMealPlan("Plan dinners for this week, avoid tomatoes");
        assertEquals(7, plan.size());
        assertFalse(facade.getLastAgentTrace().isEmpty());

        String before = plan.getMeal("Tuesday").getRecipe().getId();
        facade.editMealPlan("swap Tuesday's dinner for something with spinach");
        assertNotEquals(before, facade.getCurrentPlan().getMeal("Tuesday").getRecipe().getId());
        assertTrue(facade.canUndo());
        facade.undoLastChange();
        assertEquals(before, facade.getCurrentPlan().getMeal("Tuesday").getRecipe().getId());
        assertThrows(SmartPlateException.class, facade::undoLastChange);

        assertTrue(facade.calculatePlanNutrition().getCalories() > 0);
        assertFalse(facade.generateShoppingList().getItems().isEmpty());

        Recipe r = facade.getRecipe("r10");
        assertEquals(6, facade.adjustServings(r, 6).getServings());
        assertThrows(SmartPlateException.class, () -> facade.adjustServings(r, 0));
        Recipe swapped = facade.substituteIngredient(r, new Ingredient("butter"));
        assertTrue(swapped.findIngredient("olive oil") != null);
        assertThrows(SmartPlateException.class, () -> facade.substituteIngredient(r, new Ingredient("chocolate")));
        assertEquals(1240, facade.calculateRecipeNutrition(r).getCalories(), 1e-9);
    }

    @Test
    void planFeaturesNeedAPlan() {
        assertThrows(SmartPlateException.class, () -> facade.editMealPlan("swap monday"));
        assertThrows(SmartPlateException.class, facade::calculatePlanNutrition);
        assertThrows(SmartPlateException.class, facade::generateShoppingList);
    }

    @Test
    void newPlanClearsUndoHistory() {
        facade.createMealPlan("weekday dinners");
        facade.editMealPlan("swap Monday");
        assertTrue(facade.canUndo());
        facade.createMealPlan("weekday dinners");
        assertFalse(facade.canUndo());
    }

    @Test
    void strategyCanBeSwitched() {
        facade.setStrategy("fastest");
        assertEquals("fastest", facade.getStrategyName());
        assertThrows(SmartPlateException.class, () -> facade.setStrategy("random"));
    }
}
