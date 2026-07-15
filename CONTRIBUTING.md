# Contributing to cloud-itonami-isic-563

Thank you for your interest in contributing! This document outlines the development workflow and standards.

## Development Setup

```bash
# Clone the repository
git clone https://github.com/cloud-itonami/cloud-itonami-isic-563.git
cd cloud-itonami-isic-563

# Install dependencies
clojure -M:dev

# Run tests
nbb run-tests.cljs

# Run demo
nbb run-demo.cljs
```

## Code Standards

- **ClojureScript/Clojure**: Use `.cljc` for portable code (JVM/Node/WASM)
- **Formatting**: Follow standard Clojure conventions (kebab-case, 2-space indentation)
- **Namespaces**: One namespace per file; use `src/beverageops/` for modules
- **Tests**: Collocate tests in `test/beverageops/test.cljc`; all tests must pass before PR

## Governor Checks (Inviolable)

Do not modify the three HARD checks in `governor.cljc`:
1. Table/order verification
2. Effect = `:propose`
3. Scope exclusion (age-verification, responsible-service, recipe, alcohol)

Any proposal to weaken these checks will be rejected.

## Scope Boundaries

This actor is **administrative coordination only**:
- ✓ Table scheduling, order-queue status, non-beverage supply, staff shifts, safety escalation
- ✗ Age-verification, responsible-service decisions, beverage recipes, alcohol ordering

## Submitting Changes

1. **Fork** the repository
2. **Create a branch** for your feature: `git checkout -b feature/my-feature`
3. **Write tests** for new functionality
4. **Run tests** to ensure all pass: `nbb run-tests.cljs`
5. **Commit** with clear messages: `git commit -m "feat: description"`
6. **Push** to your fork: `git push origin feature/my-feature`
7. **Open a PR** with description of changes

## PR Review Criteria

- All tests pass (16/16)
- No modifications to Governor checks
- Scope remains administrative coordination only
- Code follows conventions
- Commit messages are descriptive

## Questions?

- **GitHub Issues**: Report bugs or suggest features
- **Discussions**: Ask questions about usage and design
- **Email**: Reach out to the maintainer

---

**License**: AGPL-3.0
**Code of Conduct**: Contributor Covenant (see CODE_OF_CONDUCT.md)
