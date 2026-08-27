# Prompt 4 — Project scaffold (tasks A2–A5)

INTENT: Generate the Spring Boot project skeleton with quality gates wired from the first commit.
CONTEXT: Java 21, Maven, Spring Boot 3.x. Package com.siraj.shortener. ADR-001, ADR-003 accepted.
CONSTRAINTS: Layered structure; no Lombok on domain entities; externalized config with dev/docker profiles; no secrets in code; Flyway owns schema (ddl-auto=validate); TestNG for unit, failsafe for *IT.
ACCEPTANCE CRITERIA: `mvn spring-boot:run` starts; /actuator/health UP; V1 migration creates short_url with unique index on code; `mvn verify` runs Spotless, Checkstyle, JaCoCo.
OUTPUT FORMAT: Full file tree with contents.

Tool: Claude (Anthropic), 2026-08-25.
