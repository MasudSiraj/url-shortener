package com.siraj.shortener;

import io.restassured.RestAssured;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.BeforeClass;

/**
 * Base class for *IT tests (ADR-003): boots the app on a random port against a real PostgreSQL
 * container. One container per JVM. Concurrency and constraint tests must extend this, not run on
 * H2.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractPostgresIT extends AbstractTestNGSpringContextTests {

  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("shortener")
          .withUsername("shortener")
          .withPassword("shortener");

  static {
    POSTGRES.start();
  }

  @LocalServerPort protected int port;

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("shortener.analytics.ip-hash-salt", () -> "it-salt");
  }

  @BeforeClass
  public void configureRestAssured() {
    RestAssured.port = port;
  }
}
