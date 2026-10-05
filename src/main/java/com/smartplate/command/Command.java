package com.smartplate.command;

/** Command pattern - Command interface. A reversible change to the meal plan. */
public interface Command {

    void execute();

    void undo();

    /** Human-readable description, e.g. "Tuesday: Beef Tacos -> Veggie Stir-Fry". */
    String getDescription();
}
