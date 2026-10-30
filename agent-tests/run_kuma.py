"""Runs SmartPlate's behavioural tests with KUMA, fully locally (no KUMA account or key).

    python agent-tests/run_kuma.py                      # the model chosen in .env
    python agent-tests/run_kuma.py --provider ollama    # local Ollama model (OLLAMA_MODEL in .env)
    python agent-tests/run_kuma.py --provider mock      # offline Mock AI (checks the harness)
    python agent-tests/run_kuma.py --only BT-04,BT-15 --repeat 3

For every case KUMA runs one Run: a custom Case Provider hands KUMA the case's steps, the
harness sends each step to a fresh `java -jar smartplate.jar --json` session and submits the
reply, and a custom Judge (cases.py + checks.py) decides pass or issue. Results go to
agent-tests/results/<model>-<time>/ as results.json, summary.md and one transcript per case.
"""
from __future__ import annotations

import argparse
import json
import sys
import time
from collections import defaultdict
from datetime import datetime
from pathlib import Path

from kuma import __version__ as kuma_version
from kuma import create_run
from kuma.providers import CaseGenerationContext, JudgeContext

from cases import BY_ID, CASES, REQUIREMENTS, Case
from checks import issue
from smartplate_session import REPO_ROOT, SessionError, SmartPlateSession

HERE = Path(__file__).resolve().parent
AGENT_PROFILE = HERE / "agent-profile.md"


def load_catalog() -> dict[str, dict]:
    """The recipe repository, read once through the app itself (no AI involved)."""
    with SmartPlateSession("mock") as session:
        reply = session.send({"action": "catalog"})
    return {r["id"]: r for r in reply["recipes"]}


def kuma_case(case: Case, run_id: str) -> dict:
    """The public KUMA Case: only the inputs. Expectations stay in the judge.

    Each input is the exact JSON line the app reads. (KUMA 0.3.9 rejects every "structured"
    input schema after freezing it into a read-only mapping, so text inputs are used.)
    """
    return {
        "case_id": run_id,
        "input_type": "text",
        "inputs": [{"input_id": f"{run_id}_step{i + 1}", "payload_type": "text", "payload": json.dumps(step)}
                   for i, step in enumerate(case.steps)],
        "extensions": {"requirement": case.requirement, "title": case.title, "kind": case.kind,
                       "context": case.context},
    }


def make_judge(case: Case, catalog: dict, harness_errors: list[str]):
    def judge(context: JudgeContext) -> dict:
        outputs = [item.submission.output for item in context.history]
        if harness_errors or any(o is None for o in outputs):
            return {"status": "insufficient_evidence", "confidence": "high", "stop_reason": "harness_error",
                    "issues": [issue(case.requirement, "major", e) for e in harness_errors]}
        issues = case.evaluate(outputs, catalog)
        return {"status": "issue" if issues else "pass", "confidence": 1.0, "stop_reason": "case_completed",
                "issues": issues, "requirement": case.requirement}
    return judge


def run_case(case: Case, rep: int, args, catalog: dict, out_dir: Path) -> dict:
    run_id = case.id + (f"-r{rep}" if args.repeat > 1 else "")
    transcript = out_dir / "transcripts" / f"{run_id}.jsonl"
    harness_errors: list[str] = []
    replies: list[dict] = []
    provider = case.provider or args.provider
    started = time.monotonic()

    with SmartPlateSession(provider, case.env, args.timeout) as session, \
            transcript.open("w", encoding="utf-8") as log:
        def send(request: dict) -> dict:
            reply = session.send(request)
            log.write(json.dumps({"request": request, "reply": reply}) + "\n")
            log.flush()
            return reply

        # context the case needs (not part of what KUMA delivers)
        try:
            if case.profile:
                setup = send({"action": "profile", **case.profile})
                if not setup.get("ok"):
                    harness_errors.append(f"profile setup failed: {setup.get('error')}")
            for item in case.pantry:
                send({"action": "pantry", **item})
        except SessionError as e:
            harness_errors.append(str(e))

        run = create_run(
            repo_path=REPO_ROOT, agent_profile_path=AGENT_PROFILE,
            case_provider=lambda _ctx: kuma_case(case, run_id),
            judge_provider=make_judge(case, catalog, harness_errors),
            max_steps=len(case.steps), judge=True, on_failure="continue",
            allow_local=True, track_files=False, save_local=False)
        report = None
        try:
            while (line := run.get_input()) is not None:
                step = json.loads(line)
                if harness_errors:
                    report = run.submit(status="aborted", error="harness error", logs=[transcript])
                    continue
                try:
                    reply = send(step)
                except SessionError as e:
                    harness_errors.append(str(e))
                    report = run.submit(status="failed", error="SmartPlate session error", logs=[transcript])
                    continue
                replies.append(reply)
                report = run.submit(reply, logs=[transcript])
        finally:
            if run.state in {"ready", "input_delivered"}:
                run.cancel()

    report = report or run.report
    model = next((r["model"] for r in replies if r.get("model")), provider or "env")
    return {
        "case": case.id, "run": run_id, "requirement": case.requirement, "title": case.title,
        "kind": case.kind, "model": model,
        "status": report.status if report else "insufficient_evidence",
        "issues": [dict(i) for i in report.issues] if report else [],
        "kumaReportId": report.report_id if report else None, "kumaRunId": report.run_id if report else None,
        "seconds": round(time.monotonic() - started, 1),
        "steps": [{"request": s, "ok": r.get("ok"), "error": r.get("error"),
                   "result": [f"{m.get('day', '')}{':' if m.get('day') else ''}{m['id']} {m['name']}"
                              for m in r.get("plan") or r.get("recipes") or []],
                   "aiAnswers": [t for t in r.get("trace", []) if t.startswith("AI answered")],
                   "elapsedMs": r.get("elapsedMs")}
                  for s, r in zip(case.steps, replies)],
        "transcript": str(transcript.relative_to(REPO_ROOT)),
    }


