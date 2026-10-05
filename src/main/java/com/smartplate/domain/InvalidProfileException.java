package com.smartplate.domain;

import java.util.List;

/** Thrown by SmartRecipeFacade.setupProfile() when UserProfile.validate() reports problems. */
public class InvalidProfileException extends SmartPlateException {

    private final List<String> problems;

    public InvalidProfileException(List<String> problems) {
        super("The profile was not saved:\n- " + String.join("\n- ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() {
        return problems;
    }
}
