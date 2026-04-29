# Threat Dragon Model — Maintenance Guide

**File:** `agentic/security/threat-model.td`
**Tool:** [OWASP Threat Dragon v2](https://owasp.org/www-project-threat-dragon/)
**Created:** April 2026 per ADR-018

---

## How to open the model

1. Go to https://www.threatdragon.com or run Threat Dragon locally:
   ```bash
   docker run -it --rm -p 3000:3000 threatdragon/owasp-threat-dragon:latest
   ```
2. Open `agentic/security/threat-model.td` via "Open existing model".
3. Select the "Play4Change — System Overview" diagram.

## When to update this model

Update `threat-model.td` at the start of each phase that introduces a new attack surface.
This happens in parallel with writing the STRIDE table in THREAT-LOG.md.

**Update triggers:**
- New bounded context added (new process node)
- New external integration added (new actor node)
- New data store added
- New API endpoint exposed beyond the current trust boundary
- A known threat is mitigated — change status from Open to Mitigated

## How to update

1. Open the model in Threat Dragon.
2. Add the new component (actor, process, store, flow).
3. For each new process or data flow, run a STRIDE analysis:
   - Spoofing — can an attacker impersonate this component?
   - Tampering — can data in transit or at rest be modified?
   - Repudiation — are actions logged and attributable?
   - Information disclosure — what data could leak?
   - Denial of service — can this be disrupted?
   - Elevation of privilege — can an attacker gain higher access?
4. Add threats to the relevant component. Set status to Open.
5. Save the file. The `.td` file is JSON — commit it as any other file.
6. The corresponding THREAT-LOG.md STRIDE table entry is written in the same PR.

## Threat status values

| Status | Meaning |
|--------|---------|
| Open | Threat identified, not yet mitigated |
| Mitigated | Control in place and verified |
| Not applicable | Threat does not apply to this component in this architecture |
| Out of scope | Known threat, deliberately excluded from this model |

## Numbering

Threats are numbered sequentially (t1, t2, t3...) across the entire model.
Do not reuse a threat ID even if a threat is deleted.
The next available ID after initial creation is **t10**.
