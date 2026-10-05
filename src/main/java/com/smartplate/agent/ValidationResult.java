package com.smartplate.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a ConstraintValidator check. Carries the violation messages so the agent can
 * send them back to the AI model when it revises (report section 3, "Reading the diagram").
 */
public class ValidationResult {

    private final List<String> violations = new ArrayList<>();

    public static ValidationResult ok() {
        return new ValidationResult();
    }

    public void addViolation(String message) {
        violations.add(message);
    }

    public void merge(ValidationResult other) {
        violations.addAll(other.violations);
    }

    public boolean isValid() {
        return violations.isEmpty();
    }

    public List<String> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    @Override
    public String toString() {
        return isValid() ? "valid" : String.join("; ", violations);
    }
}
