# SmartPlate — Stage 3 Part A: testing the deterministic components

The deterministic part of SmartPlate (parsing, validation, the domain model, the services, the
design-pattern classes and the user interfaces) is tested with **JUnit 5** through Maven. None of
these tests needs an API key or the network: wherever the AI model is involved, the offline
`MockAIModel` stands in for it. Its scripted mode returns pre-written answers, including wrong
ones, and its failing mode simulates an unavailable service. The Gemini and Ollama adapters are
tested against a small local HTTP server started by the test itself.

The non-deterministic agent behaviour (what a real LLM chooses) is tested separately with KUMA:
see [STAGE3_TEST_DESIGN.md](STAGE3_TEST_DESIGN.md) and [agent-tests/](../agent-tests/README.md).

## How to run

```bash
mvn test                                   # all tests (about 5 s)
mvn -Dtest=RequestConstraintsTest test     # one test class
mvn -Dtest='MealPlanningAgentTest#editPlan*' test   # some methods of one class
```

In VS Code: *Terminal → Run Task… → SmartPlate: test*, or the Testing view (the Java
extensions find the JUnit tests). Reports are written to `target/surefire-reports/`.

## Results

Run on 30 Oct 2026 with OpenJDK 21.0.11 and Maven 3.9.11 (branch `feat/stage3-testing`):

```text
Tests run: 135, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS   (Total time: 4.1 s)
```

**135 tests in 22 test classes**: 103 unit tests and 32 integration tests. The integration tests
are the facade tests (8), CLI (3), JSON session (3), GUI panels (16) and the two tests that run the
agent through the Gemini and Ollama adapters.

## What each important class needs, and where it is tested

Types: **N** normal case, **B** boundary, **I** invalid input, **E** exception / error handling,
**X** interaction between components (integration).

| Class(es) under test | What needs to be tested | Test class (tests) | Types |
|---|---|---|---|
| `RequestConstraints` | Reading days, recipe count, servings, time limit, foods to avoid and diets from free text; 5–7 day limits; decimals ("9.5 days"); words after "no" that are not foods; multi-word and accented foods; huge numbers | `RequestConstraintsTest` (11) | N B I E |
| `ConstraintValidator` | Allergens found in ingredients even when the tags look fine; time limit and dislikes; 5–7 days per plan; violation messages name the day | `ConstraintValidatorTest` (6) | N B I |
| `DietRules` | Plural and singular terms; group terms (nuts, gluten, dairy) by category or member; whole-word matching ("nut" ≠ "nutmeg"); diets from categories; dislikes vs. allergies for plant-based milks; accents | `DietRulesTest` (8) | N B I |
| `MealPlanningAgent` | Only validated results are returned; one revision with the violation messages, then a clear failure; impossible constraints reported before the AI is called; AI service failure; request diets enforced; strategy order; edit returns an undoable command; unknown day / invalid or invented replacement rejected; AI chooses the day when none is named; "sun-dried" is not Sunday; one model per request | `MealPlanningAgentTest` (17) | N B I E X |
| `UserProfile` | Valid profile; servings and time below 1; duplicate, malformed and unknown entries; 1 serving / 1 minute as the smallest valid values; request constraints tighten a copy only | `UserProfileTest` (6) | N B I |
| `Pantry`, `Units` | Merging compatible units (g + kg); rejecting incompatible or unsupported units; update and remove; invalid quantities; unit formatting | `PantryTest` (4), `UnitsTest` (3) | N B I E |
| `RecipeRepository`, `ServingScaler`, `SubstitutionService`, `NutritionService`, `ShoppingListService` | Search by name, tag and ingredient; scaling up and down to one serving, as a copy; substitutes that respect the profile, with converted quantity and unit; recipe and plan nutrition; shopping list consolidation minus the pantry, counted items rounded up; empty plan is an error | `ServicesTest` (12), `ShoppingListTest` (1) | N B I E |
| `MealPlan`, `PlannedMeal` (Composite), `Days` | A leaf scales nutrition and ingredients to its servings; the composite sums its children; one meal per day, at most 7 days; replace only an existing day; day names in free text, hyphenated words excluded | `MealPlanTest` (4), `DaysTest` (2) | N B I |
| `CommandHistory`, `ReplaceMealCommand` (Command) | Execute, then undo in reverse order | `CommandHistoryTest` (1), plus undo in `MealPlanningAgentTest`, `SmartRecipeFacadeTest`, CLI and GUI tests | N X |
| `FastestMealStrategy`, `PantryFirstStrategy` (Strategy) | Sort by time; prefer pantry items; both drop recipes that do not fit the profile | `StrategyTest` (3) | N |
| `AIModelFactory`, `EnvFile` (Factory) | `auto` falls back to the mock without a key, prefers Claude then Gemini, never picks Ollama on its own; explicit providers honoured; defaults; missing keys reported; unknown provider is a clear error; `.env` parsing; real environment wins over the file | `AIModelFactoryTest` (9), `EnvFileTest` (4) | N B I E |
| `AIAdapter`, `AIClient`, `GeminiAdapter`, `OllamaAdapter`, `MockAIModel` (Adapter) | The prompt is translated into each provider's request format; answers are extracted; a missing key fails before any network call; HTTP errors, empty answers, a missing model and an unreachable server become clear `AIServiceException`s; the agent works unchanged through each adapter | `AIModelTest` (5), `GeminiAdapterTest` (4), `OllamaAdapterTest` (5) | N I E X |
| `SmartRecipeFacade` (Facade) | F01 invalid profile keeps the previous one; F02 pantry actions; F03 empty query; F04–F10 full workflow; plan features need a plan; a new plan clears the undo history; strategy and AI model switching | `SmartRecipeFacadeTest` (8) | N I E X |
| `MealPlannerCLI` | A scripted session covering every command; errors are shown, not thrown; `undo` with nothing to undo does not claim success | `MealPlannerCLITest` (3) | X E |
| `MealPlannerJsonSession` (Stage 3) | State kept between lines (plan → edit → undo); invalid JSON, invalid requests and unknown actions become error replies and the session continues; AI failure reported with the trace | `MealPlannerJsonSessionTest` (3) | X I E |
| GUI panels | Empty states and disabled actions; errors next to the right field; request, edit, undo and the CHANGED marker; shopping list and nutrition need a plan; the model switcher; the window locks the boxes while a request runs | `GuiPanelsTest` (16) | X I E |

