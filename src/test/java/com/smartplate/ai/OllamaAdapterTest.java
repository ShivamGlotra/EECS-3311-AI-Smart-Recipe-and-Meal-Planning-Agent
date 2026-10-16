package com.smartplate.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs OllamaAdapter against a small fake Ollama server, so no real model is needed. */
class OllamaAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final List<JsonNode> requests = new ArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Starts a fake /api/chat; the handler maps the request JSON to (status, response body). */
    private OllamaAdapter adapterFor(Function<JsonNode, Object[]> handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> respond(exchange, handler));
        server.start();
        return new OllamaAdapter("http://127.0.0.1:" + server.getAddress().getPort() + "/", "test-model",
                Duration.ofSeconds(10));
    }

    private void respond(HttpExchange exchange, Function<JsonNode, Object[]> handler) throws IOException {
        JsonNode request = JSON.readTree(exchange.getRequestBody().readAllBytes());
        synchronized (requests) {
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

    private static String chatReply(String content) {
        var body = JSON.createObjectNode();
        body.put("model", "test-model");
        body.putObject("message").put("role", "assistant").put("content", content);
        body.put("done", true);
        return body.toString();
    }

    @Test
    void sendsAChatRequestAndReturnsTheAnswerWithoutThinking() throws IOException {
        OllamaAdapter adapter = adapterFor(request ->
                new Object[] {200, chatReply("<think>\nr04 looks good\n</think>\n\nRECIPES: r04, r09")});

        assertEquals("RECIPES: r04, r09", adapter.generate("TASK: RECOMMEND"));

        JsonNode request = requests.get(0);
        assertEquals("test-model", request.path("model").asText());
        assertFalse(request.path("stream").asBoolean(true));
        assertEquals("system", request.path("messages").get(0).path("role").asText());
        assertEquals(AIAdapter.SYSTEM_PROMPT, request.path("messages").get(0).path("content").asText());
        assertEquals("user", request.path("messages").get(1).path("role").asText());
        assertEquals("TASK: RECOMMEND", request.path("messages").get(1).path("content").asText());
    }

    @Test
    void aMissingModelTellsTheUserHowToPullIt() throws IOException {
        OllamaAdapter adapter = adapterFor(request ->
                new Object[] {404, "{\"error\":\"model 'test-model' not found\"}"});
        AIServiceException e = assertThrows(AIServiceException.class, () -> adapter.generate("x"));
        assertTrue(e.getMessage().contains("ollama pull test-model"), e.getMessage());
    }

    @Test
    void otherHttpErrorsAndEmptyAnswersBecomeServiceErrors() throws IOException {
        List<Object[]> replies = new ArrayList<>(List.of(
                new Object[] {500, "{\"error\":\"out of memory\"}"},
                new Object[] {200, chatReply("<think>hmm</think>   ")},
                new Object[] {200, "not json"}));
        OllamaAdapter adapter = adapterFor(request -> replies.remove(0));

        AIServiceException serverError = assertThrows(AIServiceException.class, () -> adapter.generate("x"));
        assertTrue(serverError.getMessage().contains("HTTP 500"));
        assertTrue(serverError.getMessage().contains("out of memory"));
        assertTrue(assertThrows(AIServiceException.class, () -> adapter.generate("x"))
                .getMessage().contains("empty"));
        assertTrue(assertThrows(AIServiceException.class, () -> adapter.generate("x"))
                .getMessage().contains("not valid JSON"));
    }

    @Test
    void anUnreachableServerIsAClearError() throws IOException {
        int freePort;
        try (ServerSocket socket = new ServerSocket(0)) {
            freePort = socket.getLocalPort();
        }
        OllamaAdapter adapter = new OllamaAdapter("http://127.0.0.1:" + freePort, "test-model", Duration.ofSeconds(5));
        AIServiceException e = assertThrows(AIServiceException.class, () -> adapter.generate("x"));
        assertTrue(e.getMessage().contains("Could not reach Ollama"), e.getMessage());
    }

    @Test
    void theAgentWorksUnchangedThroughTheOllamaAdapter() throws IOException {
        // the fake "model" answers each prompt the way the offline mock would, wrapped in thinking tags
        MockAIModel brain = new MockAIModel();
        OllamaAdapter adapter = adapterFor(request -> new Object[] {200, chatReply(
                "<think>checking the candidates</think>\n"
                        + brain.generate(request.path("messages").get(1).path("content").asText()))});

        SmartRecipeFacade facade = SmartRecipeFacade.create(adapter);
        SampleData.pantry().forEach(item -> facade.managePantry(PantryAction.ADD, item));
        assertEquals("Ollama (test-model)", facade.getAIModelName());

        assertFalse(facade.recommendRecipes("something quick with rice").isEmpty());
        MealPlan plan = facade.createMealPlan("plan my week");
        assertTrue(plan.getMeals().size() >= MealPlan.MIN_DAYS);
        assertFalse(requests.isEmpty());
    }
}
