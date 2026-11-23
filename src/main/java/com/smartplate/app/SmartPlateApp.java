package com.smartplate.app;

import com.smartplate.ai.AIModel;
import com.smartplate.ai.AIModelFactory;
import com.smartplate.ai.EnvFile;
import com.smartplate.ai.MockAIModel;
import com.smartplate.domain.PantryAction;
import com.smartplate.domain.PantryItem;
import com.smartplate.facade.SmartRecipeFacade;
import com.smartplate.presentation.MealPlannerCLI;
import com.smartplate.presentation.MealPlannerGUI;
import com.smartplate.service.SampleData;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Program entry point. The options are listed in {@link CommandLineOptions#USAGE}:
 *
 * <pre>
 *   java -jar smartplate.jar                               GUI (or CLI if no display is available)
 *   java -jar smartplate.jar --cli                         interactive command-line interface
 *   java -jar smartplate.jar --provider ollama --input task.txt   run the commands in task.txt
 *   java -jar smartplate.jar --mock                        force the offline Mock AI
 *   java -jar smartplate.jar --empty                       start with an empty pantry
 * </pre>
 * Without --provider (or --mock), AIModelFactory decides from the environment and .env:
 * SMARTPLATE_AI_PROVIDER = auto (default) | mock | anthropic | gemini | ollama.
 */
public final class SmartPlateApp {

    /** Exit status for a bad option, a missing task file or a model that cannot be used. */
    static final int USAGE_ERROR = 2;

    private SmartPlateApp() {
    }

    public static void main(String[] args) {
        CommandLineOptions options;
        AIModel model;
        try {
            options = CommandLineOptions.parse(args);
            if (options.isHelp()) {
                System.out.println(CommandLineOptions.USAGE);
                return;
            }
            model = chooseModel(options.getProvider());
        } catch (IllegalArgumentException e) {
            // a mistyped option or SMARTPLATE_AI_PROVIDER, or a missing key: say so instead of a stack trace
            System.err.println(e.getMessage());
            System.exit(USAGE_ERROR);
            return;
        }
        SmartRecipeFacade facade = SmartRecipeFacade.create(model);
        if (!options.isEmptyPantry()) {
            for (PantryItem item : SampleData.pantry()) {
                facade.managePantry(PantryAction.ADD, item);
            }
        }
        if (options.getInputFile() != null) {
            runTaskFile(facade, options.getInputFile());
        } else if (options.isCli() || GraphicsEnvironment.isHeadless()) {
            new MealPlannerCLI(facade, System.in, System.out).runLoop();
        } else {
            new MealPlannerGUI(facade).start();
        }
    }

    /** Runs the commands in a task file ("-" = standard input), echoing each one before its result. */
    private static void runTaskFile(SmartRecipeFacade facade, String file) {
        if (file.equals("-")) {
            new MealPlannerCLI(facade, System.in, System.out, true).runLoop();
            return;
        }
        try (InputStream in = Files.newInputStream(Path.of(file))) {
            new MealPlannerCLI(facade, in, System.out, true).runLoop();
        } catch (NoSuchFileException e) {
            System.err.println("Task file not found: " + file);
            System.exit(USAGE_ERROR);
        } catch (IOException e) {
            System.err.println("Could not read the task file " + file + ": " + e.getMessage());
            System.exit(USAGE_ERROR);
        }
    }

    /**
     * The provider given with --provider (or --mock) wins; with none, the environment decides
     * (see AIModelFactory). A provider whose key is missing is an error, not a silent fallback.
     */
    static AIModel chooseModel(String provider) {
        if (provider == null) {
            return AIModelFactory.fromEnvironment();
        }
        if (provider.equals(AIModelFactory.MOCK)) {
            return new MockAIModel();
        }
        Map<String, String> settings = EnvFile.load();
        String missing = AIModelFactory.missingSetting(provider, settings);
        if (missing != null) {
            throw new IllegalArgumentException(missing);
        }
        return AIModelFactory.forProvider(provider, settings);
    }
}
