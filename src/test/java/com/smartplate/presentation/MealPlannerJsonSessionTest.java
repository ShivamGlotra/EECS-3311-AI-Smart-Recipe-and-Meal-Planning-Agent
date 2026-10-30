package com.smartplate.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartplate.ai.MockAIModel;
import com.smartplate.facade.SmartRecipeFacade;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MealPlannerJsonSessionTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Runs the lines through one session and returns one parsed reply per line. */
    private List<JsonNode> run(SmartRecipeFacade facade, String... lines) throws Exception {
        String input = String.join("\n", lines) + "\n";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new MealPlannerJsonSession(facade, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(bytes, true, StandardCharsets.UTF_8)).runLoop();
        List<JsonNode> replies = new ArrayList<>();
        for (String line : bytes.toString(StandardCharsets.UTF_8).split("\n")) {
            replies.add(JSON.readTree(line));
        }
        return replies;
    }

    private List<JsonNode> run(String... lines) throws Exception {
        return run(SmartRecipeFacade.create(new MockAIModel()), lines);
    }

    @Test
    void planEditUndoKeepsStateBetweenLines() throws Exception {
        List<JsonNode> r = run(
                "{\"action\":\"profile\",\"diets\":[\"vegetarian\"],\"allergies\":[\"peanuts\"],\"maxTime\":40}",
                "{\"action\":\"plan\",\"request\":\"plan 5 dinners\"}",
                "{\"action\":\"edit\",\"request\":\"swap Tuesday for something with spinach\"}",
                "{\"action\":\"undo\"}",
                "{\"action\":\"state\"}");
        assertEquals(5, r.size());
        r.forEach(reply -> assertTrue(reply.get("ok").asBoolean(), reply.toString()));
        JsonNode plan = r.get(1).get("plan");
        assertEquals(5, plan.size());
        assertEquals("Monday", plan.get(0).get("day").asText());
        assertTrue(plan.get(0).get("ingredients").get(0).has("category"));
        assertTrue(r.get(1).get("trace").size() > 3, "an AI request returns the agent trace");
        String tuesday = plan.get(1).get("id").asText();
        assertNotEquals(tuesday, r.get(2).get("plan").get(1).get("id").asText());
        assertEquals(tuesday, r.get(3).get("plan").get(1).get("id").asText());
        assertFalse(r.get(4).get("canUndo").asBoolean());
        assertEquals(40, r.get(4).get("profile").get("maxTime").asInt());
    }

    @Test
    void errorsBecomeRepliesAndTheSessionContinues() throws Exception {
        List<JsonNode> r = run(
                "not json",
                "{\"action\":\"plan\",\"request\":\"plan 9 dinners\"}",
                "{\"action\":\"undo\"}",
                "{\"action\":\"profile\",\"allergies\":[\"peanuts\",\"123\"],\"servings\":0}",
                "{\"action\":\"frobnicate\"}",
                "{\"action\":\"catalog\"}");
        assertEquals("The input is not valid JSON.", r.get(0).get("error").asText());
        assertTrue(r.get(1).get("error").asText().contains("5 to 7 days"));
        assertEquals("Nothing to undo.", r.get(2).get("error").asText());
        assertTrue(r.get(3).get("error").asText().contains("Malformed allergy"));
        assertTrue(r.get(4).get("error").asText().contains("Unknown action"));
        for (int i = 0; i < 5; i++) {
            assertFalse(r.get(i).get("ok").asBoolean());
        }
        assertTrue(r.get(5).get("ok").asBoolean());
        assertTrue(r.get(5).get("recipes").size() >= 15);
    }

    @Test
    void anAIServiceFailureIsReportedWithTheTrace() throws Exception {
        List<JsonNode> r = run(SmartRecipeFacade.create(MockAIModel.failing()),
                "{\"action\":\"recommend\",\"request\":\"2 dinners\"}");
        assertFalse(r.get(0).get("ok").asBoolean());
        assertTrue(r.get(0).get("error").asText().contains("unavailable"));
        assertTrue(r.get(0).get("trace").toString().contains("AI service error"));
    }
}
