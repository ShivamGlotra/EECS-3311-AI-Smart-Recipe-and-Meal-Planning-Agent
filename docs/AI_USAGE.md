# AI Assistance Disclosure — SmartPlate (EECS 3311)

This document states where an AI tool helped produce the SmartPlate implementation. It keeps two
different uses of AI apart:

1. **AI as a development tool:** an AI coding assistant helped write this project's code and documents.
2. **AI as a component of the product:** SmartPlate itself calls an LLM at run time (the `AIModel`
   abstraction from the design report). That is part of the required design, not help with the coursework.

---

## 1. Tool used

| Item | Detail |
|---|---|
| Tool | **Claude Code**, Anthropic's AI coding assistant, used through claude.ai/code |
| Used for | Generating the Java implementation, the unit tests, the sample data and the documentation in this branch |
| Input given to the tool | The team's **Stage 1 design report and UML diagrams** (branch `stage-1Fix`: `README.md`, `docs/uml-diagrams/**`), plus the instruction to implement every feature as described in the report, explain the code, and document the AI assistance |
| Where its output lives | Every file added in the Stage 2 implementation commit: `pom.xml`, `src/**`, `docs/IMPLEMENTATION.md`, `docs/AI_USAGE.md`, `docs/screenshots/**`, and the short "Implementation" section at the top of `README.md` |

The Stage 1 report (requirements, features F01–F10, use cases, class and sequence diagrams, and the
choice of the five design patterns) was the **input** to the tool. This disclosure does not cover
how that report was written.

---

## 2. What the AI produced

| Area | Files | Produced by AI? | Notes |
|---|---|---|---|
| Build setup | `pom.xml` | Yes | Maven, Java 17, JUnit 5, Anthropic Java SDK, runnable jar |
| Domain layer | `domain/*` | Yes | Follows the report's class diagram. Added helpers: `Units`, `DietRules`, exceptions |
| Composite | `plan/*` | Yes | `PlanComponent`, `MealPlan`, `PlannedMeal` as in the diagram |
| Adapter / AI | `ai/*` | Yes | `AIModel`, `AIAdapter`, `AIClient` (official SDK), `MockAIModel`, `PromptFormat` |
| Agent | `agent/*` | Yes | Retrieve → strategy → AI → validate → revise once → return/report loop, from report §1.4 and SD03–SD05 |
| Strategy | `strategy/*` | Yes | Pantry-first and Fastest strategies |
| Command | `command/*` | Yes | `ReplaceMealCommand`, `CommandHistory` |
| Services and data | `service/*` | Yes | Includes the 30 sample recipes, starter pantry and 33 substitutions |
| Facade | `facade/SmartRecipeFacade.java` | Yes | One method per feature, as in the diagram |
| User interfaces | `presentation/*`, `app/*` | Yes | Swing GUI and CLI; both use only the facade |
| GUI redesign ("Clean Slate", light mode) | `presentation/MealPlannerGUI.java`, `presentation/gui/**`, FlatLaf in `pom.xml` | Yes | Claude Code proposed three design directions as mockups; **the team chose Direction B**, and Claude Code then implemented it in Swing. The IBM Plex font files are IBM's (SIL Open Font License), not AI-generated. |
| AI provider choice | `ai/AIModelFactory.java`, `app/SmartPlateApp.java` (`chooseModel` and the error handling in `main`) | **No** (written by the team) | The team wrote the factory that picks mock / anthropic / ollama from `SMARTPLATE_AI_PROVIDER`; Claude Code added it to the project unchanged |
| Local model adapter | `ai/OllamaAdapter.java` | Yes | Written by Claude Code to the constructor and constants the team's factory expects |
| Tests | `src/test/**` | Yes | 99 JUnit 5 tests (14 of them for the GUI; 11 for the factory and the Ollama adapter) |
| Documentation | `docs/IMPLEMENTATION.md`, this file | Yes | Code explanation and this disclosure |

**Sample data caveat:** the recipes, quantities and nutrition values in `SampleData.java` were made
up by the AI for demonstration. They were **not** checked against a nutrition database and should
not be treated as accurate dietary information.

---

## 3. How the AI's output was checked

These checks were run during generation and can be repeated by anyone:

| Check | How to repeat | Result at time of writing |
|---|---|---|
| Compiles | `mvn compile` | Success |
| Unit tests | `mvn test` | 99 tests, 0 failures, 0 errors |
| Runnable jar | `mvn package` then `java -jar target/smartplate-1.0.0.jar --cli` | Starts; commands work |
| Scripted CLI session covering F01–F10 | See `MealPlannerCLITest` and the session in `docs/IMPLEMENTATION.md` §1 | Expected output |
| GUI | Launched on a virtual display and driven through every state (empty, error, plan, edit, shopping list, nutrition, recipes, pantry error, preferences error) | Works; screenshots in `docs/screenshots/`. One bug was found by the new GUI tests and fixed (the pantry "Added …" message was cleared by the row selection). |
| Agent revision behavior | `MealPlanningAgentTest` (scripted bad AI answers) | Invented ids and constraint violations are rejected; exactly one revision; failure reported after that |

**Ollama adapter:** tested against a fake Ollama server that answers `/api/chat` in Ollama's JSON
format (`OllamaAdapterTest`), and the packaged jar was run against such a fake server. It was **not**
run against a real local model while generating the code; the team should confirm it once with their own model.

**Not verified:** the live call to the real Claude API (`AIAdapter` → `AIClient`). No API key was
available while generating the code. The request-building code is unit tested
(`AIModelTest.adapterTranslatesThePromptIntoAProviderRequest`), and the no-key error path is
tested, but no real network call was made. Every test and the default run use the offline `MockAIModel`.

---

## 4. Team review record (to be completed by the team)

The AI generated the code, but the team is responsible for understanding and defending it. Please
fill in this section before submitting.

| Team member | Files / features reviewed | Changes made after review | Date |
|---|---|---|---|
| | | | |
| | | | |
| | | | |

Questions worth answering during review (the TA may ask them):

* Can each member explain the agent loop in `MealPlanningAgent` and where each of the five patterns lives?
* Do the added helper classes (`Units`, `DietRules`, `RequestConstraints`, `PromptFormat`) need to be
  added to the class diagram for the final submission?
* Are the sample recipes and nutrition values acceptable for the demo?

---

## 5. AI as a component of SmartPlate (run time)

As the design report requires, SmartPlate uses an LLM only for **choosing among existing
recipes** (F04, F05, F06). The LLM never invents recipes or substitutions, and deterministic Java
code (`ConstraintValidator`) checks every answer before the user sees it.

| Mode | When | Network / cost |
|---|---|---|
| `MockAIModel` | Default; always used in tests | None |
| `AIAdapter` → `AIClient` → Claude (Anthropic API) | `SMARTPLATE_AI_PROVIDER=anthropic`, or the default `auto` when `ANTHROPIC_API_KEY` is set | Network call; billed to the key's owner |
| `OllamaAdapter` → a local model through Ollama | Only with `SMARTPLATE_AI_PROVIDER=ollama` | Runs on the user's machine; no cost, nothing leaves the machine with the default URL |

No API key is stored in the repository.
