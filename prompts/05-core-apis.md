# Prompt 5 — Core API implementation (tasks B1–B6)

INTENT: Implement the core shortener APIs on the verified scaffold.
CONTEXT: Scaffold from Prompt 4 (mvn verify + docker compose green, no edits). ADR-001/002/003/006/007. Task IDs B1–B6 in docs/03.
CONSTRAINTS:
 - POST /api/v1/urls {longUrl, customAlias?, expiresAt?} → 201 + Location
 - GET /{code} → 302; 404 unknown; 410 expired/deleted
 - GET /api/v1/urls/{code} metadata; DELETE → 204 soft delete
 - Validate URL: http/https only, ≤2048, reject loopback/private/link-local/metadata hosts (SSRF)
 - Alias policy: 4–16 chars [A-Za-z0-9_-], reserved words rejected
 - Collision retry ×3 → 503; RFC 7807 for all errors; no business logic in controllers; injectable Clock
 - Do NOT yet map alias DataIntegrityViolationException → 409 (that is the planted brownfield defect, C5/C6)
ACCEPTANCE CRITERIA: Compiles; springdoc exposes the endpoints; validation and alias rules unit-testable without Spring.
OUTPUT FORMAT: Java files by layer.

Tool: Claude (Anthropic), 2026-08-25.
