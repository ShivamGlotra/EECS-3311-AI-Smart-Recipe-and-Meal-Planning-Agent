package com.smartplate.command;

import com.smartplate.domain.SmartPlateException;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Command pattern - Invoker. Runs commands and keeps the undo stack.
 * (The class diagram shows {@code Stack<Command>}; ArrayDeque is Java's recommended stack.)
 */
public class CommandHistory {

    private final Deque<Command> undoStack = new ArrayDeque<>();

    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
    }

    /** Undoes the most recent command. */
    public Command undo() {
        if (!canUndo()) {
            throw new SmartPlateException("Nothing to undo.");
        }
        Command command = undoStack.pop();
        command.undo();
        return command;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** Forgets all commands (used when a new plan replaces the old one). */
    public void clear() {
        undoStack.clear();
    }

    public int size() {
        return undoStack.size();
    }
}
