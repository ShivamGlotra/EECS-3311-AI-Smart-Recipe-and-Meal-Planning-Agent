package com.smartplate.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartplate.domain.PantryAction;
import com.smartplate.domain.PantryItem;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.RecipeIngredient;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.domain.UserProfile;
import com.smartplate.facade.SmartRecipeFacade;
import com.smartplate.plan.MealPlan;
import com.smartplate.plan.PlannedMeal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Machine-readable interface for automated agent testing (Stage 3, KUMA).
 *
 * <p>Reads one JSON object per line and writes one JSON object per line. Like the CLI and the
 * GUI it calls only SmartRecipeFacade, so the tests exercise exactly the code the user runs.
 * The session keeps its state (profile, pantry, plan, undo history) between lines, so a test
 * can create a plan and then edit or undo it.
 *
 * <pre>
 *   {"action":"profile","diets":["vegetarian"],"allergies":["peanuts"],"dislikes":[],"maxTime":30,"servings":2}
 *   {"action":"pantry","op":"add","name":"rice","quantity":500,"unit":"g"}
 *   {"action":"strategy","name":"fastest"}
 *   {"action":"recommend","request":"3 quick dinners using rice"}
 *   {"action":"plan","request":"plan 5 dinners, avoid mushrooms"}
 *   {"action":"edit","request":"swap Tuesday for something with spinach"}
 *   {"action":"undo"}
 *   {"action":"state"}      the current profile and plan
 *   {"action":"catalog"}    every recipe with its ingredients and categories
 * </pre>
 * Every reply has "ok" (and "error" when ok is false), the AI model's name, the time taken and,
 * after an AI request, the agent trace.
 */
public class MealPlannerJsonSession {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final SmartRecipeFacade facade;
    private final InputStream in;
    private final PrintStream out;

    public MealPlannerJsonSession(SmartRecipeFacade facade, InputStream in, PrintStream out) {
        this.facade = facade;
        this.in = in;
        this.out = out;
    }

