package com.smartplate.domain;

/**
 * Unchecked exception for every expected, user-facing error in SmartPlate
 * (invalid input, item not found, no valid plan, AI failure, ...).
 *
 * <p>The report's sequence diagrams show many "error" return arrows. In code each
 * of those is a SmartPlateException whose message is safe to show to the user.
 */
public class SmartPlateException extends RuntimeException {

    public SmartPlateException(String message) {
        super(message);
    }

    public SmartPlateException(String message, Throwable cause) {
        super(message, cause);
    }
}
