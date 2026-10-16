package com.smartplate.agent;

import com.smartplate.TestData;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.UserProfile;
import com.smartplate.plan.MealPlan;
import com.smartplate.plan.PlannedMeal;
import com.smartplate.service.RecipeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstraintValidatorTest {

    private final ConstraintValidator validator = new ConstraintValidator();
    private final RecipeRepository repo = RecipeRepository.withSampleRecipes();

    @Test
    void validRecipePasses() {
        assertTrue(validator.validateRecipe(repo.findById("r09"), TestData.noRestrictions()).isValid());
    }

    @Test
    void allergenIsFoundInIngredientsEvenIfTagsLookFine() {
        // Peanut Noodles is tagged vegan/vegetarian but contains peanut butter
        UserProfile p = TestData.profile(List.of("vegetarian"), List.of("peanuts"), List.of(), 60);
        ValidationResult result = validator.validateRecipe(repo.findById("r15"), p);
        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("peanut butter"));
    }

    @Test
    void mistaggedRecipeIsStillCaught() {
        Recipe mistagged = Recipe.builder("x1", "Secret Bacon Pasta").tags("vegetarian").time(10)
                .ingredient("bacon", "meat", 100, "g").nutrition(1, 1, 1, 1).build();
        UserProfile p = TestData.profile(List.of("vegetarian"), List.of(), List.of(), 60);
        assertFalse(validator.validateRecipe(mistagged, p).isValid());
    }

    @Test
    void timeLimitAndDislikesAreChecked() {
        UserProfile p = TestData.profile(List.of(), List.of(), List.of("mushrooms"), 30);
        ValidationResult result = validator.validateRecipe(repo.findById("r06"), p); // 45 min mushroom risotto
        assertEquals(2, result.getViolations().size());
    }

    @Test
    void planMustHaveFiveToSevenDays() {
        MealPlan plan = new MealPlan();
        plan.addMeal(new PlannedMeal("Monday", repo.findById("r09"), 2));
        ValidationResult result = validator.validatePlan(plan, TestData.noRestrictions());
        assertFalse(result.isValid());
        assertTrue(result.getViolations().get(0).contains("5 to 7"));
        assertFalse(validator.validatePlan(new MealPlan(), TestData.noRestrictions()).isValid());
    }

    @Test
    void planViolationsNameTheDay() {
        MealPlan plan = new MealPlan();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        for (String d : days) {
            plan.addMeal(new PlannedMeal(d, repo.findById(d.equals("Friday") ? "r04" : "r09"), 2));
        }
        UserProfile vegetarian = TestData.profile(List.of("vegetarian"), List.of(), List.of(), 60);
        ValidationResult result = validator.validatePlan(plan, vegetarian);
        assertEquals(1, result.getViolations().size());
        assertTrue(result.getViolations().get(0).startsWith("Friday"));
    }
}
