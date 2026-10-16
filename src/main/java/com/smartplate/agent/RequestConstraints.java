package com.smartplate.agent;

import com.smartplate.domain.SmartPlateException;
import com.smartplate.domain.UserProfile;
import com.smartplate.plan.MealPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hard constraints stated inside one natural-language request, for example
 * "plan 5 vegetarian dinners, avoid mushrooms, under 30 minutes, for 4 people".
 *
 * <p>The AI model also reads the request, but these few constraints are extracted with plain
 * rules so that ConstraintValidator can enforce them deterministically for this request only
 * (the stored profile is not changed).
 */
public final class RequestConstraints {

    private static final Pattern DAYS = Pattern.compile("\\b(\\d+)\\s*-?\\s*(?:days?|dinners?|nights?)\\b");
    private static final Pattern COUNT = Pattern.compile(
            "\\b(\\d+)\\s+(?:[a-z-]+\\s+)?(?:recipes?|ideas?|options?|suggestions?|dishes|dinners?|meals?)\\b");
    private static final Pattern TIME = Pattern.compile(
            "\\b(?:under|below|less than|within|max(?:imum)?|at most|no more than|in)\\s+(\\d+)\\s*(?:min|mins|minutes)\\b");
    private static final Pattern SERVINGS = Pattern.compile("\\bfor\\s+(\\d+)\\s*(?:people|persons|servings|guests)\\b");
    private static final Pattern AVOID = Pattern.compile("\\b(?:avoid|avoiding|no|without|skip)\\s+([a-z][a-z-]*)");
    private static final Set<String> NOT_INGREDIENTS = Set.of(
            "more", "longer", "than", "repeats", "repeat", "repeated", "cooking", "oven", "time", "fuss",
            "need", "thanks", "the", "a", "an", "any", "spicy", "leftovers");

    private final Integer days;
    private final Integer count;
    private final Integer maxTime;
    private final Integer servings;
    private final List<String> avoid;
    private final List<String> diets;

    private RequestConstraints(Integer days, Integer count, Integer maxTime, Integer servings, List<String> avoid,
                               List<String> diets) {
        this.days = days;
        this.count = count;
        this.maxTime = maxTime;
        this.servings = servings;
        this.avoid = Collections.unmodifiableList(avoid);
        this.diets = Collections.unmodifiableList(diets);
    }

    public static RequestConstraints parse(String request) {
        String text = request == null ? "" : request.toLowerCase(Locale.ROOT);
        Integer days = null;
        if (text.contains("weekday") || text.contains("work week") || text.contains("workweek")) {
            days = 5;
        }
        Matcher m = DAYS.matcher(text);
        if (m.find()) {
            days = Integer.parseInt(m.group(1));
        }
        Integer count = null;
        m = COUNT.matcher(text);
        if (m.find()) {
            count = Integer.parseInt(m.group(1));
        }
        Integer maxTime = null;
        m = TIME.matcher(text);
        if (m.find()) {
            maxTime = Integer.parseInt(m.group(1));
        }
        Integer servings = null;
        m = SERVINGS.matcher(text);
        if (m.find()) {
            servings = Integer.parseInt(m.group(1));
        }
        List<String> avoid = new ArrayList<>();
        m = AVOID.matcher(text);
        while (m.find()) {
            String word = m.group(1);
            if (!NOT_INGREDIENTS.contains(word) && !avoid.contains(word)) {
                avoid.add(word);
            }
        }
        List<String> diets = new ArrayList<>();
        for (String diet : UserProfile.SUPPORTED_DIETS) {
            if (Pattern.compile("(?<![a-z-])" + Pattern.quote(diet) + "(?![a-z-])").matcher(text).find()) {
                diets.add(diet);
            }
        }
        return new RequestConstraints(days, count, maxTime, servings, avoid, diets);
    }

    /** Number of plan days: the request's number, or 7. Must be 5 to 7. */
    public int planDays() {
        int n = days == null ? MealPlan.MAX_DAYS : days;
        if (n < MealPlan.MIN_DAYS || n > MealPlan.MAX_DAYS) {
            throw new SmartPlateException("A plan must have " + MealPlan.MIN_DAYS + " to " + MealPlan.MAX_DAYS
                    + " days (one dinner per day); the request asked for " + n + ".");
        }
        return n;
    }

    /** Number of recommendations: the request's number, or the default. */
    public int recommendationCount(int defaultCount) {
        return count == null || count < 1 ? defaultCount : count;
    }

    public int servings(int defaultServings) {
        return servings == null || servings < 1 ? defaultServings : servings;
    }

    public Integer getMaxTime() {
        return maxTime;
    }

    public List<String> getAvoid() {
        return avoid;
    }

    /** Dietary preferences named in the request, e.g. "vegetarian". */
    public List<String> getDiets() {
        return diets;
    }

    /** The profile plus this request's diets, extra dislikes and time limit. */
    public UserProfile applyTo(UserProfile profile) {
        return profile.withRequestConstraints(diets, avoid, maxTime);
    }

    @Override
    public String toString() {
        List<String> parts = new ArrayList<>();
        if (days != null) {
            parts.add(days + " days");
        }
        if (count != null) {
            parts.add(count + " items");
        }
        if (maxTime != null) {
            parts.add("max " + maxTime + " min");
        }
        if (servings != null) {
            parts.add(servings + " servings");
        }
        if (!diets.isEmpty()) {
            parts.add(String.join(", ", diets));
        }
        if (!avoid.isEmpty()) {
            parts.add("avoid " + String.join(", ", avoid));
        }
        return parts.isEmpty() ? "none" : String.join("; ", parts);
    }
}
