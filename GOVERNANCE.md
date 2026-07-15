# Governance: cloud-itonami-isic-563

## Decision Authority

This actor is governed by **three HARD, permanent, un-overridable checks** enforced in the Governor module.

### Governor: Three HARD Checks

#### Check 1: Table/Order-Record Verification
- **Rule**: For table/order-specific operations, the target table/order must exist AND be `:registered?` AND `:verified?`
- **Facility-level operations** (supply, shift, safety) do NOT require table/order verification
- **Re-derived** from store's own fields on every proposal
- **Effect**: Rejection if unverified

#### Check 2: Effect = `:propose`
- **Rule**: The effect field must be `:propose`
- **No exceptions**: No other effect values are accepted
- **Effect**: Outright rejection if violated

#### Check 3: Scope Exclusion
- **Blocked patterns** (EN+JA substring matching):
  - Age-verification, ID-checking, ID-check
  - Responsible-service, responsible service
  - Recipe, drink-recipe, drink content, beverage recipe
  - Alcohol-inventory, alcohol ordering, alcohol purchase
  - Japanese: 年齢確認, 未成年, 飲酒責任, 調理, アルコール, ID確認, 本人確認
- **Allowed operations** (closed allowlist):
  - `:schedule-table-reservation`
  - `:coordinate-order-status-update`
  - `:coordinate-supply-request`
  - `:schedule-staff-shift-proposal`
  - `:flag-safety-concern`
- **Special case**: `:flag-safety-concern` is allowed even if it mentions "safety" (always escalates)
- **Effect**: Rejection if scope-excluded

## Modification Policy

- **Governor checks**: IMMUTABLE. Any proposal to weaken, remove, or bypass these checks will be rejected.
- **Operation allowlist**: Changes require full ADR (governance decision record).
- **Phase progression**: Changes require ADR + full test re-validation.

## Escalation & Human Review

- `:flag-safety-concern` operations **always escalate** to human review (never auto-commit, even in Phase 3).
- Safety escalations are logged and never self-enforced.

## Regulatory Compliance

This actor provides **administrative coordination only**. Deployment in regulated jurisdictions (alcohol service, age-gated beverages) requires:
- Local compliance review
- Integration with licensed staff judgment
- Age-verification and responsible-service governance layers (outside this actor)
- Human-in-the-loop for safety determinations

## Change Log

- **2026-07-15**: Initial implementation (ADR-2607162700)
- Three HARD checks established as inviolable governance

---

**Governance Model**: Permanent, un-overridable Governor
**Review Authority**: ADR (Architectural Decision Record)
**License**: AGPL-3.0
