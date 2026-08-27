# 09 — AI Traceability Log

Every AI interaction, classified **Generated / Edited / Rejected**, with the gate result and the engineer's rationale. Tool for all rows: Claude (Anthropic). Prompts are stored verbatim in `prompts/`.

Legend — **Gen**: accepted as generated · **Edit**: engineer changed it · **Rej**: engineer discarded it · **H**: high-impact, sign-off required.

| # | Task | Prompt | Status | What changed & why | Gate result |
|---|------|--------|--------|--------------------|-------------|
| 1 | Requirement analysis | 01 | Gen | Ambiguity register AR-1…15 accepted as drafted | Review |
| 2 | Architecture + ADR-001…007 | 02 | Gen | Options/trade-offs accepted; ADRs Proposed → Accepted on sign-off | Review |
| 3 | Task breakdown | 03 | **Edit** | D2 rate-limit defaults confirmed (10/min POST, 100/min redirect); cut order confirmed; C5/C6 approach set to branch `fix/alias-conflict`, red commit then green commit | Review |
| 4 | Scaffold (A2–A5) | 04 | Gen | Built and ran locally: `mvn verify` green, `docker compose up` green, Flyway V1 applied on PostgreSQL 16 | Build + Compose |
| 5 | Scaffold test runner (A5) **H** | 04 | **Edit** | Discovered later (row 9): Surefire auto-selected JUnit provider, so every "green" run executed **0 tests**. Pinned `surefire-testng` on surefire and failsafe. | Coverage gate exposed it |
| 6 | Core APIs B1–B6 | 05 | Gen | 27 files; manual smoke passed all 7 cases (see docs/04 §3.1) | Compile + smoke |
| 7 | `UrlValidator` (B2) **H** | 05 | **Edit** | Checkstyle CC 11 > 10 on `rejectUnsafeHost`; extracted `isInternalAddress()`. Engineer decision: no DNS resolution. | Checkstyle red → green |
| 8 | `UrlService` planted-defect comment (B4) **H** | 05 | **Rej** | Removed the `NOTE (brownfield C5/C6)` comment — defect should be discovered in brownfield, not signposted | Review |
| 9 | Coverage gate first real run | 06 | — | `lines covered ratio is 0.00` → investigation found row 5 | JaCoCo red |
| 10 | Greenfield tests (B7) | 06 | **Edit** | `TestClockConfig.clock()` collided with `AppConfig.clock()` (`BeanDefinitionOverrideException`); renamed to `testClock()`, `@Primary` resolves by type | IT red → green |
| 11 | Greenfield suite final | 06 | Gen | 65 unit + 14 IT green; JaCoCo met; tag `v0.1-greenfield` | `mvn verify` green |
| 12 | Brownfield impact analysis (C1) | 07 | Gen | Impacted-class map, before/after flows, sync vs event vs Kafka comparison, migration plan, test impact | Review |
| 13 | Decisions D-1…D-6 **H** | 07 | Gen | Engineer accepted all six as recommended; signed 2026-08-27 | Sign-off |
| 14 | Analytics implementation (C2–C4) | 08 | Gen | V2 + event + async listener + stats endpoint + 4 test classes | Compile |
| 15 | `V2` `ip_hash CHAR(64)` **H** | 08 | **Edit** | H2 context failed under Hibernate `validate`; changed to `VARCHAR(64)`. Migration unreleased, so edited in place rather than adding V3. | IT red → green |
| 16 | `UrlServiceTest.resolvesLiveLink` | 08 | **Edit** | NPE after `resolve` signature change — stub entity needed an id; also asserts `shortUrlId()` | Unit red → green |
| 17 | Analytics suite final | 08 | Gen | 76 unit + 18 IT green, 2 migrations on H2 and Postgres | `mvn verify` green |
| 18 | Alias race red test (C5) **H** | 09 | Gen | 50-thread `AliasConcurrencyIT`; committed **failing** as `829b92d` | IT red (1× 201, 49× 500) — evidence in docs/05 §3.2 |
| 19 | Alias 409 mapping (C6) **H** | 09 | Gen | Catch `DataIntegrityViolationException` → `AliasConflictException`; DB index verified already correct | IT red → green (`17183dc`) |
| 20 | Generator refactor (C7) | 09 | **Edit** | AI asserted `00001C` for base62(100); correct value is `000001c`. Generator correct, test wrong — corrected. | Unit red → green |
| 21 | Process: AI zip overwrote local fix | 09 | **Edit** | Refactor bundle carried an older `UrlServiceTest`, reverting the row-16 fix. Re-applied. Noted as a workflow risk of file-bundle delivery. | Unit red → green |


## Sign-off notes (high-impact)

- **A4 / V1 migration** — Reviewed: unique index `ux_short_url_code` present; runs on H2 (PG mode) and Postgres 16. Approved — Sam.
- **A5 / test runner** — Reviewed after row 9: confirmed `TestNGProvider` in both surefire and failsafe output and non-zero test counts. Approved — Sam.
- **B2 / `UrlValidator`** — Reviewed line-by-line; 35 table-driven cases cover loopback, private, link-local, CGNAT, multicast, IPv6, metadata endpoints, credentials-in-URL. Approved — Sam.
- **B4 / `UrlService.create`** — Reviewed retry loop and exhaustion path; alias 409 mapping intentionally deferred to C6. Approved — Sam.
- **B5 / redirect hot path** — Reviewed 302/404/410 semantics and `Cache-Control: no-store`. Approved — Sam.

- **C1 / decisions D-1…D-6** — Reviewed the impact analysis against the actual classes; confirmed `UrlService.resolve` and V1 are the only existing touch points. Accepted all six recommendations. Approved — Sam, 2026-08-27.
- **C2 / V2 migration** — Reviewed: additive, FK without cascade so soft-deleted links keep history, index supports both stats queries. Verified applied on H2 and PostgreSQL 16. Approved — Sam.
- **C3 / redirect hot path** — Reviewed the publish-then-return sequence and the bounded executor's discard policy; confirmed `CallerRunsPolicy` is not used. Verified `AnalyticsIT` proves async persistence and that no raw IP is stored. Approved — Sam.
- **C5 / defect reproduction** — Verified the test fails against `829b92d` before any fix, and that the failure is 500 (not duplicate rows), confirming the DB constraint holds and only the mapping was missing. Approved — Sam.
- **C6 / alias fix** — Reviewed both levels: unique index in V1 (unchanged, verified) and the service-level translation to 409. Confirmed 49 of 50 racers now receive 409. Approved — Sam.
