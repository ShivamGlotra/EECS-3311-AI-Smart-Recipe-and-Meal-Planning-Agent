package com.smartplate.ai;

import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;

import java.util.stream.Collectors;

/**
 * Adapter pattern - Adapter.
 *
 * <p>Implements the AIModel interface the agent expects and translates each
 * {@code generate(prompt)} call into the request AIClient expects (MessageCreateParams),
 * then translates the provider's response (Message) back into plain text. Provider
 * errors become AIServiceException, so the agent never sees provider-specific types.
 */
public class AIAdapter implements AIModel {

    /** Tells the LLM how to behave; the per-request details are in the prompt. */
    static final String SYSTEM_PROMPT = "You are the recipe-selection component of SmartPlate, a meal-planning "
            + "assistant. You only choose recipes from the CANDIDATES list in the prompt, using their ids "
            + "exactly as written. Never invent recipes or ids. Respect the user's diet, allergies, "
            + "disliked ingredients and time limit, and the user's request. If PREVIOUS_VIOLATIONS are "
            + "listed, fix them. Reply only in the RESPONSE_FORMAT given, with no other text.";

    private static final long MAX_TOKENS = 8000L;
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";

    private final AIClient client;

    public AIAdapter(AIClient client) {
        this.client = client;
    }

    @Override
    public String generate(String prompt) {
        Message response;
        try {
            response = client.sendRequest(buildRequest(prompt));
        } catch (AIServiceException e) {
            throw e;
        } catch (UnauthorizedException e) {
            throw new AIServiceException("The AI service rejected the API key.", e);
        } catch (RateLimitException e) {
            throw new AIServiceException("The AI service is busy (rate limited). Try again shortly.", e);
        } catch (AnthropicServiceException e) {
            throw new AIServiceException("The AI service returned an error (HTTP " + e.statusCode() + ").", e);
        } catch (AnthropicIoException e) {
            throw new AIServiceException("Could not reach the AI service: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new AIServiceException("The AI service failed: " + e.getMessage(), e);
        }
        return extractText(response);
    }

    /** Translates a plain prompt into the provider's request format. */
    MessageCreateParams buildRequest(String prompt) {
        return MessageCreateParams.builder()
                .model(client.getModel())
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM_PROMPT)
                .addUserMessage(prompt)
                // choosing among a short list is a simple task, so low effort keeps it fast and cheap
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                // if a safety classifier declines, let the server retry on a fallback model
                .putAdditionalHeader("anthropic-beta", FALLBACK_BETA)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();
    }

    /** Translates the provider's response back into plain text. */
    static String extractText(Message response) {
        if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            throw new AIServiceException("The AI model declined this request.");
        }
        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining("\n"))
                .trim();
        if (text.isEmpty()) {
            throw new AIServiceException("The AI model returned an empty answer.");
        }
        return text;
    }

    @Override
    public String getName() {
        return "Claude (" + client.getModel() + ")";
    }
}
