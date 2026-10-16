package com.smartplate.ai;

/**
 * The plain-text protocol between MealPlanningAgent and any AIModel.
 *
 * <p>The agent writes one "KEY: value" line per field, then a CANDIDATES list with one
 * recipe per line, and asks for a fixed RESPONSE_FORMAT. Because the format is plain text,
 * a real LLM can follow it and MockAIModel can parse it.
 */
public final class PromptFormat {

    public static final String TASK = "TASK";
    public static final String REQUEST = "REQUEST";
    public static final String COUNT = "COUNT";
    public static final String DAYS = "DAYS";
    public static final String TARGET_DAY = "TARGET_DAY";
    public static final String CURRENT_PLAN = "CURRENT_PLAN";
    public static final String DIET = "DIET";
    public static final String AVOID = "AVOID";
    public static final String MAX_TIME = "MAX_TIME";
    public static final String EXCLUDE = "EXCLUDE";
    public static final String PREVIOUS_VIOLATIONS = "PREVIOUS_VIOLATIONS";
    public static final String CANDIDATES = "CANDIDATES";
    public static final String RESPONSE_FORMAT = "RESPONSE_FORMAT";

    public static final String TASK_RECOMMEND = "RECOMMEND";
    public static final String TASK_PLAN = "PLAN";
    public static final String TASK_EDIT = "EDIT";

    /** Response keys. */
    public static final String RECIPES = "RECIPES";
    public static final String DAY = "DAY";
    public static final String RECIPE = "RECIPE";

    private PromptFormat() {
    }

    /** Returns the value of a "KEY: value" line, or "" if the key is absent. */
    public static String field(String text, String key) {
        for (String line : text.split("\n")) {
            String clean = line.replace("*", "").replace("`", "").trim();
            if (clean.regionMatches(true, 0, key + ":", 0, key.length() + 1)) {
                return clean.substring(key.length() + 1).trim();
            }
        }
        return "";
    }
}
