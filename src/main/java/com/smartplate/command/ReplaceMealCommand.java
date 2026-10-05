package com.smartplate.command;

import com.smartplate.plan.MealPlan;
import com.smartplate.plan.PlannedMeal;

/**
 * Command pattern - Concrete command. Replaces one day's meal. It stores the old meal, so
 * undo() needs no extra copies of the plan. MealPlan is the Receiver.
 */
public class ReplaceMealCommand implements Command {

    private final MealPlan plan;
    private final String day;
    private final PlannedMeal oldMeal;
    private final PlannedMeal newMeal;

    public ReplaceMealCommand(MealPlan plan, String day, PlannedMeal oldMeal, PlannedMeal newMeal) {
        this.plan = plan;
        this.day = day;
        this.oldMeal = oldMeal;
        this.newMeal = newMeal;
    }

    @Override
    public void execute() {
        plan.replaceMeal(day, newMeal);
    }

    @Override
    public void undo() {
        plan.replaceMeal(day, oldMeal);
    }

    public String getDay() {
        return day;
    }

    public PlannedMeal getOldMeal() {
        return oldMeal;
    }

    public PlannedMeal getNewMeal() {
        return newMeal;
    }

    @Override
    public String getDescription() {
        return day + ": " + oldMeal.getRecipe().getName() + " -> " + newMeal.getRecipe().getName();
    }
}
