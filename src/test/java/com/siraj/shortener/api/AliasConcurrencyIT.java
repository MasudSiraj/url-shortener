package com.siraj.shortener.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.siraj.shortener.AbstractPostgresIT;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.test.context.TestPropertySource;
import org.testng.annotations.Test;

/**
 * Brownfield task C5: reproduce the alias race. N clients POST the same custom alias at once.
 * Expected contract: exactly one 201, every other request 409 Conflict. Anything else — in
 * particular a 500 — is the defect.
 */
@TestPropertySource(properties = "shortener.rate-limit.enabled=false")
public class AliasConcurrencyIT extends AbstractPostgresIT {

  private static final int THREADS = 50;

  @Test
  public void concurrentCreateOfSameAliasYieldsOne201AndRest409() throws Exception {
    String alias = "race-" + System.nanoTime() % 1_000_000;
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Integer>> results = new ArrayList<>();
    int port = this.port;

    for (int i = 0; i < THREADS; i++) {
      Callable<Integer> task =
          () -> {
            start.await();
            return RestAssured.given()
                .port(port)
                .contentType(ContentType.JSON)
                .body(Map.of("longUrl", "https://example.com/race", "customAlias", alias))
                .post("/api/v1/urls")
                .statusCode();
          };
      results.add(pool.submit(task));
    }
    start.countDown();
    pool.shutdown();
    assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

    List<Integer> statuses = new ArrayList<>();
    for (Future<Integer> f : results) {
      statuses.add(f.get());
    }

    long created = statuses.stream().filter(s -> s == 201).count();
    long conflicts = statuses.stream().filter(s -> s == 409).count();
    long other = statuses.stream().filter(s -> s != 201 && s != 409).count();

    assertThat(created).as("exactly one winner").isEqualTo(1);
    assertThat(other).as("no unexpected statuses (got %s)", statuses).isZero();
    assertThat(conflicts).as("all losers get 409").isEqualTo(THREADS - 1);
  }
}
