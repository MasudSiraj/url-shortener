# ADR-007: Soft delete for short URLs

**Status:** Proposed · **Date:** 2026-08-24 · **Deciders:** Sam · **AI-assisted:** yes (Prompt 2)

## Context
Delete is in scope (AR-1). Analytics rows reference the short URL; hard delete would cascade or orphan them.

## Decision
`DELETE /api/v1/urls/{code}` sets `deleted_at`; redirect returns 410 Gone; stats remain queryable; cache entry evicted. Codes are not reused.

## Alternatives considered
- Hard delete with cascade — loses analytics history; code could be re-issued, confusing old links.

## Consequences
+ Audit trail preserved; unambiguous 410 semantics.
− Unbounded retention; no purge job — listed as limitation.
