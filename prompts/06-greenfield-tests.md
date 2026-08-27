# Prompt 6 — Tests for core APIs (task B7)

INTENT: Generate unit and integration tests for the core APIs.
CONTEXT: Code from Prompt 5 after engineer edits (UrlValidator complexity refactor; planted-defect comment removed). Engineer decisions: validator blocks literal IPs only (no DNS); Cache-Control: no-store retained.
CONSTRAINTS: TestNG for unit (validator, alias policy, generator, service with Mockito); REST Assured + @SpringBootTest on Testcontainers Postgres for integration; deterministic — no sleeps, injectable/mutable Clock; no test depends on another.
ACCEPTANCE CRITERIA: happy path, invalid URL, SSRF-blocked host, expired (410), deleted (410), unknown (404), reserved alias (400), bean validation (400), no stack-trace leakage; JaCoCo >= 80% on application package; `mvn verify` green.
OUTPUT FORMAT: Test files + note on what is NOT covered and why.

NOT COVERED at v0.1 (deliberate): concurrent creation of the same custom alias. This is the brownfield defect scenario (tasks C5/C6) and is reproduced there with a red-first test.

Tool: Claude (Anthropic), 2026-08-27.
