package com.siraj.shortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.testng.annotations.Test;

public class TokenBucketTest {

  private static final Instant T0 = Instant.parse("2026-08-27T00:00:00Z");

  @Test
  public void allowsUpToCapacityThenDenies() {
    TokenBucket b = new TokenBucket(10, T0);
    for (int i = 0; i < 10; i++) {
      assertThat(b.tryConsume(T0)).as("request %s", i + 1).isTrue();
    }
    assertThat(b.tryConsume(T0)).isFalse();
  }

  @Test
  public void refillsSteadilyRatherThanAllOnceAtWindowEnd() {
    TokenBucket b = new TokenBucket(60, T0); // 1 token/second
    for (int i = 0; i < 60; i++) {
      b.tryConsume(T0);
    }
    assertThat(b.tryConsume(T0)).isFalse();
    assertThat(b.tryConsume(T0.plusSeconds(1))).isTrue();
    assertThat(b.tryConsume(T0.plusSeconds(1))).isFalse();
  }

  @Test
  public void neverExceedsCapacityWhenIdle() {
    TokenBucket b = new TokenBucket(10, T0);
    Instant later = T0.plus(Duration.ofHours(1));
    for (int i = 0; i < 10; i++) {
      assertThat(b.tryConsume(later)).isTrue();
    }
    assertThat(b.tryConsume(later)).isFalse();
  }

  @Test
  public void retryAfterIsZeroWhenTokensRemainAndAtLeastOneWhenEmpty() {
    TokenBucket b = new TokenBucket(60, T0);
    assertThat(b.retryAfterSeconds(T0)).isZero();
    for (int i = 0; i < 60; i++) {
      b.tryConsume(T0);
    }
    assertThat(b.retryAfterSeconds(T0)).isGreaterThanOrEqualTo(1);
  }
}
