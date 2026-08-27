# url-shortener

AI-assisted URL shortener — Charles Schwab engineering assessment.

> Scaffold stage. Full README lands in task F5. See `docs/` for requirement analysis, architecture, and task breakdown.

## Quick start

```bash
# without Docker (H2, dev profile is default)
mvn spring-boot:run
curl localhost:8080/actuator/health

# with Docker (PostgreSQL)
docker compose up --build
```

## Run tests

```bash
mvn test      # unit (TestNG, H2)
mvn verify    # + integration (*IT on Testcontainers Postgres), Spotless, Checkstyle, JaCoCo gate
mvn dependency-check:check   # OWASP scan (first run downloads NVD data)
```
