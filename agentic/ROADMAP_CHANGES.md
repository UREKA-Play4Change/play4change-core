# Play4Change — Roadmap Deviation Proposals

## Purpose

This file records proposed deviations from the plan in `ROADMAP.md`.
It exists to prevent silent scope changes that catch other operators by surprise.

**The rule:** No deviation from the roadmap is made without a written proposal here
and explicit human operator approval. "I'll just do this quickly" is not a valid process.

## Process

1. The AI writes a proposal entry below — without implementing anything.
2. The AI stops and reports: "I need to deviate from the roadmap. A proposal is in ROADMAP_CHANGES.md."
3. A human operator reads the proposal and replies with: "Approved", "Rejected", or "Modified — [changes]".
4. If approved: the AI proceeds, and marks the proposal as APPROVED with the approver's name.
5. If rejected: the AI does not implement the change. The proposal remains as a record.
6. After the approved deviation is implemented, update the relevant phase file and ROADMAP.md.

## Proposal Format

```
## [YYYY-MM-DD] PROPOSAL [PENDING|APPROVED|REJECTED] — [Short title]

**Requested by:** [AI | Operator name]
**Current plan:** [What ROADMAP.md currently says]
**Proposed change:** [What should be done differently]
**Reason:** [Why the current plan cannot be executed as written]
**Impact on other phases:** [Does this change the sequence or entry criteria for downstream phases?]
**Approved by:** [Operator name, date — filled after approval]
```

---

*(No deviations yet. The roadmap has not been executed. This file is empty by design.)*
