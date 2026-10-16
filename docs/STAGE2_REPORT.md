# SmartPlate — Stage 2 Report: AI-Human Collaborative Implementation

EECS 3311, Fall 2026. This report covers the Stage 2 deliverables: what was implemented, which AI
tools were used and how, how the implementation traces back to the Stage 1 design, and which parts
of the design changed and why. The Stage 1 design is in
[`SmartPlate_Stage1_Report_FINAL.md`](../SmartPlate_Stage1_Report_FINAL.md) and
[`docs/umlet-diagrams/`](umlet-diagrams/).

> **Items marked _[Team]_ must be completed by the team before submission.** They are facts only the
> team knows (for example, other AI tools a member used, or the results of the team's own review).

---

## Contents

1. [Stage 2 deliverables checklist](#1-stage-2-deliverables-checklist)
2. [Setup and run](#2-setup-and-run)
3. [AI tools and models (§9)](#3-ai-tools-and-models-9)
4. [AI-human collaboration log (§10)](#4-ai-human-collaboration-log-10)
5. [Design-to-implementation traceability (§11)](#5-design-to-implementation-traceability-11)
6. [Design changes: original → change → reason](#6-design-changes-original--change--reason)
7. [How the AI's output was checked](#7-how-the-ais-output-was-checked)

---

## 1. Stage 2 deliverables checklist

| Required (§20, Stage 2) | Where |
|---|---|
| Complete source code | `src/main/java/com/smartplate/**` (59 source files), built with Maven (`pom.xml`) |
| README and setup instructions | [`README.md`](../README.md) (quick start) and [`IMPLEMENTATION.md` §1](IMPLEMENTATION.md#1-build-and-run) (full) |
| AI tools used / LLM models used | [§3](#3-ai-tools-and-models-9) of this report |
| Representative AI-human collaboration log | [§4](#4-ai-human-collaboration-log-10) |
| Feature-to-implementation mapping | [§5](#5-design-to-implementation-traceability-11) |
| Explanation of important design changes | [§6](#6-design-changes-original--change--reason) |
| Code explanation (supporting) | [`IMPLEMENTATION.md`](IMPLEMENTATION.md), [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md) |
| AI assistance disclosure (supporting) | [`AI_USAGE.md`](AI_USAGE.md) |

---

## 2. Setup and run

Requirements: JDK 17 or newer, Maven 3.8 or newer.

```bash
mvn test                                   # 112 JUnit 5 tests, all offline
mvn package                                # builds target/smartplate-1.0.0.jar
java -jar target/smartplate-1.0.0.jar      # desktop GUI
java -jar target/smartplate-1.0.0.jar --cli
```

The app works offline with the Mock AI. To use a real model, copy `.env.example` to `.env`
(git-ignored), add a key (`ANTHROPIC_API_KEY` or `GEMINI_API_KEY`) or a local Ollama model
(`OLLAMA_MODEL`), and choose the model in the **AI model** box at the top right of the window.
Details: [`IMPLEMENTATION.md` §1](IMPLEMENTATION.md#1-build-and-run).

---

## 3. AI tools and models (§9)

### 3.1 AI tools used to develop SmartPlate

| Tool / platform | LLM model | Purpose | Example tasks | Major parts of the project |
|---|---|---|---|---|
| **Claude Code** (Anthropic's coding agent, used through claude.ai/code in cloud sessions) | _[Team]_ Record the model exactly as Claude Code's session details show it. If the session shows that some turns were served by a different (fallback) model, say so. | Code generation, test generation, debugging, refactoring, UI design proposals, documentation | Implemented all ten features and the five patterns from the Stage 1 design; wrote the 112 JUnit tests; proposed three GUI designs and implemented the chosen one; wrote the Ollama and Gemini adapters; found and fixed GUI bugs through tests and screenshots; wrote `IMPLEMENTATION.md`, `DEVELOPER_GUIDE.md` and this report's drafts | `src/main/**`, `src/test/**`, `docs/**` except the Stage 1 report and diagrams; see [`AI_USAGE.md` §2](AI_USAGE.md#2-what-the-ai-produced) for the file-by-file split |
| _[Team]_ any other tool (e.g. ChatGPT, Copilot, Gemini) used for the Stage 1 report, the `AIModelFactory` / `SmartPlateApp` changes, or anything else | _[Team]_ | | | |

### 3.2 LLMs used *inside* SmartPlate at run time

These are components of the product (the `AIModel` abstraction from the design), not development
tools. They choose recipes from a shortlist; deterministic code (`ConstraintValidator`) checks every
answer.

| `AIModel` implementation | Provider / model | How it is selected |
|---|---|---|
| `MockAIModel` | none (offline, deterministic) | default without keys; always used by the tests |
| `AIAdapter` → `AIClient` | Anthropic API, Claude; model id from `SMARTPLATE_AI_MODEL` (default in `AIClient.DEFAULT_MODEL`) | `ANTHROPIC_API_KEY` in `.env` |
| `GeminiAdapter` | Google Gemini API; `GEMINI_MODEL` (default `gemini-flash-latest`, Google's alias for its newest Flash model) | `GEMINI_API_KEY` in `.env` |
| `OllamaAdapter` | a local model served by Ollama; the team uses `gemma4:e4b` (`OLLAMA_MODEL`) | `SMARTPLATE_AI_PROVIDER=ollama` or the GUI's AI model box |

---

## 4. AI-human collaboration log (§10)

Representative interactions with Claude Code, in order. Entries CL-01 to CL-10 were drafted from the
Claude Code session history; the **Human contribution** column lists what the team actually did in
that session. _[Team]_ Please confirm each entry, add what you reviewed or changed afterwards, and
add entries for work you did with other tools or on your own.

### CL-01 — Implement the Stage 1 design in Java (5 Oct 2026)

| | |
|---|---|
| **Task** | Turn the Stage 1 design (10 features, 5 patterns, UML) into a complete Java application. |
| **Input / instruction** | "Use [the `stage-1Fix` branch] and make the complete project in java implementing every feature as said in the report with explanation of the code and a documentation to give the prof where AI helped." |
| **AI contribution** | Read the report and UML, then generated every class in the class diagram plus helpers, the 30-recipe sample data, a Swing GUI and a CLI that both use only the facade, 88 JUnit tests, `IMPLEMENTATION.md` and `AI_USAGE.md`. Where the design was silent it followed the diagrams, e.g. `editPlan` makes one AI attempt with no revision, because SD05 shows none. |
| **Human contribution** | Wrote the design that the AI implemented (requirements, use cases, class and sequence diagrams, choice of patterns) and chose which branch held the final version of it. _[Team]_ add what you reviewed in the generated code. |
| **Outcome** | Incorporated: the whole code base on `claude/nifty-albattani-1lnuay`. Differences from the design are listed in §6. |

### CL-02 — Front-end options and a backend guide, kept out of the project (5 Oct 2026)

| | |
|---|---|
| **Task** | Understand how the Java logic could later serve a React or Flutter front end. |
| **Input / instruction** | "Don't do anything, just tell me" how a React or Flutter front end could use the Java logic; then a step-by-step downloadable guide for turning the app into a Spring Boot backend. Mid-task: "do not make it or run, just give me the steps. Do not make anything." |
| **AI contribution** | Explained the approach (expose the facade as a REST API, keep every rule in Java) and wrote a detailed Spring Boot conversion guide as a separate document. It had started to verify the steps by building them, and stopped when told. |
| **Human contribution** | Said the team would handle any front end itself, and limited the AI to explanation and documentation so the course project was not changed. |
| **Outcome** | Not incorporated into the project; the guide is a personal reference. An example of the team keeping the AI inside the agreed scope. |

### CL-03 — GUI design proposals (15 Oct 2026)

| | |
|---|---|
| **Task** | Improve the look and usability of the GUI before continuing. |
| **Input / instruction** | "Give me a bunch of designs UI for the app. Also give me a prompt for Figma AI to make designs." |
| **AI contribution** | Proposed design directions as mockups on a design canvas, using the real sample data from the code, and wrote a Figma AI prompt. |
| **Human contribution** | Asked whether the AI had read the code before designing. Corrected the platform: the first proposals included a dark-mode direction and mobile screens ("it is a desktop app, only light mode in java"). Showed the AI a screenshot of the current Swing GUI. Chose **Direction B, "Clean Slate"** (top tabs) from the revised light-mode desktop set. |
| **Outcome** | Direction B was used as the specification for CL-04. |

### CL-04 — Implement Direction B in Swing (15 Oct 2026)

| | |
|---|---|
| **Task** | Rebuild the GUI in the chosen design without touching non-GUI code. |
| **Input / instruction** | Implement Direction B on a **new branch** off `claude/nifty-albattani-1lnuay` (the team asked for a new branch rather than changing the existing one). The team also supplied the submitted Stage 1 report for context. |
| **AI contribution** | Split `MealPlannerGUI` into four tab panels plus small UI helpers, added the FlatLaf look-and-feel and bundled IBM Plex fonts, and wrote 14 headless GUI tests. Its tests and screenshots exposed bugs that it then fixed: the pantry "Added …" message was cleared by the row selection; a pantry error was shown next to the wrong field; the preferences column was clipped; table headers and column widths were wrong. |
| **Human contribution** | Chose the design, required a separate branch, and supplied the submitted Stage 1 report for context. |
| **Outcome** | Incorporated on `claude/swing-ui-direction-b`; GUI screenshots in [`screenshots/`](screenshots/). |

### CL-05 — A developer guide for the team (15 Oct 2026)

| | |
|---|---|
| **Task** | Make the AI-written code understandable to team members who know React but little Java. |
| **Input / instruction** | "Give me a complete guide and structure of this application so I can… make changes… and fix bugs… (I don't know much Java but I know React apps.)" |
| **AI contribution** | Wrote [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md): Java and Swing explained through React analogies, the architecture and patterns, a function-by-function reference with line numbers, end-to-end walkthroughs, a cookbook of common changes, and a list of known bugs. |
| **Human contribution** | Asked for the guide in a form the team could learn from, so that the team can explain and maintain the code rather than accept it unread. |
| **Outcome** | Incorporated as `docs/DEVELOPER_GUIDE.md`; kept up to date with later changes. |

### CL-06 — Questioning the AI's choice of JDK (15 Oct 2026)

| | |
|---|---|
| **Task** | Understand a technical choice the AI made. |
| **Input / instruction** | "Why did you use JDK 21?" then "Why not JDK 25?" |
| **AI contribution** | Explained that the code targets Java 17 (`maven.compiler.release`), that JDK 21 was only its recommendation because it had tested on 21, and that JDK 25 also works but may print harmless native-access warnings from FlatLaf. |
| **Human contribution** | Challenged an unexplained decision instead of accepting it. |
| **Outcome** | No change: the target stays Java 17, so the project builds on JDK 17, 21 or 25. |

### CL-07 — Choosing the AI model: the team's factory, the AI's adapter (15 Oct 2026)

| | |
|---|---|
| **Task** | Run SmartPlate on a local model (Ollama) and choose the model by configuration. |
| **Input / instruction** | The team designed the change ("`OllamaAdapter` implements `AIModel` the same way `AIAdapter` does, so the agent needs no changes"), wrote `AIModelFactory` and the new `SmartPlateApp.chooseModel`, and asked the AI to integrate them. |
| **AI contribution** | Added the team's two files unchanged. Pointed out that `OllamaAdapter` was missing and wrote it to the constructor and constants the team's factory expected. Tested it against a fake Ollama server, because it could not reach the team's machine. When its push was rejected, it found that the team had merged the final Stage 1 report into the branch, merged that in, and fixed three doc links that the diagram move had made stale. |
| **Human contribution** | Designed the extension using the Adapter pattern from the Stage 1 design; wrote the factory and the entry-point change; ran the app with the local model `gemma4:e4b`; merged the final Stage 1 report into the branch. |
| **Outcome** | Incorporated: `AIModelFactory`, `OllamaAdapter`, new tests. Shows the Stage 1 Adapter design paying off: a new provider without changing `MealPlanningAgent`. |

### CL-08 — Keeping API keys out of git, and stopping the AI from over-reaching (15 Oct 2026)

| | |
|---|---|
| **Task** | Use a real API key without ever committing it. |
| **Input / instruction** | "Can I have some safe way of adding the key that I don't have to push?" |
| **AI contribution** | Started implementing a `.env` loader, a `.env.example` file and `.gitignore` rules (including IntelliJ's `.idea/`, whose run configurations can hold keys) **before the team had agreed to a code change**. |
| **Human contribution** | Stopped it mid-task: "Don't keep adding things unless I ask. Tell me first." The AI then listed exactly what it had changed (uncommitted) and offered to keep, reduce or undo it. The team reviewed the options and approved keeping it. |
| **Outcome** | Incorporated after approval: `EnvFile`, `.env.example`, `.gitignore` rules. |

### CL-09 — Gemini adapter and a model switcher (15 Oct 2026)

| | |
|---|---|
| **Task** | Use the team's Gemini key and switch models without restarting. |
| **Input / instruction** | "Add an adapter for Gemini, I have a Gemini API key to use. Make sure to add proper commits and names. I also need an option on top right to switch between models." |
| **AI contribution** | Looked up the current Gemini model names; the sources disagreed about which models were being retired, so it chose Google's `gemini-flash-latest` alias as the default. Wrote `GeminiAdapter` (key sent in a header, never in the URL), added a `gemini` option to the factory, `setAIModel` on the agent, `setAIProvider` on the facade and the **AI model** box in the GUI. A screenshot showed the box cut off "Ollama (gemma4:e4b)", and the AI fixed it. Made four separate commits. |
| **Human contribution** | Asked whether the default Ollama model in the code should change to `gemma4:e4b`; the AI recommended setting it per machine in `.env` instead, and the code default was left unchanged. Ran the app locally with the new options and reported that it works. |
| **Outcome** | Incorporated: `GeminiAdapter`, the run-time switcher, tests (112 in total). |

### CL-10 — Checking the project against the Stage 2 rubric (15 Oct 2026)

| | |
|---|---|
| **Task** | Find what is still missing for Stage 2. |
| **Input / instruction** | The team pasted the Stage 2 and Stage 3 requirements and asked what was left. |
| **AI contribution** | Compared the repository with each requirement, found the missing tools table, collaboration log and traceability tables, and looked up how KUMA works for Stage 3. |
| **Human contribution** | Set the scope: finish Stage 2 only, leave Stage 3 for later, and keep branch merging with the team. |
| **Outcome** | This report, the refreshed screenshots and the updated README. |

### CL-11 onwards — _[Team]_

Add your own interactions here, for example the AI help used to write the Stage 1 report or the
`AIModelFactory`, and anything you reviewed, corrected or rejected after reading the generated code.

---

## 5. Design-to-implementation traceability (§11)

### 5.1 By feature

Use cases, sequence diagrams and designed classes are from the Stage 1 report (§2, §6–§8). "Status"
compares the implementation with that design. All paths are under `src/main/java/com/smartplate/`.

| Feature | UC / SD | Designed classes | Implemented classes / files | Tests | Status |
|---|---|---|---|---|---|
| **F01** Preference profile | UC01 / SD01 | `SmartRecipeFacade`, `UserProfile` | `facade/SmartRecipeFacade.java` (`setupProfile`), `domain/UserProfile.java` (`validate`), `domain/InvalidProfileException.java`; GUI `gui/PreferencesPanel.java` | `UserProfileTest`, `SmartRecipeFacadeTest.f01…`, `GuiPanelsTest` (profile) | Implemented |
| **F02** Pantry | UC02 / SD01 | `SmartRecipeFacade`, `Pantry`, `PantryItem`, `Ingredient`, `PantryAction` | `facade/SmartRecipeFacade.java` (`managePantry`), `domain/Pantry.java` (`addItem`, `updateItem`, `removeItem`, `findItem`), `domain/PantryItem.java`, `domain/Units.java`; GUI `gui/PantryPanel.java` | `PantryTest`, `UnitsTest`, `SmartRecipeFacadeTest.f02…`, `GuiPanelsTest` (pantry) | Implemented (extended: unit conversion when merging) |
| **F03** Recipe search | UC03 / SD02 | `SmartRecipeFacade`, `RecipeRepository` | `facade/SmartRecipeFacade.java` (`searchRecipes`), `service/RecipeRepository.java` (`search`, `findById`, `findAll`), `service/SampleData.java`; GUI `gui/RecipesPanel.java` | `ServicesTest.search…`, `SmartRecipeFacadeTest.f03…`, `GuiPanelsTest` (search) | Implemented |
| **F04** AI recommendation | UC04 / SD03 | `SmartRecipeFacade`, `MealPlanningAgent`, `RecipeRepository`, `RecipeSelectionStrategy`, `AIModel`, `ConstraintValidator`, `ValidationResult` | `agent/MealPlanningAgent.java` (`recommend`), `agent/RequestConstraints.java`, `strategy/*`, `ai/*` (`AIModel` and its four implementations, `PromptFormat`), `agent/ConstraintValidator.java`, `agent/ValidationResult.java`; GUI `gui/AssistantPanel.java` | `MealPlanningAgentTest.recommend…`, `RequestConstraintsTest`, `ConstraintValidatorTest`, `StrategyTest`, `AIModelTest` | Modified (request constraints enforced; more AI providers; see §6) |
| **F05** AI weekly plan | UC05 / SD04 | as F04, plus `MealPlan`, `PlannedMeal` | `agent/MealPlanningAgent.java` (`createPlan`), `plan/MealPlan.java`, `plan/PlannedMeal.java`, `plan/PlanComponent.java`, `plan/Days.java`; GUI `gui/AssistantPanel.java` | `MealPlanningAgentTest.createPlan…`, `MealPlanTest`, `ConstraintValidatorTest.plan…`, `GuiPanelsTest.createEditAndUndo…` | Implemented (plus request constraints, as F04) |
| **F06** Edit plan and undo | UC06 / SD05 | `SmartRecipeFacade`, `MealPlanningAgent`, `AIModel`, `ConstraintValidator`, `ReplaceMealCommand`, `CommandHistory`, `MealPlan` | `agent/MealPlanningAgent.java` (`editPlan` returns a `Command`), `command/Command.java`, `command/ReplaceMealCommand.java`, `command/CommandHistory.java`, `facade/SmartRecipeFacade.java` (`editMealPlan`, `undoLastChange`) | `MealPlanningAgentTest.editPlan…`, `CommandHistoryTest`, `SmartRecipeFacadeTest.newPlanClearsUndoHistory`, `GuiPanelsTest.createEditAndUndo…` | Implemented (extended: AI picks the day if the request names none) |
| **F07** Serving size | UC07 / SD06 | `SmartRecipeFacade`, `ServingScaler`, `Recipe` | `service/ServingScaler.java` (`scale`), `domain/Recipe.java` (immutable, returns copies) | `ServicesTest.scaler…`, `SmartRecipeFacadeTest.f04ToF10FullWorkflow` | Implemented |
| **F08** Substitution | UC08 / SD06 | `SmartRecipeFacade`, `SubstitutionService`, `Substitution`, `UserProfile` | `service/SubstitutionService.java` (`findSubstitute`, `applySubstitution`), `domain/Substitution.java`, `domain/DietRules.java` | `ServicesTest.substitution…`, `DietRulesTest`, `GuiPanelsTest.substitutionErrors…` | Implemented |
| **F09** Nutrition | UC09 / SD06 | `SmartRecipeFacade`, `NutritionService`, `NutritionInfo`, `PlanComponent`, `MealPlan`, `PlannedMeal` | `service/NutritionService.java` (`calculateRecipe`, `calculatePlan`), `domain/NutritionInfo.java`, `plan/*` (`getNutrition`) | `ServicesTest.nutrition…`, `MealPlanTest.compositeSums…` | Implemented |
| **F10** Shopping list | UC10 / SD06 | `SmartRecipeFacade`, `ShoppingListService`, `ShoppingList`, `ShoppingItem`, `Pantry`, `PlanComponent` | `service/ShoppingListService.java` (`generate`), `domain/ShoppingList.java` (`addItem`, `consolidate`), `domain/ShoppingItem.java` | `ServicesTest.shoppingList…`, `ShoppingListTest`, `GuiPanelsTest.shoppingList…` | Implemented |
| GUI and CLI (Appendix B) | — | `MealPlannerGUI`, `MealPlannerCLI` | `presentation/MealPlannerGUI.java`, `presentation/gui/*` (4 tab panels + helpers), `presentation/MealPlannerCLI.java`, `app/SmartPlateApp.java` | `GuiPanelsTest` (15), `MealPlannerCLITest` | Modified (screens grouped into 4 tabs; see §6) |
| AI model (Adapter, §1.5) | — | `AIModel`, `AIAdapter`, `AIClient`, `MockAIModel` | `ai/AIModel.java`, `ai/AIAdapter.java`, `ai/AIClient.java`, `ai/MockAIModel.java`, plus `ai/OllamaAdapter.java`, `ai/GeminiAdapter.java`, `ai/AIModelFactory.java`, `ai/EnvFile.java` | `AIModelTest`, `AIModelFactoryTest`, `OllamaAdapterTest`, `GeminiAdapterTest`, `EnvFileTest` | Modified (extended: two more providers, configuration, run-time switching) |

### 5.2 By designed class

Every class and method in the Stage 1 class diagram exists in the code with the designed name and
role. Paths are under `src/main/java/com/smartplate/`.

| Designed class (diagram) | File | Designed methods found | Status |
|---|---|---|---|
| `SmartRecipeFacade` «Facade» | `facade/SmartRecipeFacade.java` | all 12 feature methods | Implemented (plus query methods, §6) |
| `MealPlanningAgent` | `agent/MealPlanningAgent.java` | `setStrategy`, `recommend`, `createPlan`, `editPlan` | Implemented (plus `setAIModel`, trace) |
| `ConstraintValidator`, `ValidationResult` | `agent/` | `validateRecipe`, `validatePlan`, `isValid` | Implemented |
| `RecipeSelectionStrategy` «Strategy», `PantryFirstStrategy`, `FastestMealStrategy` | `strategy/` | `selectRecipes` | Implemented |
| `AIModel` «target», `AIAdapter` «adapter», `AIClient` «adaptee», `MockAIModel` | `ai/` | `generate`, `sendRequest` | Implemented (`sendRequest` signature changed, §6) |
| `Command` «Command», `ReplaceMealCommand`, `CommandHistory` «Invoker» | `command/` | `execute`, `undo`, `canUndo` | Implemented |
| `PlanComponent` «Component», `MealPlan` «Composite», `PlannedMeal` «Leaf» | `plan/` | `getNutrition`, `getIngredients`, `getCookingTime`, `addMeal`, `removeMeal`, `replaceMeal`, `getMeal` | Implemented |
| `NutritionService`, `ServingScaler`, `SubstitutionService`, `ShoppingListService`, `RecipeRepository` | `service/` | `calculateRecipe`, `calculatePlan`, `scale`, `findSubstitute`, `applySubstitution`, `generate`, `search`, `findById`, `findAll` | Implemented |
| `UserProfile`, `Pantry`, `PantryItem`, `PantryAction`, `Recipe`, `RecipeIngredient`, `Ingredient`, `NutritionInfo`, `ShoppingList`, `ShoppingItem`, `Substitution` | `domain/` | `validate`, `addItem`, `updateItem`, `removeItem`, `findItem`, `addItem`, `consolidate` | Implemented |
| `MealPlannerGUI`, `MealPlannerCLI` | `presentation/` | `start`, `runLoop` | Implemented (GUI split into panels, §6) |

Classes added during implementation (not in the diagram) are listed with their reasons in §6.

---

## 6. Design changes: original → change → reason

None of these changes alters the five patterns or the order of the sequence diagrams; most add
detail the diagrams left open. The ones the team asked for are marked **(team)**.

| # | Original design | Change | Reason |
|---|---|---|---|
| 1 | The request text is passed to the AI; the validator checks the **profile** only (F04, F05). | `RequestConstraints` reads constraints written in the request ("vegetarian", "avoid mushrooms", "under 30 minutes", "5 dinners") and `UserProfile.withRequestConstraints` adds them to a copy of the profile used for shortlisting and validation. | If the request's constraints were only in the prompt, an LLM could ignore them and the validator would not notice. Making them hard constraints keeps "the AI chooses, deterministic code checks" true for the request too, as in the report's example request. |
| 2 | `AIClient.sendRequest(prompt : String) : String`. | `sendRequest(MessageCreateParams) : Message` (the Anthropic SDK's own types). | With strings on both sides, `AIAdapter` would have nothing to adapt. Using the SDK's types makes `AIClient` a real adaptee whose interface differs from `AIModel`, which is what the Adapter pattern describes. |
| 3 | One external LLM behind `AIAdapter → AIClient`; "the provider can change without touching `MealPlanningAgent`" (§1.5). | Two more adapters, `OllamaAdapter` (local model) and `GeminiAdapter` (Google), plus `AIModelFactory`, which picks one from `SMARTPLATE_AI_PROVIDER`. **(team)** | The team wanted a free local model and had a Gemini key. This is the extension the Stage 1 design planned for: the agent did not change. The factory was written by the team. |
| 4 | The AI model is fixed when the app starts. | `MealPlanningAgent.setAIModel`, `SmartRecipeFacade.setAIProvider` and an **AI model** box in the GUI switch models while the app runs. **(team)** | Lets the team compare models in a demo. The GUI still only talks to the facade, so the layering rule from §1.7 holds. |
| 5 | "The key from an environment variable" (§1.5). | `EnvFile` also reads a git-ignored `.env` file; real environment variables still take priority. `.env.example` documents the settings. **(team)** | Setting variables in every terminal was error-prone for the team, and keys had to stay out of git. |
| 6 | `AIModel.generate(prompt)` with free-form text. | `PromptFormat` defines a plain-text `KEY: value` protocol (TASK, CANDIDATES, EXCLUDE, PREVIOUS_VIOLATIONS, RESPONSE_FORMAT …) shared by the agent, the mock and every real model. | The agent has to parse the answer reliably, and the mock must speak the same format so the tests exercise the real parsing code. |
| 7 | Validation "using each recipe's ingredient list". | `DietRules` (ingredient-level matching for allergies, dislikes and diets, including plurals and food groups) and `Units` (unit conversion) are shared domain helpers. | The validator, the strategies, the substitution service and the pantry all need the same rules; one place avoids inconsistent checks. |
| 8 | Ten GUI screens (Appendix B). | Four tabs: AI Assistant (recommend, plan, edit, undo, plan nutrition, shopping list), Recipes (search, servings, substitution, recipe nutrition), Pantry, Preferences. `MealPlannerGUI` builds them from `presentation/gui/*`. **(team)** | The team chose the "Clean Slate" design (CL-03). Every screen from Appendix B still exists, grouped by task; splitting the panels keeps each class small. |
| 9 | CLI commands written "conceptually" (`recipe search`, `plan create` …). | Short commands (`search`, `recommend`, `plan`, `edit`, `undo`, `scale`, `substitute`, `nutrition`, `shopping`) plus `strategy` and `trace`. | Shorter to type. `strategy` exposes the Strategy pattern and `trace` shows the agent's steps. |
| 10 | `SmartRecipeFacade` shows only `currentPlan` and the 12 feature methods. | The facade also owns the profile, pantry and last substitution, and offers read methods (`getProfile`, `getPantry`, `getCurrentPlan`, `getRecipe`, `getStrategyName`, `getLastAgentTrace`, …). | The UIs need to display state and must not reach past the facade. |
| 11 | `editPlan(request, …)` with a day in the request. | If the request names no day, the AI also chooses which day to change (`DAY:` and `RECIPE:` lines), and the day is checked. | Requests like "swap the shrimp dish for something vegetarian" are natural for an agent; the chosen day and recipe are still validated. Following SD05, an edit gets one AI attempt and no revision. |
| 12 | `CommandHistory.undoStack : Stack<Command>`. | `ArrayDeque<Command>` used as a stack. | Java's documented recommendation; same behaviour. |
| 13 | Errors appear as "error" returns in the diagrams. | `SmartPlateException`, `InvalidProfileException` and `AIServiceException` carry a message the UI can show. | Both UIs show the same message, and the GUI shows it next to the field it is about. |
| 14 | Not specified. | `Days` (day names), `SampleData` (30 recipes, a starter pantry, 33 substitutions), `SmartPlateApp` (entry point), `MealPlanningAgent.MAX_CANDIDATES = 15`. | Needed to run the system; the 15-candidate limit keeps prompts short. |

---

## 7. How the AI's output was checked

* **112 automated tests** (JUnit 5, all offline). Run with `mvn test`. They cover every feature and
  pattern, the agent's revision loop with scripted bad AI answers, every AI adapter against fake
  servers, and every GUI tab without opening a window. Testing in detail is the subject of Stage 3.
* **Running the app:** the packaged jar was run through the CLI and the GUI (on a virtual display)
  after each change; GUI screenshots are in [`screenshots/`](screenshots/). The team ran the app
  locally with the `gemma4:e4b` Ollama model.
* **Bugs found by checking and fixed:** see CL-04 and CL-09.
* **Not yet verified:** the real Claude API and the real Gemini API have not been called by the AI
  tool (no keys were available to it). _[Team]_ Record here whether you ran them with your keys.
* **Known limitations:** [`DEVELOPER_GUIDE.md` §14.3](DEVELOPER_GUIDE.md#143-known-issues-and-limitations-good-first-fixes).
* The team's review record is in [`AI_USAGE.md` §4](AI_USAGE.md#4-team-review-record-to-be-completed-by-the-team).
