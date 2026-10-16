package com.smartplate.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads settings such as ANTHROPIC_API_KEY from a local {@code .env} file, like dotenv in Node.
 *
 * <p>The file is git-ignored, so a key kept there is never pushed. Format: one {@code KEY=value}
 * per line; blank lines and lines starting with {@code #} are skipped, an optional leading
 * {@code export } and surrounding quotes are removed. Real environment variables win over the
 * file, so a value set in the terminal or IDE always takes precedence. Values are never printed.
 */
public final class EnvFile {

    /** Looked up in the working directory (the project folder when run from there or from the IDE). */
    public static final String FILE_NAME = ".env";

    private EnvFile() {
    }

    /** The real environment plus the entries of ./.env that the environment does not already set. */
    public static Map<String, String> load() {
        return merge(System.getenv(), Path.of(FILE_NAME));
    }

    static Map<String, String> merge(Map<String, String> environment, Path file) {
        Map<String, String> merged = new HashMap<>(read(file));
        environment.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                merged.put(key, value);
            }
        });
        return merged;
    }

    /** Parses the file; a missing or unreadable file gives an empty map. */
    static Map<String, String> read(Path file) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(file)) {
            return values;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not read " + file + "; ignoring it.");
            return values;
        }
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).strip();
            }
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = line.substring(0, equals).strip();
            String value = unquote(line.substring(equals + 1).strip());
            if (!value.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' || first == '\'') && first == last) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
