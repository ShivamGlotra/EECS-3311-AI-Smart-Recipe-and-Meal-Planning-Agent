# AI-Smart-Recipe-and-Meal-Planning-Agent

This project is code in java and is a customized AI agent that helps user in deciding their meals and those recipes.

## Setup and run

1. Install **JDK 17 or newer** and **Maven 3.8 or newer** (check with `java -version` and `mvn -v`).
2. From the project folder, run the tests: `mvn test` (112 tests, offline, no API key needed).
3. Build and start the desktop app: `mvn package`, then `java -jar target/smartplate-1.0.0.jar`.
   Add `--cli` for the command-line version, `--mock` to force the offline AI, `--empty` to start with an empty pantry.
4. *Optional, real AI:* copy `.env.example` to `.env` (git-ignored, so keys are never pushed) and fill in
   `ANTHROPIC_API_KEY`, `GEMINI_API_KEY` and/or `OLLAMA_MODEL` for a local Ollama model. Start the app from
   the project folder and pick the model in the **AI model** box at the top right of the window.
   Without a `.env` the app uses the offline Mock AI.
5. *VS Code:* the same commands are ready-made tasks. Use *Terminal → Run Task…* → `SmartPlate: …`,
   or the **Task Runner** view at the bottom of the Explorer (accept the recommended extensions when
   VS Code offers them). Press F5 to run with the debugger.

## Documents

- Stage 1 design report: [SmartPlate_Stage1_Report_FINAL.md](SmartPlate_Stage1_Report_FINAL.md) (UML in [docs/umlet-diagrams/](docs/umlet-diagrams/))
- Stage 2 report (AI tools, collaboration log, traceability, design changes): [docs/STAGE2_REPORT.md](docs/STAGE2_REPORT.md)
- Code explanation: [docs/IMPLEMENTATION.md](docs/IMPLEMENTATION.md)
- Developer guide (structure, every function, how to change and debug): [docs/DEVELOPER_GUIDE.md](docs/DEVELOPER_GUIDE.md)
- Where AI helped (disclosure for the instructor): [docs/AI_USAGE.md](docs/AI_USAGE.md)
