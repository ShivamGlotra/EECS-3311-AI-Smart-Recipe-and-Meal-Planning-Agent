package com.smartplate.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartplate.domain.PantryAction;
import com.smartplate.facade.SmartRecipeFacade;
import com.smartplate.plan.MealPlan;
import com.smartplate.service.SampleData;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs GeminiAdapter against a small fake Gemini server, so no key or network is needed. */
class GeminiAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final List<HttpExchange> exchanges = new ArrayList<>();
    private final List<JsonNode> requests = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Starts a fake Gemini API; the handler maps the request JSON to (status, response body). */
    private GeminiAdapter adapterFor(Function<JsonNode, Object[]> handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> respond(exchange, handler));
        server.start();
        return new GeminiAdapter("test-key", "test-model",
                "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(10));
    }

    private void respond(HttpExchange exchange, Function<JsonNode, Object[]> handler) throws IOException {
        JsonNode request = JSON.readTree(exchange.getRequestBody().readAllBytes());
        synchronized (requests) {
            exchanges.add(exchange);
            requests.add(request);
        }
        Object[] reply = handler.apply(request);
        byte[] bytes = ((String) reply[1]).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders((Integer) reply[0], bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** A generateContent reply; parts given as "thought:" are marked as thinking. */
    private static String reply(String... parts) {
        ObjectNode body = JSON.createObjectNode();
        ObjectNode candidate = body.putArray("candidates").addObject();
        ArrayNode array = candidate.putObject("content").put("role", "model").putArray("parts");
        for (String part : parts) {
            ObjectNode node = array.addObject();
            if (part.startsWith("thought:")) {
                node.put("text", part.substring("thought:".length())).put("thought", true);
            } else {
                node.put("text", part);
            }
        }
        candidate.put("finishReason", "STOP");
        return body.toString();
    }

    private static String error(int code, String message) {
        return "{\"error\":{\"code\":" + code + ",\"message\":\"" + message + "\",\"status\":\"X\"}}";
    }

    @Test
    void sendsAGenerateContentRequestAndReturnsTheAnswer() throws IOException {
        GeminiAdapter adapter = adapterFor(request ->
                new Object[] {200, reply("thought:r04 fits best", "RECIPES: r04,", " r09")});

        assertEquals("RECIPES: r04, r09", adapter.generate("TASK: RECOMMEND"));

        HttpExchange exchange = exchanges.get(0);
        assertEquals("/v1beta/models/test-model:generateContent", exchange.getRequestURI().getPath());
        assertEquals(null, exchange.getRequestURI().getQuery(), "the key must not be in the URL");
        assertEquals("test-key", exchange.getRequestHeaders().getFirst("x-goog-api-key"));
        JsonNode request = requests.get(0);
        assertEquals(AIAdapter.SYSTEM_PROMPT,
                request.path("systemInstruction").path("parts").get(0).path("text").asText());
        assertEquals("user", request.path("contents").get(0).path("role").asText());
        assertEquals("TASK: RECOMMEND", request.path("contents").get(0).path("parts").get(0).path("text").asText());
    }

    @Test
    void withoutAKeyItFailsBeforeAnyNetworkCall() {
        GeminiAdapter adapter = new GeminiAdapter(" ", null);
        assertFalse(adapter.isConfigured());
        assertEquals(GeminiAdapter.DEFAULT_MODEL, adapter.getModel());
        AIServiceException e = assertThrows(AIServiceException.class, () -> adapter.generate("x"));
        assertTrue(e.getMessage().contains(GeminiAdapter.API_KEY_VARIABLE));
    }

    @Test
    void apiErrorsBecomeClearServiceErrors() throws IOException {
        List<Object[]> replies = new ArrayList<>(List.of(
                new Object[] {400, error(400, "API key not valid. Please pass a valid API key.")},
                new Object[] {404, error(404, "models/test-model is not found")},
                new Object[] {429, error(429, "Resource has been exhausted")},
                new Object[] {500, error(500, "Internal error")},
                new Object[] {200, "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"},
                new Object[] {200, "{\"candidates\":[{\"content\":{\"parts\":[]},\"finishReason\":\"MAX_TOKENS\"}]}"},
                new Object[] {200, "not json"}));
        GeminiAdapter adapter = adapterFor(request -> replies.remove(0));

        assertTrue(message(adapter).contains("rejected the API key"));
        assertTrue(message(adapter).contains("GEMINI_MODEL"));
        assertTrue(message(adapter).contains("rate limited"));
        assertTrue(message(adapter).contains("HTTP 500"));
        assertTrue(message(adapter).contains("declined"));
        assertTrue(message(adapter).contains("MAX_TOKENS"));
        assertTrue(message(adapter).contains("not valid JSON"));
    }

    private static String message(GeminiAdapter adapter) {
        return assertThrows(AIServiceException.class, () -> adapter.generate("x")).getMessage();
    }

    @Test
    void theAgentWorksUnchangedThroughTheGeminiAdapter() throws IOException {
        // the fake Gemini answers each prompt the way the offline mock would
        MockAIModel brain = new MockAIModel();
        GeminiAdapter adapter = adapterFor(request -> new Object[] {200, reply(
                brain.generate(request.path("contents").get(0).path("parts").get(0).path("text").asText()))});

        SmartRecipeFacade facade = SmartRecipeFacade.create(adapter);
        SampleData.pantry().forEach(item -> facade.managePantry(PantryAction.ADD, item));
        assertEquals("Gemini (test-model)", facade.getAIModelName());

        assertFalse(facade.recommendRecipes("something quick with rice").isEmpty());
        MealPlan plan = facade.createMealPlan("plan my week");
        assertTrue(plan.getMeals().size() >= MealPlan.MIN_DAYS);
    }
}
