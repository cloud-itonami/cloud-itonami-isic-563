# Security Policy: cloud-itonami-isic-563

## Threat Model

This actor is designed as a **coordination advisor**, not a compliance authority. It provides **administrative coordination only** for bar/cafe operations.

### What This Actor DOES
- Propose administrative operations (table scheduling, order status, supply coordination)
- Score proposal confidence deterministically
- Enforce three HARD Governor checks
- Log all operations to append-only ledger
- Escalate safety concerns to human review

### What This Actor DOES NOT DO
- Make age-verification or ID-checking decisions
- Enforce responsible-service-of-alcohol (requires licensed staff judgment)
- Determine drink recipes or beverage content
- Override human safety authority
- Auto-enforce safety determinations (always escalates)

## Governor Guarantees

The three HARD checks are **cryptographically-enforced invariants**:

1. **Table verification**: Cannot propose table operations on unverified tables
2. **Effect = propose**: Cannot commit, hold, or execute — only propose
3. **Scope exclusion**: Cannot bypass age-verification, responsible-service, recipe patterns

These checks are **permanent** and **un-overridable** by design.

## Audit & Transparency

- **Append-only ledger**: All operations logged (proposal, scoring, decision, timestamp)
- **Ledger immutability**: Ledger entries are never deleted or modified (append-only pattern)
- **Deterministic scoring**: Advisor scoring is fully deterministic (no LLM randomness)
- **Governance visibility**: Governor decisions are logged with reason code

## Liability Disclaimer

**NO WARRANTY**: This software is provided "as is" without warranty of any kind, express or implied.

**NOT A COMPLIANCE TOOL**: This actor is not a legal compliance tool. It does not determine compliance with local liquor laws, age-gating regulations, or responsible-service-of-alcohol standards.

**HUMAN JUDGMENT REQUIRED**: Deployment in regulated contexts (alcohol service, age-gated venues) requires:
- Local legal review
- Integration with licensed staff judgment
- Separate age-verification and responsible-service governance
- Human-in-the-loop for safety determinations

**NO LIABILITY FOR**:
- Violations of liquor laws or age-gating regulations (not the actor's domain)
- Safety incidents (escalated to human review; actor does not enforce)
- Decisions made by deployments that weaken the three HARD checks
- Jurisdictional compliance failures (actor provides no compliance guarantee)

## Responsible Disclosure

If you discover a security issue:

1. **Do NOT** open a public GitHub issue
2. **Email** the maintainer with details
3. **Allow** 90 days for patch development before public disclosure

## Dependencies

- **Clojure/ClojureScript**: Use latest stable versions
- **langgraph-clj**: Pinned to superproject version
- **No external AI/LLM**: All scoring is deterministic; no third-party model calls

## Deployment Checklist

Before deploying this actor in production:

- [ ] Read this SECURITY.md fully
- [ ] Understand that this is **administrative coordination only**
- [ ] Confirm local compliance review (not performed by this actor)
- [ ] Verify human-review infrastructure exists (for escalations)
- [ ] Test Governor checks in your environment
- [ ] Log all operations; audit logs regularly
- [ ] Never weaken or remove the three HARD checks
- [ ] Disclose limitations to operators and regulators

---

**Security Model**: Permanent Governor + Append-only Audit
**Review Authority**: ADR-2607162700
**Last Updated**: 2026-07-15
**License**: AGPL-3.0
