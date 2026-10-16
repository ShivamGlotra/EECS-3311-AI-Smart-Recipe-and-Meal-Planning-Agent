package com.smartplate.facade;

import com.smartplate.agent.ConstraintValidator;
import com.smartplate.agent.MealPlanningAgent;
import com.smartplate.ai.AIModel;
import com.smartplate.ai.AIModelFactory;
import com.smartplate.ai.EnvFile;
import com.smartplate.command.Command;
import com.smartplate.command.CommandHistory;
import com.smartplate.domain.Ingredient;
import com.smartplate.domain.InvalidProfileException;
import com.smartplate.domain.NutritionInfo;
import com.smartplate.domain.Pantry;
import com.smartplate.domain.PantryAction;
import com.smartplate.domain.PantryItem;
import com.smartplate.domain.Recipe;
import com.smartplate.domain.ShoppingList;
import com.smartplate.domain.SmartPlateException;
import com.smartplate.domain.Substitution;
import com.smartplate.domain.UserProfile;
import com.smartplate.plan.MealPlan;
import com.smartplate.service.NutritionService;
import com.smartplate.service.RecipeRepository;
import com.smartplate.service.ServingScaler;
import com.smartplate.service.ShoppingListService;
import com.smartplate.service.SubstitutionService;
import com.smartplate.strategy.FastestMealStrategy;
import com.smartplate.strategy.PantryFirstStrategy;
import com.smartplate.strategy.RecipeSelectionStrategy;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Facade pattern - the single entry point for every feature.
 *
 * <p>MealPlannerGUI and MealPlannerCLI call only this class, so both interfaces behave the
 * same. The facade owns the session state (profile, pantry, current plan) and delegates the
 * work to the agent, the services and the command history. Every method either returns a
 * result or throws a SmartPlateException whose message can be shown to the user.
 */
public class SmartRecipeFacade {

    private UserProfile profile = UserProfile.defaultProfile();
    private final Pantry pantry = new Pantry();
    private final RecipeRepository repository;
    private final MealPlanningAgent agent;
    private final NutritionService nutritionService;
    private final ServingScaler servingScaler;
    private final SubstitutionService substitutionService;
    private final ShoppingListService shoppingListService;
    private final CommandHistory commandHistory;
    private MealPlan currentPlan;
    private Substitution lastSubstitution;
    /** Where AI settings (API keys, model names) come from when the user switches model. */
    private Supplier<Map<String, String>> aiSettings = EnvFile::load;

    public SmartRecipeFacade(RecipeRepository repository, MealPlanningAgent agent, NutritionService nutritionService,
                             ServingScaler servingScaler, SubstitutionService substitutionService,
                             ShoppingListService shoppingListService, CommandHistory commandHistory) {
        this.repository = repository;
        this.agent = agent;
        this.nutritionService = nutritionService;
        this.servingScaler = servingScaler;
        this.substitutionService = substitutionService;
        this.shoppingListService = shoppingListService;
        this.commandHistory = commandHistory;
    }

    /** Wires the whole system with the sample recipes and the given AI model. */
    public static SmartRecipeFacade create(AIModel aiModel) {
        return create(aiModel, EnvFile::load);
    }

    /** As {@link #create(AIModel)}, with the AI settings taken from the given source (used by tests). */
    public static SmartRecipeFacade create(AIModel aiModel, Supplier<Map<String, String>> aiSettings) {
        RecipeRepository repository = RecipeRepository.withSampleRecipes();
        MealPlanningAgent agent = new MealPlanningAgent(aiModel, repository, new ConstraintValidator(),
                new PantryFirstStrategy());
        SmartRecipeFacade facade = new SmartRecipeFacade(repository, agent, new NutritionService(),
                new ServingScaler(), SubstitutionService.withDefaultSubstitutions(), new ShoppingListService(),
                new CommandHistory());
        facade.aiSettings = aiSettings;
        return facade;
    }

    // ------------------------------------------------------------------ F01
    /**
     * Stores the profile if {@link UserProfile#validate()} finds no problems. Otherwise the
     * previous valid profile is kept and an InvalidProfileException lists the problems.
     */
    public UserProfile setupProfile(UserProfile newProfile) {
        if (newProfile == null) {
            throw new SmartPlateException("No profile given.");
        }
        List<String> problems = newProfile.validate();
        if (!problems.isEmpty()) {
            throw new InvalidProfileException(problems);
        }
        profile = newProfile;
        return profile;
    }

    // ------------------------------------------------------------------ F02
    public Pantry managePantry(PantryAction action, PantryItem item) {
        if (action == null || item == null) {
            throw new SmartPlateException("Choose an action and an item.");
        }
        switch (action) {
            case ADD:
                pantry.addItem(item);
                break;
            case UPDATE:
                pantry.updateItem(item);
                break;
            case REMOVE:
                pantry.removeItem(item.getName());
                break;
            default:
                throw new SmartPlateException("Unknown pantry action " + action + ".");
        }
        return pantry;
    }

    // ------------------------------------------------------------------ F03
    /** Returns matching recipes (possibly an empty list). Rejects an empty query. */
    public List<Recipe> searchRecipes(String query) {
        if (query == null || query.isBlank()) {
            throw new SmartPlateException("Please enter a search query.");
        }
        return repository.search(query);
    }

    // ------------------------------------------------------------------ F04
    public List<Recipe> recommendRecipes(String request) {
        requireText(request);
        return agent.recommend(request, profile, pantry);
    }