Getters, setters and other trivial methods are not tested on their own.

## Important failures found and fixed

Each defect below was found during Stage 2 by a test, a code review or manual use. It was
fixed on its own branch, and its regression test now runs in the suite.

| # | Failure | Found by | Fix | Regression test(s) |
|---|---|---|---|---|
| A1 | The shopping list asked for "13.5 pcs garlic" | review of CLI output | counted items rounded up | `ServicesTest.shoppingListRoundsCountedItemsUpToWholeNumbers` |
| A2 | "swap the sun-dried tomato dish" was read as Sunday | review of the day parser | `Days.findIn` ignores hyphenated words | `DaysTest.hyphenatedWordsThatStartLikeADayAreNotDays`, `MealPlanningAgentTest.editPlanDoesNotMistakeSundriedForSunday` |
| A3 | "avoid red onion" also avoided every "red" food (red lentils) | review of the request parser | avoid phrases, not single words | `RequestConstraintsTest.avoidingRedOnionDoesNotBlockOtherRedOrOnionIngredients` |
| A4 | Disliking milk blocked oat milk as milk's substitute | a substitution test | dislikes are softer than allergies for plant-based foods | `DietRulesTest.dislikingADairyFoodDoesNotBlockPlantBasedFoodsNamedAfterIt`, `ServicesTest.dislikingMilkStillAllowsPlantBasedMilks`, `DietRulesTest.allergiesStayStrictForFoodsNamedAfterTheAllergen` |
| A5 | Switching model during a request could use two models in one request | code review | one model per request | `MealPlanningAgentTest.aRequestKeepsTheAIModelItStartedWithIfTheUserSwitchesMidway` |
| A6 | "jalapeño" was rejected as malformed | manual test | accented letters accepted and matched | `UserProfileTest.entriesWithAccentedLettersAreAccepted`, `RequestConstraintsTest.avoidAcceptsAccentedWords`, `DietRulesTest.accentedAndPlainSpellingsMatchEachOther` |
| A7 | The CLI printed "Undone." and then "Nothing to undo." | checking every error case | undo first, print after | `MealPlannerCLITest.undoWithNothingToUndoDoesNotClaimSuccess` |
| A8 | "dinner for 9.5 days" made a 5-day plan | the author, while testing | decimals read and rounded (a half toward 5–7); huge numbers are an error | `RequestConstraintsTest.decimalDaysRoundToTheNearestDayWithHalvesTowardFiveToSeven`, `RequestConstraintsTest.otherDecimalNumbersAreRoundedAndHugeNumbersAreAnError` |

The KUMA agent tests then found further deterministic defects: allergens such as "sesame",
"tree nuts" and "lactose" are not recognised, and "Plan 5 vegan dinners" is read as 7 days. They
are analysed in the Stage 3 report. Every fix made for them adds a JUnit regression test here as
well, so the deterministic suite keeps them fixed.