def short(text: str, limit: int = 160) -> str:
    return text if len(text) <= limit else text[:limit - 3] + "..."


def write_summary(results: list[dict], out_dir: Path, args) -> Path:
    model = results[0]["model"] if results else "?"
    passed = sum(r["status"] == "pass" for r in results)
    by_case = defaultdict(list)
    for r in results:
        by_case[r["case"]].append(r)
    lines = [f"# KUMA results: {model}", "",
             f"- Date: {datetime.now():%Y-%m-%d %H:%M}",
             f"- KUMA SDK {kuma_version}, local custom Case Provider and Judge (no KUMA service)",
             f"- Model under test: **{model}** (provider option: `{args.provider or '.env'}`)",
             f"- Runs: {len(results)} ({len(by_case)} cases x {args.repeat}); "
             f"**{passed} pass**, {sum(r['status'] == 'issue' for r in results)} issue, "
             f"{sum(r['status'] == 'insufficient_evidence' for r in results)} insufficient evidence",
             "", "## By requirement", "", "| Requirement | Runs | Pass |", "|---|---|---|"]
    for br, (name, _) in REQUIREMENTS.items():
        runs = [r for r in results if r["requirement"] == br]
        if runs:
            lines.append(f"| {br} {name} | {len(runs)} | {sum(r['status'] == 'pass' for r in runs)} |")
    lines += ["", "## By case", "", "| Test | Req | Kind | Title | Result | Issues found | Time (s) |",
              "|---|---|---|---|---|---|---|"]
    for case_id, runs in by_case.items():
        result = ", ".join(r["status"] for r in runs) if len(runs) > 1 else runs[0]["status"]
        msgs = sorted({f"{i['requirement']} ({i['severity']}): {short(i['message'])}"
                       for r in runs for i in r["issues"]})
        lines.append(f"| {case_id} | {runs[0]['requirement']} | {runs[0]['kind']} | {runs[0]['title']} | "
                     f"{result} | {'<br>'.join(msgs) or '-'} | {sum(r['seconds'] for r in runs):.1f} |")
    lines += ["", "## Details of runs with issues", ""]
    for r in results:
        if r["status"] == "pass":
            continue
        lines += [f"### {r['run']} - {r['title']} ({r['status']})", ""]
        for i, step in enumerate(r["steps"], 1):
            lines.append(f"{i}. `{json.dumps(step['request'])}` -> "
                         + ("ok: " + "; ".join(step["result"]) if step["ok"] else f"error: {step['error']}"))
            for answer in step["aiAnswers"]:
                lines.append(f"   - {answer}")
        lines += [""] + [f"- **{i['requirement']} ({i['severity']})**: {i['message']}" for i in r["issues"]]
        lines += ["", f"Transcript: `{r['transcript']}`", ""]
    path = out_dir / "summary.md"
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--provider", choices=["mock", "ollama", "gemini", "anthropic"],
                        help="model under test (default: SMARTPLATE_AI_PROVIDER from .env)")
    parser.add_argument("--only", help="comma-separated case ids, e.g. BT-04,BT-15")
    parser.add_argument("--repeat", type=int, default=1, help="runs per case (LLM answers vary)")
    parser.add_argument("--timeout", type=float, default=300, help="seconds to wait for one reply")
    parser.add_argument("--label", help="name of the results folder (default: <provider>-<time>)")
    args = parser.parse_args()

    cases = [BY_ID[c.strip().upper()] for c in args.only.split(",")] if args.only else CASES
    out_dir = HERE / "results" / (args.label or f"{args.provider or 'env'}-{datetime.now():%Y%m%d-%H%M%S}")
    (out_dir / "transcripts").mkdir(parents=True, exist_ok=True)

    catalog = load_catalog()
    results = []
    for case in cases:
        for rep in range(1, args.repeat + 1):
            result = run_case(case, rep, args, catalog, out_dir)
            results.append(result)
            print(f"{result['run']:<10} {result['requirement']}  {result['status']:<22} "
                  f"{result['seconds']:>6.1f}s  {result['title']}", flush=True)
            for i in result["issues"]:
                print(f"           - {i['requirement']} ({i['severity']}): {i['message']}", flush=True)

    (out_dir / "results.json").write_text(json.dumps(results, indent=2) + "\n", encoding="utf-8")
    summary = write_summary(results, out_dir, args)
    passed = sum(r["status"] == "pass" for r in results)
    print(f"\n{passed}/{len(results)} runs passed. Summary: {summary.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
