package com.smartplate.plan;

import com.smartplate.domain.NutritionInfo;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.SmartPlateException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Composite pattern - Composite. A plan of one dinner per day. getNutrition(),
 * getIngredients() and getCookingTime() combine the results of its PlannedMeal children.
 *
 * <p>MealPlan is also the Receiver of ReplaceMealCommand (Command pattern).
 */
public class MealPlan implements PlanComponent {

    public static final int MIN_DAYS = 5;
    public static final int MAX_DAYS = 7;

    private final List<PlannedMeal> meals = new ArrayList<>();

    /** Adds a meal. A day can only have one meal and a plan has at most 7 days. */
    public void addMeal(PlannedMeal meal) {
        if (getMeal(meal.getDay()) != null) {
            throw new SmartPlateException(meal.getDay() + " already has a meal.");
        }
        if (meals.size() >= MAX_DAYS) {
            throw new SmartPlateException("A plan has at most " + MAX_DAYS + " days.");
        }
        meals.add(meal);
        meals.sort(Comparator.comparingInt(m -> Days.WEEK.indexOf(m.getDay())));
    }

    public void removeMeal(String day) {
        PlannedMeal meal = requireMeal(day);
        meals.remove(meal);
    }

    /** Replaces the meal of an existing day. Used by ReplaceMealCommand.execute()/undo(). */
    public void replaceMeal(String day, PlannedMeal meal) {
        PlannedMeal old = requireMeal(day);
        if (!old.getDay().equals(meal.getDay())) {
            throw new SmartPlateException("The new meal must be for " + old.getDay() + ".");
        }
        meals.set(meals.indexOf(old), meal);
    }

    /** Returns the meal for this day, or null. */
    public PlannedMeal getMeal(String day) {
        String d = Days.normalize(day);
        for (PlannedMeal meal : meals) {
            if (meal.getDay().equals(d)) {
                return meal;
            }
        }
        return null;
    }

    public List<PlannedMeal> getMeals() {
        return Collections.unmodifiableList(meals);
    }

    public int size() {
        return meals.size();
    }

    public boolean isEmpty() {
        return meals.isEmpty();
    }

    @Override
    public NutritionInfo getNutrition() {
        NutritionInfo total = NutritionInfo.ZERO;
        for (PlannedMeal meal : meals) {
            total = total.plus(meal.getNutrition());
        }
        return total;
    }

    @Override
    public List<RecipeIngredient> getIngredients() {
        List<RecipeIngredient> all = new ArrayList<>();
        for (PlannedMeal meal : meals) {
            all.addAll(meal.getIngredients());
        }
        return all;
    }

    @Override
    public int getCookingTime() {
        int total = 0;
        for (PlannedMeal meal : meals) {
            total += meal.getCookingTime();
        }
        return total;
    }

    private PlannedMeal requireMeal(String day) {
        PlannedMeal meal = getMeal(day);
        if (meal == null) {
            throw new SmartPlateException("The plan has no meal on '" + day + "'.");
        }
        return meal;
    }

    @Override
    public String toString() {
        if (meals.isEmpty()) {
            return "(empty plan)";
        }
        StringBuilder sb = new StringBuilder();
        for (PlannedMeal meal : meals) {
            sb.append(meal).append('\n');
        }
        return sb.toString().trim();
    }
}
