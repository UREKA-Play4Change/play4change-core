# Phase 01 — Scaffold & Operating System

**Status:** `IN PROGRESS`
**Goal:** Install the agentic/ operating system, fix CI JDK mismatch, add Detekt,
fix Name/Password value object stubs, document initial security posture, add OWASP dependency check.

**Entry criteria:** None — this is the first phase.

---

## Tasks

### Task 1.1 — Create agentic/ folder and all files
- [x] **What:** Create every file in the `agentic/` directory as specified in the operator guide.
      This task is done when `find agentic/ -type f | sort` matches the spec exactly.
- **Design constraints:** No file may contain "TODO: fill this in" placeholders.
      Every phase file must have a Goal, Entry Criteria, task list, per-task spec, Exit Criteria,
      and Human Checkpoint.
- **Tests required:** None — this is a documentation task.
- **Security log requirement:** Write the initial security posture section in THREAT-LOG.md
      as part of this task (see Task 1.5 for detail — these can be done together).
- **Exit criteria:** `find agentic/ -type f | sort` outputs every file in the spec.
      All files are complete with no placeholder text.

---

### Task 1.2 — Fix CI JDK mismatch
- [x] **What:** Change `.github/workflows/ci.yml` Java version from 17 to 21.
      The server module targets JVM 21 (`jvmToolchain(21)` in `build.gradle.kts`).
      Running CI on JDK 17 produces silent compatibility warnings and may cause
      test failures on language features only available in 21.
- **Design constraints:**
  - Change `java-version: '17'` to `java-version: '21'` in the `setup-java` step.
  - Use the `temurin` distribution (Eclipse Temurin — the standard in GitHub Actions).
  - Verify the CI YAML is valid after the change.
  - Do not change any other CI configuration in this task.
- **Tests required:** Push the branch. Confirm the GitHub Actions CI run succeeds.
      If CI is not available in the session, run `./gradlew :server:build` locally
      on JDK 21 and confirm the build is clean.
- **Security log requirement:** None for this task. Document in HACKS.md that the mismatch existed.
- **Exit criteria:** CI YAML specifies JDK 21. The build passes locally on JDK 21.
      HACKS.md entry for the mismatch is marked as FIXED.

---

### Task 1.3 — Add Detekt
- [x] **What:** Add the Detekt static analysis plugin to the `server` module.
      Create a `detekt.yml` configuration file. Wire Detekt to the CI check step.
      Generate a baseline for all existing violations so CI does not fail on pre-existing issues.
- **Design constraints:**
  - Add `id("io.gitlab.arturbosch.detekt") version "1.23.x"` to `server/build.gradle.kts`.
  - Create `server/detekt.yml` — enable all rule sets, disable only rules that conflict with
    the project's Kotlin style (document each disabled rule with a comment explaining why).
  - Add `detektMain` to the CI build step after `test`.
  - Run `./gradlew :server:detektGenerateConfig` to generate baseline if needed.
  - Baseline file goes in `server/detekt-baseline.xml`.
  - New code written after this task must not suppress Detekt without an explanatory comment.
- **Tests required:** `./gradlew :server:detektMain` must exit 0.
      CI step must be named "Detekt" and must run after "Test".
- **Security log requirement:** None.
- **Exit criteria:** `./gradlew :server:detektMain` exits 0. CI YAML includes a Detekt step.
      `detekt-baseline.xml` exists and is committed.

---

### Task 1.4 — Fix Name.kt and Password.kt value object stubs
- [x] **What:** Implement real validation logic in the `Name` and `Password` value objects.
      Currently they are stubs that accept any string. This allows invalid domain state to
      persist through the system (OWASP A03 — Injection via untrusted input reaching domain).
- **Design constraints:**
  - `Name` validation rules:
    - Not blank
    - Length 2–100 characters
    - No control characters
    - Return `Either<NameError, Name>` using the project's Arrow Either pattern (ADR-001)
  - `Password` is only used if the project adds password auth in future — confirm whether
    it is used. If not used, delete the stub rather than implementing empty validation.
    If it is used, implement: minimum 12 characters, at least one uppercase, one digit,
    one special character.
  - Validation must happen in the constructor / companion `of()` factory, not in the controller.
  - The value object must be impossible to construct in an invalid state.
- **Tests required:**
  - `NameValidationTest`: blank name rejects, name under 2 chars rejects, name over 100 chars rejects,
    control characters reject, valid name creates successfully.
  - `PasswordValidationTest` (if Password is used): similar boundary tests.
  - All existing tests must remain green after this change.
- **Security log requirement:** Update THREAT-LOG.md row for "Name/Password validation stubs"
      (OWASP A03, High) — change status from KNOWN RISK to FIXED, add the fix date.
- **Exit criteria:** Name.kt accepts valid names and rejects invalid ones in unit tests.
      The KNOWN RISKS table in THREAT-LOG.md is updated.

---

### Task 1.5 — Document initial security posture in THREAT-LOG.md
- [x] **What:** Write the full initial security posture audit in `agentic/security/THREAT-LOG.md`.
      This is a read-only analysis task — no code changes.
      Record what is already implemented, what is a known risk, and the planned remediation phase.
- **Design constraints:**
  - Read every file in `server/src/main/kotlin/` that is security-relevant:
    `SecurityConfig.kt`, `JwtAuthFilter.kt`, `MagicLinkService.kt`, `TokenService.kt`,
    `GoogleOAuthAdapter.kt`, `AuthController.kt`.
  - Map each finding to an OWASP Top 10 category.
  - Do not write THREAT-LOG entries for risks that are already fixed in ADR-016.
  - Format each entry as specified in the THREAT-LOG header.
