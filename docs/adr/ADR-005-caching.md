# ADR-005: Caffeine in-process read-through cache for redirects

**Status:** Proposed · **Date:** 2026-08-24 · **Deciders:** Sam · **AI-assisted:** yes (Prompt 2)

## Context
Redirect is the hot path; reads vastly outnumber writes. Single-instance constraint.

## Decision
Spring Cache abstraction with Caffeine: cache `code → (longUrl, expiresAt, deleted)`; max 10 000 entries; TTL 10 minutes; evict on delete. Expiry is re-checked on every hit so a cached entry never serves an expired link.

## Alternatives considered
- Redis — shared across instances and reusable for rate limiting; deferred until multi-node is a requirement.
- No cache — simplest, but forfeits the reliability/performance feature the assignment asks for.

## Consequences
+ No new infrastructure; measurable hit ratio via Micrometer.
− Per-process; a multi-node deployment would have stale entries for up to TTL after delete unless swapped to Redis (code unchanged thanks to the abstraction).
