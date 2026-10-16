package com.smartplate.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvFileTest {

    @TempDir
    Path dir;

    private Path write(String content) throws IOException {
        return Files.writeString(dir.resolve(".env"), content);
    }

    @Test
    void readsKeyValueLinesLikeDotenv() throws IOException {
        Map<String, String> values = EnvFile.read(write(String.join("\n",
                "# a comment",
                "",
                "ANTHROPIC_API_KEY=sk-ant-123",
                "export OLLAMA_MODEL = gemma4:e4b ",
                "SMARTPLATE_AI_PROVIDER=\"ollama\"",
                "OLLAMA_URL='http://localhost:11434'",
                "EMPTY=",
                "not a setting")));
        assertEquals("sk-ant-123", values.get("ANTHROPIC_API_KEY"));
        assertEquals("gemma4:e4b", values.get("OLLAMA_MODEL"));
        assertEquals("ollama", values.get("SMARTPLATE_AI_PROVIDER"));
        assertEquals("http://localhost:11434", values.get("OLLAMA_URL"));
        assertFalse(values.containsKey("EMPTY"));
        assertEquals(4, values.size());
    }

    @Test
    void aMissingFileIsFine() {
        assertTrue(EnvFile.read(dir.resolve("nope.env")).isEmpty());
    }

    @Test
    void realEnvironmentVariablesWinOverTheFile() throws IOException {
        Path file = write("SMARTPLATE_AI_PROVIDER=ollama\nANTHROPIC_API_KEY=from-file\n");
        Map<String, String> merged = EnvFile.merge(
                Map.of("SMARTPLATE_AI_PROVIDER", "mock", "ANTHROPIC_API_KEY", " "), file);
        assertEquals("mock", merged.get("SMARTPLATE_AI_PROVIDER"));
        assertEquals("from-file", merged.get("ANTHROPIC_API_KEY"), "a blank variable does not hide the file's value");
    }

    @Test
    void aKeyInTheFileSwitchesAutoToClaude() throws IOException {
        Map<String, String> env = EnvFile.merge(Map.of(), write("ANTHROPIC_API_KEY=sk-ant-123\n"));
        assertInstanceOf(AIAdapter.class, AIModelFactory.create(env));
    }
}
