package com.smartplate.app;

import com.smartplate.ai.AIAdapter;
import com.smartplate.ai.AIClient;
import com.smartplate.ai.AIModel;
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
 *   java -jar smartplate.jar --mock     force the offline Mock AI even if an API key is set
 *   java -jar smartplate.jar --empty    start with an empty pantry
 * </pre>
 * The real AI is used when ANTHROPIC_API_KEY is set; otherwise the Mock AI is used.
 */
public final class SmartPlateApp {

    private SmartPlateApp() {
    }

    public static void main(String[] args) {
        List<String> options = Arrays.asList(args);
        SmartRecipeFacade facade = SmartRecipeFacade.create(chooseModel(options.contains("--mock")));
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

    /** Real AI (Adapter -> Client) if configured, otherwise the offline mock. */
    static AIModel chooseModel(boolean forceMock) {
        AIClient client = AIClient.fromEnvironment();
        if (!forceMock && client.isConfigured()) {
            return new AIAdapter(client);
        }
        return new MockAIModel();
    }
}
