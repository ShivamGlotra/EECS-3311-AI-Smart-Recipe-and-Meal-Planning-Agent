package com.smartplate.ai;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIModelFactoryTest {

    private static Map<String, String> env(String... keysAndValues) {
        Map<String, String> env = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            env.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return env;
    }

    @Test
    void autoIsTheDefaultAndFallsBackToTheMockWithoutAKey() {
        assertInstanceOf(MockAIModel.class, AIModelFactory.create(env()));
        assertInstanceOf(MockAIModel.class, AIModelFactory.create(env(AIModelFactory.PROVIDER_VARIABLE, " ")));
    }

    @Test
    void autoUsesClaudeWhenAnApiKeyIsSet() {
        AIModel model = AIModelFactory.create(env(AIClient.API_KEY_VARIABLE, "test-key"));
        assertInstanceOf(AIAdapter.class, model);
    }

    @Test
    void autoNeverPicksOllamaOnItsOwn() {
        AIModel model = AIModelFactory.create(env("OLLAMA_MODEL", "llama3.2"));
        assertInstanceOf(MockAIModel.class, model);
    }

    @Test
    void explicitProvidersAreHonoured() {
        assertInstanceOf(MockAIModel.class,
                AIModelFactory.create(env(AIModelFactory.PROVIDER_VARIABLE, "mock", AIClient.API_KEY_VARIABLE, "k")));
        AIModel claude = AIModelFactory.create(env(AIModelFactory.PROVIDER_VARIABLE, "anthropic",
                AIClient.API_KEY_VARIABLE, "k", AIClient.MODEL_VARIABLE, "claude-test-model"));
        assertInstanceOf(AIAdapter.class, claude);
        assertEquals("Claude (claude-test-model)", claude.getName());
    }

    @Test
    void ollamaUsesDefaultsUnlessOverridden() {
        OllamaAdapter defaults = (OllamaAdapter) AIModelFactory.create(env(AIModelFactory.PROVIDER_VARIABLE, "ollama"));
        assertEquals(OllamaAdapter.DEFAULT_BASE_URL, defaults.getBaseUrl());
        assertEquals(OllamaAdapter.DEFAULT_MODEL, defaults.getModel());

        OllamaAdapter custom = (OllamaAdapter) AIModelFactory.create(env(AIModelFactory.PROVIDER_VARIABLE, "OLLAMA",
                "OLLAMA_URL", "http://gpu-box:11434/", "OLLAMA_MODEL", " qwen2.5:7b "));
        assertEquals("http://gpu-box:11434", custom.getBaseUrl());
        assertEquals("qwen2.5:7b", custom.getModel());
        assertEquals("Ollama (qwen2.5:7b)", custom.getName());
    }

    @Test
    void anUnknownProviderIsAClearError() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AIModelFactory.create(env(AIModelFactory.PROVIDER_VARIABLE, "olama")));
        assertTrue(e.getMessage().contains("olama"));
        assertTrue(e.getMessage().contains(AIModelFactory.PROVIDER_VARIABLE));
    }
}
