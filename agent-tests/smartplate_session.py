"""Drives one SmartPlate session through the Java app's --json mode.

Each test case gets its own Java process, so cases never share a profile, pantry or plan.
The process keeps its state between requests, which is what lets a case create a plan and
then edit or undo it.
"""
from __future__ import annotations

import json
import os
import queue
import subprocess
import threading
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
JAR = REPO_ROOT / "target" / "smartplate-1.0.0.jar"


class SessionError(RuntimeError):
    """The Java process died, timed out or wrote something that is not JSON."""


class SmartPlateSession:
    """One running `java -jar smartplate.jar --json --empty` process."""

    def __init__(self, provider: str | None, extra_env: dict[str, str] | None = None,
                 reply_timeout: float = 300.0):
        if not JAR.is_file():
            raise SessionError(f"{JAR} not found: run 'mvn package' in {REPO_ROOT} first.")
        env = dict(os.environ)
        if provider:
            # the real environment wins over .env (see EnvFile), so this picks the model under test
            env["SMARTPLATE_AI_PROVIDER"] = provider
        env.update(extra_env or {})
        self.reply_timeout = reply_timeout
        # started from the project folder so the app finds .env (API keys, OLLAMA_MODEL)
        self.process = subprocess.Popen(
            ["java", "-jar", str(JAR), "--json", "--empty"],
            cwd=REPO_ROOT, env=env, text=True, encoding="utf-8",
            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        self._lines: queue.Queue[str | None] = queue.Queue()
        threading.Thread(target=self._read_stdout, daemon=True).start()
        self.stderr: list[str] = []
        threading.Thread(target=self._read_stderr, daemon=True).start()

    def _read_stdout(self) -> None:
        for line in self.process.stdout:
            self._lines.put(line)
        self._lines.put(None)

    def _read_stderr(self) -> None:
        for line in self.process.stderr:
            self.stderr.append(line.rstrip())

    def send(self, request: dict) -> dict:
        """Sends one request and returns the parsed reply, plus the wall-clock time in ms."""
        started = time.monotonic()
        try:
            self.process.stdin.write(json.dumps(request) + "\n")
            self.process.stdin.flush()
        except (BrokenPipeError, OSError) as e:
            raise SessionError(f"SmartPlate is not running: {e}. stderr: {self._stderr_tail()}") from e
        try:
            line = self._lines.get(timeout=self.reply_timeout)
        except queue.Empty as e:
            raise SessionError(f"No reply within {self.reply_timeout:.0f} s.") from e
        if line is None:
            raise SessionError(f"SmartPlate exited. stderr: {self._stderr_tail()}")
        try:
            reply = json.loads(line)
        except json.JSONDecodeError as e:
            raise SessionError(f"SmartPlate wrote a line that is not JSON: {line[:200]!r}") from e
        reply["wallMs"] = round((time.monotonic() - started) * 1000)
        return reply

    def _stderr_tail(self) -> str:
        return " | ".join(self.stderr[-5:]) or "(empty)"

    def close(self) -> None:
        if self.process.poll() is None:
            try:
                self.process.stdin.close()
                self.process.wait(timeout=10)
            except (OSError, subprocess.TimeoutExpired):
                self.process.kill()

    def __enter__(self) -> "SmartPlateSession":
        return self

    def __exit__(self, *exc) -> None:
        self.close()
