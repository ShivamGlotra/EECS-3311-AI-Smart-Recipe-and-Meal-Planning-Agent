package com.smartplate.app;

import com.smartplate.ai.AIModel;
import com.smartplate.ai.AIModelFactory;
import com.smartplate.ai.MockAIModel;
import com.smartplate.domain.PantryAction;
import com.smartplate.domain.PantryItem;
import com.smartplate.facade.SmartRecipeFacade;
import com.smartplate.presentation.MealPlannerCLI;
import com.smartplate.presentation.MealPlannerGUI;
import com.smartplate.service.SampleData;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/**
 * Program entry point.
 *
 * <pre>
 *   java -jar smartplate.jar            GUI (or CLI if no display is available)
 *   java -jar smartplate.jar --cli      command-line interface
 *   java -jar smartplate.jar --mock     force the offline Mock AI whatever the environment says
 *   java -jar smartplate.jar --empty    start with an empty pantry
 * </pre>
 * Which AI is used is decided by AIModelFactory from the environment:
 * SMARTPLATE_AI_PROVIDER = auto (default) | mock | anthropic | ollama.
 * With "auto" the real AI is used when ANTHROPIC_API_KEY is set, otherwise the Mock AI.
 */
public final class SmartPlateApp {

    private SmartPlateApp() {
    }

    public static void main(String[] args) {
        List<String> options = Arrays.asList(args);
        AIModel model;
        try {
            model = chooseModel(options.contains("--mock"));
        } catch (IllegalArgumentException e) {
            // e.g. a mistyped SMARTPLATE_AI_PROVIDER: say so clearly instead of a stack trace
            System.err.println(e.getMessage());
            return;
        }
        SmartRecipeFacade facade = SmartRecipeFacade.create(model);
        if (!options.contains("--empty")) {
            for (PantryItem item : SampleData.pantry()) {
                facade.managePantry(PantryAction.ADD, item);
            }
        }
        if (options.contains("--cli") || GraphicsEnvironment.isHeadless()) {
            new MealPlannerCLI(facade, System.in, System.out).runLoop();
        } else {
            new MealPlannerGUI(facade).start();
        }
    }

    /** The --mock flag always wins; otherwise the environment decides (see AIModelFactory). */
    static AIModel chooseModel(boolean forceMock) {
        return forceMock ? new MockAIModel() : AIModelFactory.fromEnvironment();
    }
}
