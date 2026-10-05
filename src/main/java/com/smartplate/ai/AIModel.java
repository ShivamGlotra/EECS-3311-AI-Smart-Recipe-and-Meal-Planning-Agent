package com.smartplate.ai;

/**
 * Adapter pattern - Target interface.
 *
 * <p>MealPlanningAgent depends only on this interface. It does not know whether the answer
 * comes from a real LLM (AIAdapter -> AIClient) or from the offline MockAIModel.
 */
public interface AIModel {

    /**
     * Sends a prompt and returns the model's text answer.
     *
     * @throws AIServiceException if the AI service is unavailable or fails
     */
    String generate(String prompt);

    /** Short name shown to the user, e.g. "Mock AI (offline)" or "Claude (claude-opus-5-5)". */
    String getName();
}
