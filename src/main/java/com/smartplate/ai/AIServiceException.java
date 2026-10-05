package com.smartplate.ai;

import com.smartplate.domain.SmartPlateException;

/** The AI service is unavailable, refused, or returned an unusable answer. */
public class AIServiceException extends SmartPlateException {

    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
