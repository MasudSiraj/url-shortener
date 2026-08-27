# ADR-001: Single Spring Boot service, layered architecture

**Status:** Proposed · **Date:** 2026-08-24 · **Deciders:** Sam (engineer) · **AI-assisted:** yes (Prompt 2)

## Context
Assignment requires a runnable, production-shaped prototype in 2–3 days (§2, §5, §7). Engineer's core stack is Java/Spring Boot. Constraints: single deployable, containerized, one-command local run.

## Decision
Java 21, Spring Boot 3.x, Maven. Four layers: `api` → `application` → `domain` ← `infrastructure`. Dependency rule: inner layers never import outer ones. Spring profiles: `dev` (H2), `docker` (PostgreSQL).

## Alternatives considered
- Hexagonal/ports-and-adapters with separate Maven modules — cleaner boundaries, but module overhead not justified for one service in the time-box.
- Kotlin/Quarkus/Node — no advantage for the assessment; would dilute the "engineer owns the code" signal.

## Consequences
+ Familiar to reviewers; fast to scaffold; strong test tooling.
+ Layer rule is enforceable with ArchUnit test (optional gate).
− Spring Boot boilerplate is verbose; mitigated by AI-generated scaffold under review.
