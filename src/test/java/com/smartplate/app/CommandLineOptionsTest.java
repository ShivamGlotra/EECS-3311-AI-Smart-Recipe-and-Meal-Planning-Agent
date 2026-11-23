package com.smartplate.app;

import com.smartplate.ai.MockAIModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineOptionsTest {

    @Test
    void noOptionsMeansTheGuiAndTheModelFromTheEnvironment() {
        CommandLineOptions o = CommandLineOptions.parse();
        assertFalse(o.isCli());
        assertNull(o.getInputFile());
        assertNull(o.getProvider());
        assertFalse(o.isEmptyPantry());
        assertFalse(o.isHelp());
    }

    @Test
    void inputFileImpliesTheCliAndProviderOverridesTheEnvironment() {
        CommandLineOptions o = CommandLineOptions.parse("--provider", "Ollama", "--input", "task.txt", "--empty");
        assertTrue(o.isCli());
        assertEquals("task.txt", o.getInputFile());
        assertEquals("ollama", o.getProvider());
        assertTrue(o.isEmptyPantry());
        assertEquals("-", CommandLineOptions.parse("--input", "-").getInputFile());
        assertEquals("mock", CommandLineOptions.parse("--mock").getProvider());
        assertTrue(CommandLineOptions.parse("--help").isHelp());
    }

    @Test
    void badOptionsAreClearErrors() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommandLineOptions.parse("--inptu", "x"))
                .getMessage().contains("Unknown option '--inptu'"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommandLineOptions.parse("--input"))
                .getMessage().contains("--input needs a value"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommandLineOptions.parse("--provider", "--cli"))
                .getMessage().contains("--provider needs a value"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommandLineOptions.parse("--provider", "gpt"))
                .getMessage().contains("Unknown provider 'gpt'"));
    }

    @Test
    void theMockProviderNeedsNoSettings() {
        assertInstanceOf(MockAIModel.class, SmartPlateApp.chooseModel("mock"));
    }
}
