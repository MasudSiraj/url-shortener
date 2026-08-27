# ADR-006: RFC 7807 problem+json for all error responses

**Status:** Proposed · **Date:** 2026-08-24 · **Deciders:** Sam · **AI-assisted:** yes (Prompt 2)

## Context
Consistent, machine-readable errors; no leakage of stack traces or internal details (NFR-S3).

## Decision
Single `@RestControllerAdvice` mapping domain exceptions to `ProblemDetail` (Spring 6 native). Fields: `type`, `title`, `status`, `detail`, `instance`, plus `correlationId`. Never include exception messages from the persistence layer.

## Consequences
+ Predictable client contract; easy to assert in REST Assured tests.
− Redirect endpoint returns problem+json on 404/410 rather than HTML — acceptable for an API-first prototype; noted in docs/08.