- **Tests required:** None — this is a documentation task.
- **Security log requirement:** This task IS the security log task.
- **Exit criteria:** THREAT-LOG.md contains the ALREADY IMPLEMENTED section and the
      KNOWN RISKS table with all rows listed in the file spec.

---

### Task 1.6 — Add OWASP dependency-check Gradle plugin
- [x] **What:** Add the OWASP dependency-check Gradle plugin to the `server` module.
      Configure it to run as a weekly scheduled CI job. Log any CVEs with CVSS ≥7.0
      as HIGH entries in THREAT-LOG.md.
- **Design constraints:**
  - Add `id("org.owasp.dependencycheck") version "9.x.x"` to `server/build.gradle.kts`
    (use the latest stable version available).
  - Configure `dependencyCheck { failBuildOnCVSS = 7.0f }` — a CVE ≥7.0 fails the build.
  - Add a `.github/workflows/dependency-check.yml` that runs on a weekly schedule
    (`cron: '0 6 * * 1'` — Mondays at 6 AM UTC) and on `workflow_dispatch`.
  - The weekly job uploads the HTML report as a GitHub Actions artifact.
  - Suppress false positives in `dependency-check-suppression.xml` with documented justification.
- **Tests required:** Run `./gradlew :server:dependencyCheckAnalyze` locally. Confirm it
      produces an HTML report. Confirm no unaddressed CVEs ≥7.0 in the current dependencies.
      If any are found, add them to THREAT-LOG.md immediately.
- **Security log requirement:** Add any discovered CVEs ≥7.0 to THREAT-LOG.md as HIGH entries.
- **Exit criteria:** `./gradlew :server:dependencyCheckAnalyze` produces a report.
      The weekly CI job YAML exists. THREAT-LOG.md reflects the current dependency CVE state.

---

### Task 1.7 — Add gitleaks secret scanning to CI (ADR-018 G6)
- [ ] **What:** Add `gitleaks` to the GitHub Actions CI pipeline to prevent
      secrets from being committed to the repository. This closes the gap identified
      in ADR-018 (G6) and THREAT-LOG.md R10.
- **Design constraints:**
  - Use the official `gitleaks-action` in `.github/workflows/ci.yml`:
    ```yaml
    - name: Secret scanning (gitleaks)
      uses: gitleaks/gitleaks-action@v2
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    ```
  - This step runs BEFORE the Gradle build step so a committed secret fails
    fast without wasting build minutes.
  - Create `.gitleaks.toml` at the repository root. Any suppressed finding must
    have a comment field explaining why it is a false positive. Example format:
    ```toml
    [[allowlist]]
    description = "Test fixture — not a real credential"
    regexes = ["FAKE_SECRET_FOR_TESTING"]
    ```
  - On first run, `gitleaks` scans the full commit history. If any historical
    secrets are found, they must be assessed: if real, rotate the credential
    immediately and document the rotation in THREAT-LOG.md before the suppression
    is added. If confirmed fake/test data, suppress with justification.
  - `zap-report.html` and `zap-report.json` (Phase 07 artifacts) are already
    `.gitignore`d — confirm they remain excluded.
- **Tests required:** None — this is a CI tooling task.
      Manual verification: push a branch with a fake secret string matching a
      gitleaks pattern (e.g. `AKIA` prefix for a fake AWS key in a comment).
      Confirm the CI step fails. Remove the fake string and confirm CI passes.
- **Security log requirement:** Update THREAT-LOG.md R10: change status from
      OPEN to FIXED. Add: "gitleaks-action@v2 added to CI pre-build step. Full
      history scan clean. Suppressions in .gitleaks.toml."
- **ADR trigger:** No — implementation choice is documented in ADR-018.
- **Exit criteria:** `.github/workflows/ci.yml` includes the gitleaks step before
      the Gradle build. `.gitleaks.toml` exists at the repository root. CI pipeline
      passes on a clean branch. THREAT-LOG.md R10 is marked FIXED.

---

## Exit Criteria (Phase Level)

All 7 tasks are checked off. The following is true:
- `find agentic/ -type f | sort` matches the spec.
- `./gradlew :server:test` is green.
- `./gradlew :server:detektMain` exits 0.
- `./gradlew :server:dependencyCheckAnalyze` runs without blocking CVEs.
- Name.kt rejects invalid input in unit tests.
- THREAT-LOG.md initial posture is documented.
- CI YAML specifies JDK 21.
- Gitleaks CI step passes on a clean branch. `.gitleaks.toml` exists. THREAT-LOG.md R10 is FIXED.

---

## Human Checkpoint

Before marking Phase 01 DONE, a human operator must verify the following:

**1. CI passes (if GitHub Actions is available):**
Open the Actions tab on the repository. The latest run on the phase branch shows
all jobs green: Build, Test, Detekt.

**2. Test suite is green locally:**
```bash
./gradlew :server:test
# Expected: BUILD SUCCESSFUL, X tests passed, 0 failed
```

**3. Name validation works:**
```bash
./gradlew :server:test --tests "*.NameValidationTest"
# Expected: 5 tests, all PASSED
```

**4. Detekt passes:**
```bash
./gradlew :server:detektMain
# Expected: BUILD SUCCESSFUL — no violations above baseline
```

**5. agentic/ folder is complete:**
```bash
find agentic/ -type f | sort
# Expected: every file listed in the spec, no missing files
```

If any check fails, Phase 01 is not done. Fix the failing check before merging.
