package com.smartplate.ai;

import java.util.Locale;
import java.util.Map;

/**
 * Chooses which AIModel implementation SmartPlate uses (Adapter pattern - client side).
 *
 * <p>Selected by the environment variable {@code SMARTPLATE_AI_PROVIDER}:
 * <ul>
 *   <li>{@code mock}      - offline MockAIModel</li>
 *   <li>{@code anthropic} - AIAdapter over AIClient (needs ANTHROPIC_API_KEY)</li>
 *   <li>{@code ollama}    - OllamaAdapter, a local model (OLLAMA_MODEL and OLLAMA_URL are optional)</li>
 *   <li>{@code auto} (default) - anthropic if an API key is set, otherwise mock</li>
 * </ul>
 * Ollama is never picked automatically, so a stray local server cannot change behaviour silently.
 */
public final class AIModelFactory {

    public static final String PROVIDER_VARIABLE = "SMARTPLATE_AI_PROVIDER";

    private AIModelFactory() {
    }

    public static AIModel fromEnvironment() {
        return create(System.getenv());
    }

    /** Takes the environment as a map so tests do not depend on the real environment. */
    public static AIModel create(Map<String, String> env) {
        String provider = value(env, PROVIDER_VARIABLE, "auto").toLowerCase(Locale.ROOT);
        switch (provider) {
            case "mock":
                return new MockAIModel();
            case "anthropic":
                return new AIAdapter(new AIClient(
                        env.get(AIClient.API_KEY_VARIABLE), env.get(AIClient.MODEL_VARIABLE)));
            case "ollama":
                return new OllamaAdapter(
                        value(env, "OLLAMA_URL", OllamaAdapter.DEFAULT_BASE_URL),
                        value(env, "OLLAMA_MODEL", OllamaAdapter.DEFAULT_MODEL));
            case "auto":
                AIClient client = new AIClient(env.get(AIClient.API_KEY_VARIABLE), env.get(AIClient.MODEL_VARIABLE));
                return client.isConfigured() ? new AIAdapter(client) : new MockAIModel();
            default:
                throw new IllegalArgumentException("Unknown " + PROVIDER_VARIABLE + " '" + provider
                        + "'. Use mock, anthropic, ollama or auto.");
        }
    }

    private static String value(Map<String, String> env, String key, String fallback) {
        String v = env.get(key);
        return v == null || v.isBlank() ? fallback : v.trim();
    }
}
