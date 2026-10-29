# Stage 2 diagrams (implemented system)

These diagrams describe the system as implemented in Stage 2. The Stage 1 design diagrams are unchanged
in [`../umlet-diagrams/`](../umlet-diagrams/), so the two can be compared.

| Diagram | Image | Source | Change from Stage 1 |
|---|---|---|---|
| Class diagram | [PNG](SmartPlate_Stage2_ClassDiagram.png) | [class-diagram.puml](class-diagram.puml) | Updated: classes added during implementation are shown in blue |
| Use-case diagram | [PNG](SmartPlate_Stage2_UseCase.png) | [use-case-diagram.puml](use-case-diagram.puml) | Updated: new UC11 Select AI Model |
| SD03 AI recommendation (F04) | [PNG](SmartPlate_Stage2_SD03_Recommendation.png) | [SD03-recommendation.puml](SD03-recommendation.puml) | Updated: request constraints, 15-candidate shortlist, "no candidates" branch |
| SD04 AI weekly plan (F05) | [PNG](SmartPlate_Stage2_SD04_MealPlan.png) | [SD04-meal-plan.puml](SD04-meal-plan.puml) | Updated: request constraints and the 5-7 day check |
| SD05 Edit plan and undo (F06) | [PNG](SmartPlate_Stage2_SD05_EditUndo.png) | [SD05-edit-undo.puml](SD05-edit-undo.puml) | Updated: day found in the request (`Days.findIn`) or chosen by the AI |
| SD07 Select AI model (F11) | [PNG](SmartPlate_Stage2_SD07_SelectAIModel.png) | [SD07-select-ai-model.puml](SD07-select-ai-model.puml) | New |

SD01 (preferences and pantry), SD02 (recipe search) and SD06 (recipe utilities) still match the
implementation, so the Stage 1 versions remain current.

To regenerate the PNGs, use PlantUML 1.2024.7 or newer (no Graphviz needed; the class and use-case
diagrams use the built-in `smetana` layout):

```bash
java -jar plantuml.jar -tpng docs/stage2-diagrams/*.puml
```
