package com.siraj.shortener;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.springframework.test.context.TestPropertySource;
import org.testng.annotations.Test;

/**
 * Integration smoke: service boots against real Postgres, V1 applies, health reports UP (A3/A4).
 */
@TestPropertySource(properties = "shortener.rate-limit.enabled=false")
public class HealthIT extends AbstractPostgresIT {

  @Test
  public void healthIsUp() {
    given().when().get("/actuator/health").then().statusCode(200).body("status", equalTo("UP"));
  }
}
