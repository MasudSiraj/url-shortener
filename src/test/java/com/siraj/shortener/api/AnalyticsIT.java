package com.siraj.shortener.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.siraj.shortener.AbstractPostgresIT;
import com.siraj.shortener.domain.ClickEvent;
import com.siraj.shortener.domain.ClickEventRepository;
import com.siraj.shortener.support.MutableClock;
import com.siraj.shortener.support.TestClockConfig;
import io.restassured.http.ContentType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Brownfield task C3/C4: analytics on real PostgreSQL. */
@TestPropertySource(properties = "shortener.rate-limit.enabled=false")
public class AnalyticsIT extends AbstractPostgresIT {

  @Autowired private MutableClock clock;
  @Autowired private ClickEventRepository clicks;

  @BeforeMethod
  public void resetClock() {
    clock.set(TestClockConfig.START);
  }

  private String create(String url) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("longUrl", url))
        .post("/api/v1/urls")
        .then()
        .statusCode(201)
        .extract()
        .path("shortCode");
  }

  @Test
  public void redirectReturnsThenClickIsPersistedAsynchronously() {
    String code = create("https://example.com/analytics-1");
    long before = clicks.count();

    given()
        .redirects()
        .follow(false)
        .header("Referer", "https://news.example/story")
        .header("User-Agent", "IT-Agent/1.0")
        .when()
        .get("/" + code)
        .then()
        .statusCode(302);

    // Persistence happens on the analytics executor after the response is written.
    Awaitility.await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(() -> assertThat(clicks.count()).isEqualTo(before + 1));

    List<ClickEvent> all = clicks.findAll();
    ClickEvent last = all.get(all.size() - 1);
    assertThat(last.getReferrer()).isEqualTo("https://news.example/story");
    assertThat(last.getUserAgent()).isEqualTo("IT-Agent/1.0");
    assertThat(last.getIpHash()).hasSize(64).doesNotContain("127.0.0.1").doesNotContain("0:0:0");
    assertThat(last.getClickedAt()).isEqualTo(TestClockConfig.START);
  }

  @Test
  public void statsAggregateByDayAndReferrer() {
    String code = create("https://example.com/analytics-2");

    clickWithReferrer(code, "https://a.example");
    clickWithReferrer(code, "https://a.example");
    clock.advance(Duration.ofDays(1));
    clickWithReferrer(code, "https://b.example");

    Awaitility.await()
        .atMost(Duration.ofSeconds(3))
        .untilAsserted(
            () ->
                given()
                    .get("/api/v1/urls/" + code + "/stats")
                    .then()
                    .statusCode(200)
                    .body("shortCode", equalTo(code))
                    .body("totalClicks", equalTo(3))
                    .body("clicksByDay", hasSize(2))
                    .body("clicksByDay[0].count", equalTo(2))
                    .body("clicksByDay[1].count", equalTo(1))
                    .body("topReferrers[0].referrer", equalTo("https://a.example"))
                    .body("topReferrers[0].count", equalTo(2)));
  }

  @Test
  public void statsRemainAvailableForDeletedLink() {
    String code = create("https://example.com/analytics-3");
    clickWithReferrer(code, null);
    Awaitility.await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(
            () ->
                given()
                    .get("/api/v1/urls/" + code + "/stats")
                    .then()
                    .body("totalClicks", equalTo(1)));

    given().delete("/api/v1/urls/" + code).then().statusCode(204);
    given().redirects().follow(false).get("/" + code).then().statusCode(410);

    given()
        .get("/api/v1/urls/" + code + "/stats")
        .then()
        .statusCode(200)
        .body("totalClicks", equalTo(1));
  }

  @Test
  public void statsForUnknownCodeIs404() {
    given().get("/api/v1/urls/zzzz9997/stats").then().statusCode(404);
  }

  private void clickWithReferrer(String code, String referrer) {
    var req = given().redirects().follow(false);
    if (referrer != null) {
      req = req.header("Referer", referrer);
    }
    req.when().get("/" + code).then().statusCode(302);
  }
}
