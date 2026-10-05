package com.smartplate.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

/**
 * Adapter pattern - Adaptee.
 *
 * <p>The provider-specific client. It speaks the provider's own request/response types
 * (here the Anthropic Messages API through the official Java SDK), which are different
 * from the simple {@code generate(String) : String} interface the agent wants.
 * AIAdapter translates between the two.
 *
 * <p>Configuration comes from the environment, never from source code:
 * <ul>
 *   <li>{@code ANTHROPIC_API_KEY} - the API key (required for the real AI)</li>
 *   <li>{@code SMARTPLATE_AI_MODEL} - model id (default {@value #DEFAULT_MODEL})</li>
 * </ul>
 */
public class AIClient {

    public static final String DEFAULT_MODEL = "claude-opus-5-5";
    public static final String API_KEY_VARIABLE = "ANTHROPIC_API_KEY";
    public static final String MODEL_VARIABLE = "SMARTPLATE_AI_MODEL";

    private final String apiKey;
    private final String model;
    private AnthropicClient client; // created on first use

    public AIClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    /** Reads the API key and model from environment variables. */
    public static AIClient fromEnvironment() {
        return new AIClient(System.getenv(API_KEY_VARIABLE), System.getenv(MODEL_VARIABLE));
    }

    /** True when an API key is configured. */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    /** Sends a provider-specific request and returns the provider-specific response. */
    public Message sendRequest(MessageCreateParams request) {
        if (!isConfigured()) {
            throw new AIServiceException("No API key: set " + API_KEY_VARIABLE + " to use the real AI model.");
        }
        if (client == null) {
            client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        }
        return client.messages().create(request);
    }
}
