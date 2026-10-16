package com.smartplate.ai;

import com.anthropic.models.messages.MessageCreateParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIModelTest {

    @Test
    void adapterWithoutApiKeyReportsAServiceError() {
        AIModel model = new AIAdapter(new AIClient(null, null));
        AIServiceException e = assertThrows(AIServiceException.class, () -> model.generate("hello"));
        assertTrue(e.getMessage().contains(AIClient.API_KEY_VARIABLE));
    }

    @Test
    void adapterTranslatesThePromptIntoAProviderRequest() {
        AIClient client = new AIClient("test-key", "claude-test-model");
        MessageCreateParams request = new AIAdapter(client).buildRequest("TASK: PLAN");
        assertEquals("claude-test-model", request.model().asString());
        assertTrue(request.messages().get(0).content().toString().contains("TASK: PLAN"));
        assertTrue(request.system().isPresent());
    }

    @Test
    void clientUsesDefaultModelAndKnowsWhenItIsConfigured() {
        assertEquals(AIClient.DEFAULT_MODEL, new AIClient("k", " ").getModel());
        assertFalse(new AIClient("", null).isConfigured());
        assertTrue(new AIClient("k", null).isConfigured());
    }

    @Test
    void mockAnswersInTheRequestedFormat() {
        String prompt = String.join("\n",
                "TASK: RECOMMEND",
                "REQUEST: something with spinach",
                "DIET: vegetarian",
                "AVOID: mushrooms",
                "MAX_TIME: 30",
                "EXCLUDE: none",
                "CANDIDATES:",
                "- a1 | Mushroom Toast | 10 min | tags: vegetarian | ingredients: mushroom, bread",
                "- a2 | Beef Bowl | 10 min | tags: none | ingredients: beef, rice",
                "- a3 | Rice Bowl | 10 min | tags: vegetarian | ingredients: rice",
                "- a4 | Spinach Bowl | 20 min | tags: vegetarian | ingredients: spinach, rice",
                "- a5 | Slow Stew | 90 min | tags: vegetarian | ingredients: carrot",
                "COUNT: 2");
        assertEquals("RECIPES: a4, a3", new MockAIModel().generate(prompt));
    }

    @Test
    void scriptedAndFailingMocks() {
        MockAIModel scripted = MockAIModel.scripted("one");
        assertEquals("one", scripted.generate("x"));
        assertEquals(1, scripted.getPrompts().size());
        assertThrows(AIServiceException.class, () -> MockAIModel.failing().generate("x"));
    }
}
