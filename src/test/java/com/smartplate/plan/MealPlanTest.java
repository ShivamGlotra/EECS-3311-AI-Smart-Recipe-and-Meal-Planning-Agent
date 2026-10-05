package com.smartplate.plan;

import com.smartplate.TestData;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.SmartPlateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MealPlanTest {

    @Test
    void leafScalesNutritionAndIngredientsToItsServings() {
        PlannedMeal meal = new PlannedMeal("mon", TestData.pasta(), 4);
        assertEquals("Monday", meal.getDay());
        assertEquals(2000, meal.getNutrition().getCalories(), 1e-9);
        assertEquals(400, meal.getIngredients().get(0).getQuantity(), 1e-9);
    }

    @Test
    void compositeSumsItsChildren() {
        Recipe r = TestData.pasta();
        MealPlan plan = new MealPlan();
        plan.addMeal(new PlannedMeal("Tuesday", r, 2));
        plan.addMeal(new PlannedMeal("Monday", r, 1));
        assertEquals("Monday", plan.getMeals().get(0).getDay()); // kept in week order
        assertEquals(1500, plan.getNutrition().getCalories(), 1e-9);
        assertEquals(6, plan.getIngredients().size());
        assertEquals(40, plan.getCookingTime());
    }

    @Test
    void oneMealPerDayAndAtMostSevenDays() {
        MealPlan plan = new MealPlan();
        Recipe r = TestData.pasta();
        for (String day : Days.WEEK) {
            plan.addMeal(new PlannedMeal(day, r, 2));
        }
        assertThrows(SmartPlateException.class, () -> plan.addMeal(new PlannedMeal("Monday", r, 2)));
        assertThrows(SmartPlateException.class, () -> new PlannedMeal("Funday", r, 2));
        assertThrows(SmartPlateException.class, () -> new PlannedMeal("Monday", r, 0));
    }

    @Test
    void replaceMealOnExistingDayOnly() {
        MealPlan plan = new MealPlan();
        Recipe r = TestData.pasta();
        plan.addMeal(new PlannedMeal("Monday", r, 2));
        PlannedMeal replacement = new PlannedMeal("Monday", r, 3);
        plan.replaceMeal("monday", replacement);
        assertSame(replacement, plan.getMeal("Monday"));
        assertThrows(SmartPlateException.class, () -> plan.replaceMeal("Friday", new PlannedMeal("Friday", r, 2)));
    }
}
