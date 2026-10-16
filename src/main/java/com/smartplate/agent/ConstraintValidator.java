package com.smartplate.agent;

import com.smartplate.domain.DietRules;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.UserProfile;
import com.smartplate.plan.Days;
import com.smartplate.plan.MealPlan;
import com.smartplate.plan.PlannedMeal;

import java.util.HashSet;
import java.util.Set;

/**
 * Deterministic checker for every AI answer: "the LLM proposes; deterministic code decides".
 *
 * <p>It inspects each recipe's ingredient list (names and categories) rather than trusting
 * the recipe's tags, so an allergen is caught even if a recipe is mis-tagged.
 */
public class ConstraintValidator {

    /** Checks one recipe against allergies, dislikes, dietary preferences and the time limit. */
    public ValidationResult validateRecipe(Recipe recipe, UserProfile profile) {
        ValidationResult result = new ValidationResult();
        String label = recipe.getName() + " (" + recipe.getId() + ")";
        for (RecipeIngredient ri : recipe.getIngredients()) {
            for (String reason : DietRules.conflicts(ri.getIngredient(), profile)) {
                result.addViolation(label + " " + reason);
            }
            if (!ri.hasValidQuantity()) {
                result.addViolation(label + " has an invalid quantity for " + ri.getName());
            }
        }
        if (recipe.getCookingTime() > profile.getMaxCookingTime()) {
            result.addViolation(label + " takes " + recipe.getCookingTime() + " min (limit "
                    + profile.getMaxCookingTime() + " min)");
        }
        if (recipe.getIngredients().isEmpty()) {
            result.addViolation(label + " has no ingredients");
        }
        return result;
    }

    /**
     * Checks a whole plan: 5 to 7 days, one meal per day, valid servings, and every recipe
     * passes {@link #validateRecipe}.
     */
    public ValidationResult validatePlan(MealPlan plan, UserProfile profile) {
        ValidationResult result = new ValidationResult();
        if (plan == null || plan.isEmpty()) {
            result.addViolation("The plan is empty");
            return result;
        }
        if (plan.size() < MealPlan.MIN_DAYS || plan.size() > MealPlan.MAX_DAYS) {
            result.addViolation("The plan has " + plan.size() + " days; it must have "
                    + MealPlan.MIN_DAYS + " to " + MealPlan.MAX_DAYS);
        }
        Set<String> days = new HashSet<>();
        for (PlannedMeal meal : plan.getMeals()) {
            if (!days.add(meal.getDay()) || !Days.WEEK.contains(meal.getDay())) {
                result.addViolation("More than one meal (or an invalid day) on " + meal.getDay());
            }
            if (meal.getServings() < 1) {
                result.addViolation(meal.getDay() + " has fewer than 1 serving");
            }
            ValidationResult recipeResult = validateRecipe(meal.getRecipe(), profile);
            for (String v : recipeResult.getViolations()) {
                result.addViolation(meal.getDay() + ": " + v);
            }
        }
        return result;
    }
}
