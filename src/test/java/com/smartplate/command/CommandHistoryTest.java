package com.smartplate.command;

import com.smartplate.TestData;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.plan.MealPlan;
import com.smartplate.plan.PlannedMeal;
import com.smartplate.service.RecipeRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHistoryTest {

    @Test
    void executeAndUndoInReverseOrder() {
        RecipeRepository repo = RecipeRepository.withSampleRecipes();
        MealPlan plan = new MealPlan();
        PlannedMeal original = new PlannedMeal("Monday", TestData.pasta(), 2);
        plan.addMeal(original);
        Recipe a = repo.findById("r01");
        Recipe b = repo.findById("r02");
        PlannedMeal mealA = new PlannedMeal("Monday", a, 2);
        PlannedMeal mealB = new PlannedMeal("Monday", b, 2);

        CommandHistory history = new CommandHistory();
        assertFalse(history.canUndo());
        history.execute(new ReplaceMealCommand(plan, "Monday", original, mealA));
        history.execute(new ReplaceMealCommand(plan, "Monday", mealA, mealB));
        assertEquals(b, plan.getMeal("Monday").getRecipe());
        assertTrue(history.canUndo());

        history.undo();
        assertEquals(a, plan.getMeal("Monday").getRecipe());
        history.undo();
        assertEquals(original, plan.getMeal("Monday"));
        assertThrows(SmartPlateException.class, history::undo);
    }
}