    /** Answers each input line until the input ends. */
    public void runLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    out.println(handle(line));
                    out.flush();
                }
            }
        } catch (IOException e) {
            out.println(error("input", "Could not read input: " + e.getMessage()));
        }
    }

    /** Handles one request line and returns the reply (never throws). */
    public String handle(String line) {
        JsonNode request;
        try {
            request = JSON.readTree(line);
        } catch (IOException e) {
            return error("", "The input is not valid JSON.").toString();
        }
        if (request == null || !request.isObject()) {
            return error("", "The input must be a JSON object.").toString();
        }
        String action = text(request, "action").toLowerCase(Locale.ROOT);
        long start = System.nanoTime();
        ObjectNode reply;
        try {
            reply = dispatch(action, request);
            reply.put("ok", true);
        } catch (SmartPlateException | IllegalArgumentException e) {
            reply = error(action, e.getMessage());
        } catch (RuntimeException e) {
            // an unexpected bug must still produce a reply the test harness can read
            reply = error(action, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        reply.put("action", action);
        reply.put("model", facade.getAIModelName());
        reply.put("elapsedMs", (System.nanoTime() - start) / 1_000_000);
        if (isAgentAction(action)) {
            ArrayNode trace = reply.putArray("trace");
            facade.getLastAgentTrace().forEach(trace::add);
        }
        return reply.toString();
    }

    private ObjectNode dispatch(String action, JsonNode request) {
        ObjectNode reply = JSON.createObjectNode();
        switch (action) {
            case "profile":
                facade.setupProfile(new UserProfile(list(request, "diets"), list(request, "allergies"),
                        list(request, "dislikes"), request.path("maxTime").asInt(UserProfile.DEFAULT_MAX_COOKING_TIME),
                        request.path("servings").asInt(UserProfile.DEFAULT_SERVINGS)));
                reply.set("profile", profile(facade.getProfile()));
                return reply;
            case "pantry":
                PantryAction op = PantryAction.valueOf(text(request, "op").toUpperCase(Locale.ROOT));
                facade.managePantry(op, new PantryItem(text(request, "name"),
                        request.path("quantity").asDouble(1), text(request, "unit")));
                return reply;
            case "strategy":
                facade.setStrategy(text(request, "name"));
                reply.put("strategy", facade.getStrategyName());
                return reply;
            case "model":
                facade.setAIProvider(text(request, "provider"));
                return reply;
            case "recommend":
                reply.set("recipes", recipes(facade.recommendRecipes(text(request, "request"))));
                return reply;
            case "plan":
                reply.set("plan", plan(facade.createMealPlan(text(request, "request"))));
                return reply;
            case "edit":
                reply.set("plan", plan(facade.editMealPlan(text(request, "request"))));
                return reply;
            case "undo":
                reply.set("plan", plan(facade.undoLastChange()));
                return reply;
            case "state":
                reply.set("profile", profile(facade.getProfile()));
                reply.set("plan", plan(facade.getCurrentPlan()));
                reply.put("canUndo", facade.canUndo());
                return reply;
            case "catalog":
                reply.set("recipes", recipes(facade.getAllRecipes()));
                return reply;
            default:
                throw new SmartPlateException("Unknown action '" + action + "'.");
        }
    }

    private static boolean isAgentAction(String action) {
        return action.equals("recommend") || action.equals("plan") || action.equals("edit");
    }

    private static ObjectNode error(String action, String message) {
        ObjectNode reply = JSON.createObjectNode();
        reply.put("ok", false);
        reply.put("action", action);
        reply.put("error", message);
        return reply;
    }

    private static ObjectNode profile(UserProfile profile) {
        ObjectNode node = JSON.createObjectNode();
        profile.getDietaryPreferences().forEach(node.putArray("diets")::add);
        profile.getAllergies().forEach(node.putArray("allergies")::add);
        profile.getDislikedIngredients().forEach(node.putArray("dislikes")::add);
        node.put("maxTime", profile.getMaxCookingTime());
        node.put("servings", profile.getDefaultServings());
        return node;
    }

    private static ArrayNode recipes(List<Recipe> recipes) {
        ArrayNode array = JSON.createArrayNode();
        for (Recipe recipe : recipes) {
            array.add(recipe(recipe));
        }
        return array;
    }

    private static ObjectNode recipe(Recipe recipe) {
        ObjectNode node = JSON.createObjectNode();
        node.put("id", recipe.getId());
        node.put("name", recipe.getName());
        node.put("time", recipe.getCookingTime());
        recipe.getDietaryTags().forEach(node.putArray("tags")::add);
        ArrayNode ingredients = node.putArray("ingredients");
        for (RecipeIngredient ri : recipe.getIngredients()) {
            ObjectNode ingredient = ingredients.addObject();
            ingredient.put("name", ri.getName());
            ingredient.put("category", ri.getIngredient().getCategory());
        }
        return node;
    }

    /** The plan as a list of days; an empty list when there is no plan. */
    private static ArrayNode plan(MealPlan plan) {
        ArrayNode array = JSON.createArrayNode();
        if (plan != null) {
            for (PlannedMeal meal : plan.getMeals()) {
                ObjectNode node = recipe(meal.getRecipe());
                node.put("day", meal.getDay());
                node.put("servings", meal.getServings());
                array.add(node);
            }
        }
        return array;
    }

    private static String text(JsonNode request, String field) {
        JsonNode value = request.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private static List<String> list(JsonNode request, String field) {
        List<String> values = new ArrayList<>();
        JsonNode node = request.path(field);
        if (node.isArray()) {
            node.forEach(v -> values.add(v.asText()));
        } else if (!node.isMissingNode() && !node.isNull()) {
            for (String part : node.asText().split(",")) {
                values.add(part);
            }
        }
        values.removeIf(String::isBlank);
        return values;
    }
}
