# ADR-004: Asynchronous in-process click events (at-most-once)

**Status:** Proposed · **Date:** 2026-08-24 · **Deciders:** Sam · **AI-assisted:** yes (Prompt 2)

## Context
Analytics (AR-4) must not slow or break the redirect hot path. No extra infrastructure permitted for the prototype.

## Decision
`RedirectController` publishes a `ClickRecorded` Spring `ApplicationEvent` after resolving the target. A `@Async` listener on a dedicated bounded `ThreadPoolTaskExecutor` (core 2, max 4, queue 1000) persists `ClickEvent`. Rejection policy: `DiscardPolicy` + `analytics.events.dropped` counter. Delivery is at-most-once.

Stored fields: timestamp, referrer, user-agent, **salted SHA-256 hash of client IP** — never the raw IP.

## Alternatives considered
- Synchronous insert — couples redirect availability to DB write latency; rejected.
- `CallerRunsPolicy` on queue full — would push analytics work onto the request thread; rejected.
- Kafka — durable and replayable; the correct production choice, but broker + consumer + DLQ exceed the time-box. Recorded as upgrade path; the event/listener seam makes the swap localized.

## Consequences
+ Redirect p95 unaffected by analytics; DB outage degrades analytics only.
− Click loss on crash or queue saturation; acceptable for click counts, documented in docs/08.
− `@Async` requires the listener to run outside the request transaction — verified by an integration test asserting the redirect response returns before the click row exists.
