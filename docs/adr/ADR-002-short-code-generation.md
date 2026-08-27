# ADR-002: Random base62 short codes behind a ShortCodeGenerator interface

**Status:** Proposed · **Date:** 2026-08-24 · **Deciders:** Sam · **AI-assisted:** yes (Prompt 2)

## Context
Codes must be unique, short, and not trivially enumerable. Custom aliases (AR-2) share the same namespace.

## Decision
`ShortCodeGenerator` interface with two implementations selected by `shortener.generator=random|sequence`:
- `RandomBase62Generator` (default): 7 chars from `[0-9A-Za-z]` via `SecureRandom`; uniqueness enforced by DB unique index; service retries up to 3 times on `DataIntegrityViolationException`, then fails with 503.
- `SequenceBase62Generator`: base62 encoding of a DB sequence — deterministic, used to demonstrate the refactor in the brownfield scenario and as a fallback.

Reserved words (`api`, `actuator`, `health`, `swagger-ui`, `v3`) are rejected as aliases.

## Alternatives considered
- Sequence only — enumerable; scraping private links is a real threat for shorteners.
- Hash of long URL (e.g., first 7 of SHA-256) — collisions harder to reason about; same URL from different users would share a code, leaking existence.
- Snowflake/distributed IDs — solves multi-node, not needed for single instance.

## Consequences
+ Non-guessable codes; DB-independent generation; horizontally scalable.
− Requires collision retry logic and a test that proves it (unit test with a stubbed repository throwing on first insert).
− Keyspace ≈ 3.5 × 10^12; collision probability negligible at prototype scale but documented.
