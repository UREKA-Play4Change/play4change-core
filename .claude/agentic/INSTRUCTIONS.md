# Play4Change — Human Operator Guide

This document is for the humans. Three operators share Claude Code Pro accounts and pick up each
other's sessions. This file tells you how to work with the AI safely and hand off cleanly.

---

## 1. What This Folder Is and Why It Exists

`agentic/` is the cross-session operating system for all Claude Code work on Play4Change.

Every Claude Code session reads `AI.md` first, before touching code. Every planning decision
that is not obvious from the code is recorded here. Every hack, risk, and deviation lives here.

Without this folder, each new session starts blind. With it, any operator — or a new Claude
session — can orient in under two minutes.

**The folder is not documentation. It is the operating state of the project.**

---

## 2. How to Start a Fresh Claude Code Session

Run these commands in order at the start of every session, before asking the AI to do anything:

```bash
# Step 1 — confirm you are on the right branch or create a task branch
git status
git log --oneline -5

# Step 2 — tell the AI to orient
# Type this in the Claude Code prompt:
# "Read agentic/AI.md §10 Fresh session orient checklist and follow it exactly."

# Step 3 — after orient, assign the task
# "We are working on Phase 02, Task 2.3. Read agentic/phases/phase-02-content-generation.md
#  and implement task 2.3."
```

**Do not skip the orient step.** The AI will not know which phase is active, what is already
done, or what security constraints apply without it.

---

## 3. How to Hand Off to Another Operator Mid-Phase

Before ending your session:

1. **Commit all in-progress work** — even if the tests are red. Commit message must describe
   what is done and what is not:
   ```
   wip(phase-02): task 2.3 partial — prompt engineering done, sanitisation pending
   ```

2. **Update agentic/ISSUES.md** if you discovered a problem that is not yet fixed.

3. **Update the task checklist** in the relevant `agentic/phases/phase-XX-*.md` file —
   check completed items, add a note under in-progress items describing the state.

4. **Write a handoff message** in your team channel with this structure:
   ```
   HANDOFF — Phase 02, Task 2.3
   Branch: feat/phase-02-multilang
   Done: Mistral prompt rewritten for target language, output schema defined
   Not done: jsoup sanitisation, schema validation, unit tests for sanitiser
   Blocker: none
   Next step: implement HtmlSanitiser.kt, then run ./gradlew :server:test
   ```

5. **Do not push unfinished work to main.** The branch stays open. The next operator picks
   up the branch and continues.

---

## 4. The Golden Loop in Plain English

The AI operates in a fixed ten-step loop for every task. Here is what each step means for you
as a human:

1. **Orient** — the AI reads this folder and the current git state. It must do this first.
2. **Branch** — the AI creates or switches to a task branch. Main is never touched directly.
3. **Red test** — the AI writes a failing test that proves the feature does not yet exist.
   You will see test output with failures. This is correct.
4. **Implement** — the AI writes code to make the test pass.
5. **Refactor** — the AI cleans up the code without changing behaviour.
6. **Verify** — the AI runs the full test suite. Everything must be green before continuing.
7. **Log** — the AI updates `agentic/` files: HACKS.md if it found a hack, THREAT-LOG.md
   if there is a security note, DECISIONS.md if it made a non-obvious choice.
8. **Commit** — the AI commits with a Conventional Commit message. It does not push.
9. **PR** — the AI opens a pull request. It does not merge.
10. **STOP** — the AI stops and tells you the PR is open. **You review and merge.**

If the AI skips any step, tell it to stop and restart the loop from where it deviated.

---

## 5. PR and Merge Rules

| Rule | Who |
|------|-----|
| Opens PR | AI |
| Reviews PR | Human operator |
| Merges PR | Human operator |
| Pushes to main directly | Nobody — ever |
| Approves their own PR | Nobody — ever |

The AI will never merge its own PR. If it tries, tell it to stop.

After you merge, tell the AI: "PR merged. Mark task X.Y as done in the phase file."

---

## 6. How to Manually Test Each Phase

Each phase file (`agentic/phases/phase-XX-*.md`) ends with a **Human Checkpoint** section
containing:
- Exact `curl` commands to run against the local Docker Compose stack
- Expected HTTP status codes and response shapes
- UI actions to perform if the phase touches the admin web or mobile client
- What "pass" looks like and what "fail" looks like

All manual test recipes are also saved in `agentic/manual-testing/` when the AI writes them.
Run the recipe after the AI opens the phase PR, before you merge.

To start the local stack for testing:
```bash
docker compose up --build
# wait for health check
curl http://localhost:8080/actuator/health
# expect: {"status":"UP"}
```

---

## 7. What to Do If the AI Goes Off-Script

If the AI does something you did not ask for — refactors unrelated code, opens an extra PR,
makes an architectural decision without telling you — do the following:

1. **Stop the session immediately.** Type: "Stop. Do not continue."
2. **Revert the change** if it has not been committed:
   ```bash
   git checkout -- .
   ```
3. **If it was committed but not pushed:** reset to the previous commit:
   ```bash
   git reset HEAD~1
   ```
4. **Record the deviation** in `agentic/ROADMAP_CHANGES.md` so future sessions know it happened.
5. Restart the session with a more precise instruction. Reference the specific task number.

If the AI proposes a major architectural change not in the roadmap, tell it to write a
DECISIONS.md entry instead and wait for your approval before implementing.

---

## 8. How to Mark a Phase as Done

When all tasks in a phase are checked off and the final PR is merged:

1. Open `agentic/ROADMAP.md`.
2. Change the phase status from `IN PROGRESS` to `DONE`.
3. Add the completion date next to the status.
4. Commit directly on main (this is the one exception to the no-direct-push rule — it is
   an administrative update, not a code change):
   ```bash
   git add agentic/ROADMAP.md
   git commit -m "chore(agentic): mark phase 01 as DONE [2026-05-01]"
   ```

---

## 9. The Three Hard Rules

These are not guidelines. They are rules. If the AI violates them, stop the session.

**Rule 1 — Never merge your own PR.**
The human reviews and merges. Always.

**Rule 2 — Never push to main directly.**
All changes go through a branch and a PR. The only exception is ROADMAP.md phase-status
updates as described above.

**Rule 3 — Never skip the orient step.**
Every session begins with: "Read agentic/AI.md §10 and follow it exactly."
No exceptions. A session that skips orient will make decisions without context and create
problems that cost more to undo than the session saved.
