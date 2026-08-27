package com.siraj.shortener.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.siraj.shortener.AbstractPostgresIT;
import com.siraj.shortener.support.MutableClock;
import com.siraj.shortener.support.TestClockConfig;
import io.restassured.http.ContentType;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** End-to-end behavior of the greenfield APIs on real PostgreSQL (task B7). */
@TestPropertySource(properties = "shortener.rate-limit.enabled=false")
public class UrlApiIT extends AbstractPostgresIT {

  @Autowired private MutableClock clock;

  @BeforeMethod
  public void resetClock() {
    clock.set(TestClockConfig.START);
  }

  private String create(Map<String, Object> body) {
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/api/v1/urls")
        .then()
        .statusCode(201)
        .header("Location", matchesPattern("http://localhost:8080/[A-Za-z0-9_-]{4,16}"))
        .body("shortCode", matchesPattern("[A-Za-z0-9_-]{4,16}"))
        .body("createdAt", equalTo(TestClockConfig.START.toString()))
        .extract()
        .path("shortCode");
  }

  @Test
  public void createThenRedirect() {
    String code = create(Map.of("longUrl", "https://example.com/some/long/path"));

    given()
        .redirects()
        .follow(false)
        .when()
        .get("/" + code)
        .then()
        .statusCode(302)
        .header("Location", equalTo("https://example.com/some/long/path"))
        .header("Cache-Control", equalTo("no-store"));
  }

  @Test
  public void metadataEndpointReturnsStoredFields() {
    String code = create(Map.of("longUrl", "https://example.com/meta"));

    given()
        .when()
        .get("/api/v1/urls/" + code)
        .then()
        .statusCode(200)
        .body("shortCode", equalTo(code))
        .body("longUrl", equalTo("https://example.com/meta"))
        .body("customAlias", equalTo(false))
        .body("expiresAt", nullValue())
        .body("deletedAt", nullValue());
  }

  @Test
  public void customAliasIsHonoured() {
    String code = create(Map.of("longUrl", "https://example.com", "customAlias", "it-alias"));

    given().when().get("/api/v1/urls/" + code).then().body("customAlias", equalTo(true));
  }

  @Test
  public void unknownCodeIs404ProblemJson() {
    given()
        .when()
        .get("/zzzz9999")
        .then()
        .statusCode(404)
        .contentType("application/problem+json")
        .body("type", equalTo("https://siraj.dev/problems/not-found"))
        .body("instance", equalTo("/zzzz9999"));
  }

  @Test
  public void deletedLinkIs410AndMetadataShowsDeletedAt() {
    String code = create(Map.of("longUrl", "https://example.com/del"));

    given().when().delete("/api/v1/urls/" + code).then().statusCode(204);
    given().when().delete("/api/v1/urls/" + code).then().statusCode(204); // idempotent
    given().redirects().follow(false).when().get("/" + code).then().statusCode(410);
    given().when().get("/api/v1/urls/" + code).then().body("deletedAt", notNullValue());
  }

  @Test
  public void expiredLinkIs410WithoutSleeping() {
    String expiresAt = TestClockConfig.START.plus(Duration.ofMinutes(5)).toString();
    String code = create(Map.of("longUrl", "https://example.com/exp", "expiresAt", expiresAt));

    given().redirects().follow(false).when().get("/" + code).then().statusCode(302);

    clock.advance(Duration.ofMinutes(6));

    given().redirects().follow(false).when().get("/" + code).then().statusCode(410);
  }

  @Test
  public void expiryInThePastIs400() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("longUrl", "https://example.com", "expiresAt", "2000-01-01T00:00:00Z"))
        .when()
        .post("/api/v1/urls")
        .then()
        .statusCode(400)
        .body("type", equalTo("https://siraj.dev/problems/invalid-url"));
  }

  @Test
  public void internalHostIs422() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("longUrl", "http://169.254.169.254/latest/meta-data"))
        .when()
        .post("/api/v1/urls")
        .then()
        .statusCode(422)
        .contentType("application/problem+json")
        .body("detail", equalTo("Host is not permitted"));
  }

  @Test
  public void badSchemeIs400() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("longUrl", "ftp://example.com/file"))
        .when()
        .post("/api/v1/urls")
        .then()
        .statusCode(400)
        .body("type", equalTo("https://siraj.dev/problems/invalid-url"));
  }

  @Test
  public void blankBodyFailsBeanValidation() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("longUrl", ""))
        .when()
        .post("/api/v1/urls")
        .then()
        .statusCode(400)
        .body("type", equalTo("https://siraj.dev/problems/validation-failed"));
  }

  @Test
  public void reservedAliasIs400() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("longUrl", "https://example.com", "customAlias", "actuator"))
        .when()
        .post("/api/v1/urls")
        .then()
        .statusCode(400)
        .body("type", equalTo("https://siraj.dev/problems/invalid-alias"));
  }

  @Test
  public void redirectMappingDoesNotShadowActuator() {
    given().when().get("/actuator/health").then().statusCode(200);
  }

  @Test
  public void errorBodiesNeverLeakStackTraces() {
    given()
        .when()
        .get("/zzzz9998")
        .then()
        .body("trace", nullValue())
        .body("exception", nullValue())
        .body("message", nullValue());
  }
}
