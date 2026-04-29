# Play4Change — Known Hacks & Technical Debt

## Format

A hack is any place in the code where the implementation is not the right solution —
it is a shortcut, stub, workaround, or temporary measure.

Every hack must be tracked here until it is fixed. When a hack is fixed, mark it as
FIXED with the phase and commit reference. Do not delete fixed entries.

**Entry format:**
```
## [ID] [OPEN|FIXED] — [Short title]

**Location:** [file:line or package]
**What it is:** [Describe the hack clearly — what is wrong with this code]
**Why it exists:** [Why the hack was introduced — time pressure, missing dependency, etc.]
**Fix:** [What the correct solution is]
**Fix in:** [Phase and task number where this is scheduled to be fixed]
**Fixed:** [If FIXED: phase, task, commit hash]
```

---

## H01 [OPEN] — CI JDK version mismatch (17 vs 21)

**Location:** `.github/workflows/ci.yml:14` (the `java-version` field in `setup-java` step)

**What it is:** The CI workflow runs on JDK 17, but the server Gradle build targets JVM 21
(`jvmToolchain(21)` in `server/build.gradle.kts`). This means CI compiles and tests against
an older JVM than the target runtime. Language features and optimisations available in JVM 21
are not exercised in CI.

**Why it exists:** The CI was scaffolded early and the JVM version was not updated when the
server module set its toolchain target.

**Fix:** Change `.github/workflows/ci.yml` `java-version: '17'` to `java-version: '21'`.
Use `distribution: 'temurin'`.

**Fix in:** Phase 01, Task 1.2

**Fixed:** *(to be filled when task is complete)*

---

## H02 [OPEN] — Name.kt and Password.kt value object stubs

**Location:** `server/src/main/kotlin/*/domain/model/Name.kt`,
`server/src/main/kotlin/*/domain/model/Password.kt` (exact paths may vary — search for these files)

**What it is:** These value objects accept any string without validation. A `Name` constructed
from an empty string or a 1000-character control-character string is indistinguishable from a
valid name in the domain model. `Password.kt` may be unused entirely. Neither class enforces
any invariant.

**Why it exists:** They were scaffolded as stubs when the domain model was initially built
and were never completed before the auth system was added on top.

**Fix:**
- `Name`: validate non-blank, length 2–100, no control characters. Return `Either<NameError, Name>`.
- `Password`: if unused, delete the file. If used, implement: ≥12 chars, uppercase, digit,
  special character. Validate on construction.
- Write unit tests for both.

**Fix in:** Phase 01, Task 1.4

**Fixed:** *(to be filled when task is complete)*

---

## H03 [OPEN] — All mobile repositories mocked (Ktor not wired)

**Location:** `composeApp/src/commonMain/kotlin/*/data/` — all `Mock*Repository.kt` files

**What it is:** Every repository in the KMP mobile client is a mock implementation.
The `MockAuthRepository`, `MockTopicRepository`, `MockEnrollmentRepository`,
`MockStruggleRepository`, `MockPeerReviewRepository`, `MockProfileRepository`, and
`MockBadgeRepository` return hardcoded or randomly delayed fake data. No real HTTP calls
are made to the server.

**Why it exists:** The mobile client UI was built against mocks to allow frontend development
to proceed independently of backend development. This is the correct approach for parallel
development — the hack is that the wiring to real HTTP was never done.

**Fix:** Replace each mock repository with an `Http*Repository` implementation using Ktor
`HttpClient`. Implement `TokenStorage` with `EncryptedSharedPreferences` (Android) and
Keychain (iOS). Implement 401→refresh→retry logic.

**Fix in:** Phase 04, Tasks 4.1–4.5

**Fixed:** *(to be filled when task is complete)*

---

*(New entries are prepended above existing open items)*
