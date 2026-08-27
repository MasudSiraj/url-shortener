package com.siraj.shortener.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.siraj.shortener.AbstractPostgresIT;
import io.restassured.http.ContentType;
import java.util.Map;
import org.springframework.test.context.TestPropertySource;
import org.testng.annotations.Test;

/**
 * Task D3/D4 end-to-end. Limits are lowered via properties so the test is fast and deterministic —
 * proving in passing that limits are configuration, not constants (docs/06 Q9).
 */
@TestPropertySource(
    properties = {
      "shortener.rate-limit.enabled=true",
      "shortener.rate-limit.create-per-minute=3",
      "shortener.rate-limit.redirect-per-minute=5"
    })
public class RateLimitIT extends AbstractPostgresIT {

  @Test
  public void createBeyondLimitReturns429WithRetryAfterAndProblemJson() {
    for (int i = 0; i < 3; i++) {
      given()
          .contentType(ContentType.JSON)
          .body(Map.of("longUrl", "https://example.com/rl-" + i))
          .post("/api/v1/urls")
          .then()
          .statusCode(201);
    }

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("longUrl", "https://example.com/rl-over"))
        .post("/api/v1/urls")
        .then()
        .statusCode(429)
        .contentType("application/problem+json")
        .header("Retry-After", notNullValue())
        .body("type", equalTo("https://siraj.dev/problems/rate-limited"))
        .body("title", equalTo("Too many requests"));
  }

  @Test
  public void actuatorIsNeverRateLimited() {
    for (int i = 0; i < 30; i++) {
      given().get("/actuator/health").then().statusCode(200);
    }
  }

  @Test
  public void errorBodyLeaksNothingInternal() {
    String body =
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("longUrl", "https://example.com/leak"))
            .post("/api/v1/urls")
            .then()
            .extract()
            .asString();
    assertThat(body).doesNotContain("Exception").doesNotContain("com.siraj");
  }
}
