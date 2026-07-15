# cloud-itonami-isic-563: Beverage-Serving Administrative Coordination Actor

**ISIC-563** ("Beverage serving activities") — Administrative coordination for bars, cafes, and beverage-serving establishments.

## Domain Scope

This actor handles **administrative coordination only**:
- Table/seating scheduling logistics (NOT capacity overrides or occupancy compliance)
- Order-queue status tracking (NOT beverage preparation or responsible-service determinations)
- Non-beverage supply coordination (glassware, napkins, cleaning supplies — NOT alcohol/beverage ordering)
- Staff shift proposals (administrative proposals only — NOT bartender certification or alcohol-service assignment)
- Safety concern escalation (facility hazards, intoxication welfare checks — always escalates to human review)

### Explicitly OUT of Scope
- Age-verification / ID-checking decisions
- Responsible-service-of-alcohol determinations
- Drink recipes / beverage content decisions
- Alcohol/beverage ingredient ordering (see non-beverage supply coordination above)
- Safety-authority overrides (escalations only, never self-commits)

## Governor: Three HARD, Un-overridable Checks

1. **Table/Order-Record Verification**: Target table/order must exist AND be `:registered?` AND `:verified?` (for table/order-specific operations)
2. **Effect = `:propose`**: Effect must always be `:propose`; no other effect values accepted
3. **Scope Exclusion**: Blocked patterns (EN+JA substring matching) prevent age-verification, responsible-service, recipe, and alcohol-specific decisions

## Operation Allowlist (Closed)

- `:schedule-table-reservation` — Table/seating scheduling
- `:coordinate-order-status-update` — Order-queue status tracking
- `:coordinate-supply-request` — Non-beverage supply coordination
- `:schedule-staff-shift-proposal` — Administrative shift proposals
- `:flag-safety-concern` — Facility/welfare safety escalation (always escalates)

Any operation outside this set is rejected.

## Phase Progression (0→3)

- **Phase 0** (read-only): All proposals held for human review
- **Phase 1**: Table scheduling + order status updates auto-commit
- **Phase 2**: + supply coordination + staff shift proposals auto-commit
- **Phase 3**: All non-safety auto-commit; safety concerns always escalate

## Architecture

- **Store** (`beverageops.store`): MemStore SSoT with table/supply directories, append-only ledgers
- **Advisor** (`beverageops.advisor`): Deterministic proposal confidence scoring
- **Governor** (`beverageops.governor`): Three HARD checks (unoverridable)
- **Operation** (`beverageops.operation`): langgraph-clj StateGraph single-run orchestration
- **Phase** (`beverageops.phase`): Rollout phase control (0–3 auto-commit gates)
- **Sim** (`beverageops.sim`): Deterministic demo (5 scenarios offline)
- **Tests** (`test/beverageops/test.cljc`): 16/16 passing

## Running Tests & Demo

```bash
# Tests (via nbb)
nbb run-tests.cljs

# Demo (via nbb)
nbb run-demo.cljs
```

## References

- ADR-2607162700: Full decision record & rationale
- ADR-2607155100: ISIC-561 restaurant coordination (sibling actor)
- ADR-2607121000: Wave 4 food-service cluster definition
- Skill `build-actor`: Actor pattern details
- Skill `new-project-scaffold`: Repo lifecycle

## Governance

- **License**: AGPL-3.0
- **Code of Conduct**: Contributor Covenant
- **Decision Authority**: Three HARD checks (Governor)
- **No Liability**: See SECURITY.md

---

**Status**: Implemented
**Wave**: 4 (Food-service cluster)
**Completion**: 2026-07-15