    // ------------------------------------------------------------------ F05
    /** Creates a validated plan; it becomes the current plan and the undo history is cleared. */
    public MealPlan createMealPlan(String request) {
        requireText(request);
        MealPlan plan = agent.createPlan(request, profile, pantry);
        currentPlan = plan;
        commandHistory.clear();
        return plan;
    }

    // ------------------------------------------------------------------ F06
    public MealPlan editMealPlan(String request) {
        requireText(request);
        requirePlan();
        Command command = agent.editPlan(request, currentPlan, profile, pantry);
        commandHistory.execute(command);
        return currentPlan;
    }

    public MealPlan undoLastChange() {
        if (!commandHistory.canUndo()) {
            throw new SmartPlateException("Nothing to undo.");
        }
        commandHistory.undo();
        return currentPlan;
    }

    // ------------------------------------------------------------------ F07
    public Recipe adjustServings(Recipe recipe, int servings) {
        requireRecipe(recipe);
        if (servings <= 0) {
            throw new SmartPlateException("Servings must be at least 1.");
        }
        return servingScaler.scale(recipe, servings);
    }

    // ------------------------------------------------------------------ F08
    public Recipe substituteIngredient(Recipe recipe, Ingredient ingredient) {
        requireRecipe(recipe);
        if (ingredient == null || recipe.findIngredient(ingredient.getName()) == null) {
            throw new SmartPlateException(recipe.getName() + " does not contain "
                    + (ingredient == null ? "that ingredient" : ingredient.getName()) + ".");
        }
        Substitution substitution = substitutionService.findSubstitute(ingredient, profile);
        Recipe updated = substitutionService.applySubstitution(recipe, substitution);
        lastSubstitution = substitution;
        return updated;
    }

    // ------------------------------------------------------------------ F09
    public NutritionInfo calculateRecipeNutrition(Recipe recipe) {
        requireRecipe(recipe);
        return nutritionService.calculateRecipe(recipe);
    }

    public NutritionInfo calculatePlanNutrition() {
        requirePlan();
        return nutritionService.calculatePlan(currentPlan);
    }

    // ------------------------------------------------------------------ F10
    public ShoppingList generateShoppingList() {
        requirePlan();
        return shoppingListService.generate(currentPlan, pantry);
    }

    // ------------------------------------------------------------------ queries used by the UIs
    public UserProfile getProfile() {
        return profile;
    }

    public Pantry getPantry() {
        return pantry;
    }

    public MealPlan getCurrentPlan() {
        return currentPlan;
    }

    public boolean canUndo() {
        return commandHistory.canUndo();
    }

    /** Looks up a recipe by id. */
    public Recipe getRecipe(String id) {
        Recipe recipe = repository.findById(id);
        if (recipe == null) {
            throw new SmartPlateException("No recipe with id '" + id + "'.");
        }
        return recipe;
    }

    public List<Recipe> getAllRecipes() {
        return repository.findAll();
    }

    /** The substitution used by the last successful substituteIngredient() call. */
    public Substitution getLastSubstitution() {
        return lastSubstitution;
    }

    /** Selects the recipe-selection strategy by name: "pantry-first" or "fastest". */
    public void setStrategy(String name) {
        RecipeSelectionStrategy strategy;
        String key = name == null ? "" : name.trim().toLowerCase();
        if (key.startsWith("pantry")) {
            strategy = new PantryFirstStrategy();
        } else if (key.startsWith("fast")) {
            strategy = new FastestMealStrategy();
        } else {
            throw new SmartPlateException("Unknown strategy '" + name + "'. Use pantry-first or fastest.");
        }
        agent.setStrategy(strategy);
    }

    public String getStrategyName() {
        return agent.getStrategy().getName();
    }

    public String getAIModelName() {
        return agent.getAIModel().getName();
    }

    /** The AI providers the user can switch between: mock, anthropic, gemini, ollama. */
    public List<String> getAIProviders() {
        return AIModelFactory.PROVIDERS;
    }

    /** The provider of the AI model in use. */
    public String getAIProvider() {
        return AIModelFactory.providerOf(agent.getAIModel());
    }

    /**
     * Switches the AI model at run time. The settings (.env file and environment) are read
     * again, so a key added to .env works without a restart. If the provider cannot be used
     * (e.g. its API key is missing) the current model is kept and the reason is thrown.
     */
    public void setAIProvider(String provider) {
        Map<String, String> settings = aiSettings.get();
        String missing = AIModelFactory.missingSetting(provider, settings);
        if (missing != null) {
            throw new SmartPlateException(missing);
        }
        try {
            agent.setAIModel(AIModelFactory.forProvider(provider, settings));
        } catch (IllegalArgumentException e) {
            throw new SmartPlateException(e.getMessage());
        }
    }

    /** What the agent did during the last AI request (for "explain" / transparency). */
    public List<String> getLastAgentTrace() {
        return agent.getLastTrace();
    }

    private void requirePlan() {
        if (currentPlan == null || currentPlan.isEmpty()) {
            throw new SmartPlateException("There is no meal plan yet. Create one first.");
        }
    }

    private static void requireRecipe(Recipe recipe) {
        if (recipe == null) {
            throw new SmartPlateException("No recipe selected.");
        }
    }

    private static void requireText(String request) {
        if (request == null || request.isBlank()) {
            throw new SmartPlateException("Please type a request.");
        }
    }
}
