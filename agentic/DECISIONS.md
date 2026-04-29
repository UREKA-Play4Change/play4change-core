# Play4Change — Design Decisions Log

## Format

Each entry records a non-obvious design choice that does not warrant a full ADR.
Use this file when: the decision is tactical, affects one feature, is temporary,
or is obviously correct in hindsight with no realistic alternative.

For architectural decisions (new technology, new boundary, major security mechanism,
hard-to-reverse choices) — write a full ADR in `docs/adr/` instead.

**Entry format:**
```
## [YYYY-MM-DD] [Scope] — [Short title]

**Context:** Why this decision was needed.
**Decision:** What was decided.
**Why not [alternative]:** Why the obvious alternative was rejected.
**Phase:** Which phase introduced this.
```

---

## [2026-04-29] [agentic] — Adopt agentic/ operating system for shared sessions

**Context:** Three operators share Claude Code Pro accounts on this project with a 2–3 week
deadline. Without a shared operating system, each session starts blind: the AI does not know
which phase is active, what security decisions have been made, what hacks exist, or what
the handoff state is. ADR culture is already established in the project (ADR-001 through ADR-016).
The agentic/ folder extends that culture to the AI session layer.

**Decision:** Create an `agentic/` folder at the repository root containing: operating rules
for the AI (AI.md), a human operator guide (INSTRUCTIONS.md), a phased roadmap (ROADMAP.md),
logging files for security (THREAT-LOG.md), hacks (HACKS.md), issues (ISSUES.md), and decisions
(this file). Every Claude Code session reads AI.md §10 first.

**Why not rely on git log and ADRs alone:** Git log describes what changed but not why
a session stopped, what is in progress, or what constraint applies to the next task.
ADRs capture architectural decisions but not the tactical operating state.
The agentic/ system fills the gap between those two.

**Why not a JIRA board or external tool:** The session must be able to orient from the
repository alone, without external service access. A markdown folder in the repo is always
available to any Claude Code session.

**Phase:** 01 (this is the Phase 01 deliverable)

---

*(New entries are prepended above this line — most recent first)*
