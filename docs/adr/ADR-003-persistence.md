# ADR-003: PostgreSQL at runtime; H2 for unit tests; Testcontainers for integration

**Status:** Proposed · **Date:** 2026-08-24 · **Deciders:** Sam · **AI-assisted:** yes (Prompt 2)

## Context
"Production-grade" (§7) and "scalable" (§6). Alias uniqueness and the concurrency bug fix (brownfield) must be proven against real DB semantics. Evaluator must run with one command (§5).

## Decision
- Runtime (`docker` profile): PostgreSQL 16 via Docker Compose, schema managed by Flyway.
- Unit/slice tests: H2 in PostgreSQL compatibility mode for speed.
- Integration tests (`*IT`, failsafe): Testcontainers PostgreSQL — concurrency and constraint tests run only here.

## Alternatives considered
- H2 everywhere — fast, but the unique-constraint/concurrency proof would be against the wrong engine.
- Redis-only — no relational analytics; weak durability story.
- Testcontainers everywhere — slower feedback loop for pure unit tests.

## Consequences
+ Constraint behavior tested on the real engine.
− Docker required on evaluator machine; documented in README with a `dev` fallback (H2) that runs without Docker.
− Two test tiers to maintain; naming convention (`*Test` vs `*IT`) enforced by surefire/failsafe config.
