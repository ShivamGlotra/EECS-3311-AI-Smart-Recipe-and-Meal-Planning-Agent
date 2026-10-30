# Stage 3 agent tests (KUMA)

Behavioural tests for SmartPlate's AI agent (F04 recommend, F05 plan, F06 edit/undo), run with
[KUMA](https://github.com/DefuzeX-AI/KUMA-DefuzeX) (`kuma-defuzex`) **fully locally**: no KUMA
account, no KUMA API key, nothing is uploaded. The only network traffic is to the model you test
(none for the Mock AI or a local Ollama model).

The requirements (BR-01..BR-09) and every test case (BT-01..BT-30) are listed in
[docs/STAGE3_TEST_DESIGN.md](../docs/STAGE3_TEST_DESIGN.md).

## How it works

```
run_kuma.py ──► KUMA create_run (custom Case Provider, custom Judge, allow_local=True)
   │  for each step: run.get_input()  ──►  smartplate_session.py  ──►  java -jar smartplate.jar --json
   │                 run.submit(reply) ◄──  one JSON reply per line  ◄──  SmartRecipeFacade → agent → LLM
   └─ after the last step KUMA calls the judge: cases.py (expectations) + checks.py (independent rules)
```

| File | Role |
|---|---|
| `cases.py` | Requirements BR-01..BR-09 and the 30 cases: steps, context, expected / prohibited behaviour, evaluation |
| `checks.py` | The judge's own food and plan rules (deliberately **not** the agent's `ConstraintValidator`) |
| `smartplate_session.py` | Starts one `--json` Java session per case and exchanges JSON lines with it |
| `run_kuma.py` | Creates one KUMA Run per case, drives it, writes the results |
| `agent-profile.md` | The KUMA Agent Profile (scenario, behaviours, prohibited behaviours) |
| `make_design_doc.py` | Regenerates `docs/STAGE3_TEST_DESIGN.md` from `cases.py` |
| `results/<model>-<time>/` | `summary.md` (tables + details of failures), `results.json`, one transcript per case |

Each case runs in its own Java process, so cases never share a profile, pantry or plan. Within a
case the process keeps its state, which is how plan → edit → undo is tested.

## Run it

Needs Python 3.10+, the JDK and Maven (as for the app). From the project folder:

```bash
mvn package                                   # builds target/smartplate-1.0.0.jar
python3 -m venv .venv                         # once (Windows: py -m venv .venv)
source .venv/bin/activate                     # Windows PowerShell: .venv\Scripts\Activate.ps1
python -m pip install -r agent-tests/requirements.txt

python agent-tests/run_kuma.py --provider mock      # offline check of the harness (~15 s)
python agent-tests/run_kuma.py --provider ollama    # local model from .env (OLLAMA_MODEL), Ollama running
python agent-tests/run_kuma.py                      # whatever SMARTPLATE_AI_PROVIDER in .env says
```

Options: `--only BT-04,BT-15` (some cases), `--repeat 3` (each case three times: LLM answers
vary, so the summary then shows e.g. `pass, issue, pass`), `--timeout 300` (seconds per reply),
`--label name` (results folder name).

Statuses: **pass**; **issue** (the judge found at least one problem: *major* for safety,
structure, grounding or failure handling, *minor* for relevance, variety or message wording);
**insufficient_evidence** (the harness itself failed, e.g. the jar is missing or Java crashed).

BT-22 always uses the Mock AI for its first step and then switches to an unreachable Ollama
address on purpose: it tests what the agent does when the AI service goes down.

## Notes

- The `.env` file is read by the Java app as usual; keys never pass through the test code.
- KUMA 0.3.9 rejects any structured input schema after parsing it, so each KUMA input is the
  exact JSON line the app reads (`input_type: text`).
