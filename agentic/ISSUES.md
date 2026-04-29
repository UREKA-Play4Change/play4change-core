# Play4Change — Open Issues

## Format

An issue is a discovered problem that has not yet been fixed. Issues differ from TODO items
(which are deferred features) and HACKS (which are known code shortcuts).
Issues are bugs, inconsistencies, broken behaviour, or infrastructure problems.

When an issue is fixed, mark it FIXED with the phase, task, and commit. Do not delete entries.

**Entry format:**
```
## [ID] [OPEN|FIXED] Severity:[Critical|High|Medium|Low] — [Short title]

**Discovered:** [Date or "Phase X, Task Y"]
**Description:** [What the problem is — be specific. Not "it doesn't work" but what
  exactly happens, what is expected, and what is observed.]
**Impact:** [Who is affected and how]
**Workaround:** [Any workaround available until the fix is in, or "none"]
**Fix plan:** [Which phase and task will fix this, or "unscheduled"]
**Fixed:** [If FIXED: Phase X, Task Y, commit hash]
```

---

## I01 [OPEN] Severity:Medium — CI JDK version mismatch (17 vs 21)

**Discovered:** Phase 01

**Description:** The GitHub Actions CI workflow (`.github/workflows/ci.yml`) specifies
`java-version: '17'` in the `setup-java` step. The server Gradle build specifies
`jvmToolchain(21)`. This means CI compiles and runs tests using JDK 17 despite the
codebase targeting JDK 21. The Gradle JVM toolchain will attempt to resolve JDK 21
automatically when running locally, but in CI the resolution may fall back to the
available JDK 17, causing a potential discrepancy between local and CI compilation.

**Impact:** Tests may pass locally on JDK 21 and fail in CI (or vice versa) if any
code uses JDK 21-specific APIs or behaviour. The discrepancy silently erodes CI trust.
Severity is Medium (not High) because no active test failures are known at this time —
but the gap is real.

**Workaround:** Run `./gradlew :server:test` locally on JDK 21 before trusting CI results.

**Fix plan:** Phase 01, Task 1.2 — update CI YAML to `java-version: '21'` (Temurin distribution).

**Fixed:** *(to be filled when Task 1.2 is complete)*

---

*(New entries are prepended above existing open items. Most recent first.)*
