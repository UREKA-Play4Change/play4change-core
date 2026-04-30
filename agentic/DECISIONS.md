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

## [2026-04-30] [ci] — Detekt 2.0.0-alpha.2 instead of 1.23.x; baseline file is detekt-baseline-main.xml

**Context:** Phase 01 Task 1.3 spec says to use Detekt `"1.23.x"`. The project uses Kotlin 2.3.0.
Detekt 1.23.8 (latest stable) was compiled with Kotlin 2.0.21 and hard-fails with "detekt was
compiled with Kotlin 2.0.10/2.0.21 but is currently running with 2.3.0. This is not supported."
There is no stable Detekt release that supports Kotlin 2.3.0.

**Decision:** Use `dev.detekt` (new Maven group) version `2.0.0-alpha.2`, which targets Kotlin 2.3.0
and is published to Maven Central. The plugin ID changed from `io.gitlab.arturbosch.detekt` to
`dev.detekt` in the 2.x series. Detekt 2.x also introduced per-source-set baseline tasks:
`detektBaselineMain` generates `detekt-baseline-main.xml`, which is what `detektMain` reads.
The phase spec's expected filename `detekt-baseline.xml` is not what Detekt 2.x produces for
the main source set; the correct file is `detekt-baseline-main.xml`.

**Why not downgrade Kotlin:** Kotlin 2.x is already required by the Kotlin 2.3.0 DSL features
used throughout the build. Downgrading Kotlin to 2.0.21 would break more than it fixes.

**Why not wait for a stable Detekt 2.x:** No stable 2.x release exists as of 2026-04-30.
The alpha is the only available path for Kotlin 2.3.0 support. The alpha is functionally
complete for the rules this project uses.

**Phase:** 01, Task 1.3

---

## [2026-04-30] [ci] — OWASP dependency-check plugin version 9.0.10 (not 9.2.x or 10.x)

**Context:** Phase 01 Task 1.6 spec says to use `id("org.owasp.dependencycheck") version "9.x.x"`.
Version 9.2.x and 10.0.x both fail with Jackson classpath conflicts when combined with Spring Boot
3.2 Gradle plugin (see HACKS.md H05 for root cause). Version 9.0.10 is the latest 9.x release that
is stable with this project's toolchain combination (Gradle 8.14.3, Spring Boot 3.2.3).

**Decision:** Use `id("org.owasp.dependencycheck") version "9.0.10"`.

**Why not 9.2.x:** Version 9.2.x uses `jackson-module-blackbird` 2.16.x compiled against Jackson
2.16.0 APIs (`NativeImageUtil.isInNativeImage()`). Spring Boot Gradle plugin 3.2.3 loads
`jackson-databind:2.14.2` into the buildscript classloader; parent-first JVM delegation means
`jackson-module-blackbird` finds 2.14.2's `NativeImageUtil` (no `isInNativeImage()`) and fails.
Version 9.0.10 is affected by the same root cause — the fix is in `build.gradle.kts` root
`buildscript {}` block (see H05), not in the plugin version.

**Why not 10.0.4:** Uses Jackson 2.17.2 APIs (`JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS`
added in 2.16.0) — same root cause, different field.

**Phase:** 01, Task 1.6

---

*(New entries are prepended above this line — most recent first)*
