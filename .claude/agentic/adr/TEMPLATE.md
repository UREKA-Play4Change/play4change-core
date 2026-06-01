# ADR-XXX — [Title: short noun phrase describing the decision]

**Status:** [Proposed | Accepted | Deprecated | Superseded by ADR-YYY]
**Date:** [Month YYYY]
**Author:** [Name]
**Relates to:** [ADR-NNN (Brief title), ADR-MMM (Brief title)]

---

## Context

[Describe the problem, the forces at play, and the constraints that make this decision
necessary. Write in plain past tense: "the system required...", "the constraint was...".

Be specific about:
- What happened or was discovered that prompted this decision
- What requirements or constraints are non-negotiable
- Why doing nothing is not an option

Do not describe the decision here. Only the context that makes it necessary.]

---

## Decision

**[One sentence. Imperative. No hedging. Examples:
"Use Bucket4j with Redis for rate limiting on all /auth/** endpoints."
"Store magic link tokens as SHA-256 hashes, never as raw values."
]**

---

## Why This, Not Something Else

[A table of alternatives considered and why each was rejected. This is the most important
section — it is what prevents future sessions from re-litigating the decision.]

| Alternative | Why rejected |
|-------------|-------------|
| [Option A] | [Specific reason — not "too complex" but what specific problem it causes] |
| [Option B] | [Specific reason] |
| [Option C] | [Specific reason] |

---

## Consequences

**Easier:**
- [What becomes easier or simpler as a result of this decision]

**Harder:**
- [What becomes harder or requires more care as a result of this decision]

**Must revisit when:**
- [Specific trigger condition that should prompt re-evaluation, e.g.:
  "User base exceeds 10,000 concurrent sessions"
  "A second OAuth provider requires a different verification flow"
  "Kotlin Multiplatform stabilises the Keychain API"]

---

## Security Considerations

[Mandatory section. Map this decision to OWASP Top 10 categories where relevant.
If the decision is security-neutral, write "This decision has no direct security impact."
If it has security implications, write them explicitly:
  "This decision addresses OWASP A02 (Cryptographic Failures) by..."
  "This decision introduces a residual risk: [risk]. Mitigation: [mitigation]."
]

---

## References

- [ADR-NNN — Related decision]
- [Specification, RFC, or documentation that informed this decision]
- [Relevant section of THREAT-LOG.md if a security risk was accepted]
