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
| Tests | `src/test/**` | Yes | 74 JUnit 5 tests |
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
| Unit tests | `mvn test` | 74 tests, 0 failures, 0 errors |
| Runnable jar | `mvn package` then `java -jar target/smartplate-1.0.0.jar --cli` | Starts; commands work |
| Scripted CLI session covering F01–F10 | See `MealPlannerCLITest` and the session in `docs/IMPLEMENTATION.md` §1 | Expected output |
| GUI | Launched on a virtual display; "Create weekly plan" clicked | Works; screenshot in `docs/screenshots/` |
| Agent revision behavior | `MealPlanningAgentTest` (scripted bad AI answers) | Invented ids and constraint violations are rejected; exactly one revision; failure reported after that |

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
| `AIAdapter` → `AIClient` → Claude (Anthropic API) | Only when the environment variable `ANTHROPIC_API_KEY` is set | Network call; billed to the key's owner |

No API key is stored in the repository.
