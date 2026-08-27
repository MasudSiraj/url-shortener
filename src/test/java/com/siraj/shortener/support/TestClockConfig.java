package com.siraj.shortener.support;

import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the production Clock in integration tests. The bean is named differently from
 * AppConfig.clock() so it does not collide; @Primary makes it win wherever a Clock is injected.
 */
@TestConfiguration
public class TestClockConfig {

  public static final Instant START = Instant.parse("2026-08-27T00:00:00Z");

  @Bean
  @Primary
  public MutableClock testClock() {
    return new MutableClock(START);
  }
}
