package com.siraj.shortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.siraj.shortener.config.ShortenerProperties;
import com.siraj.shortener.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class InMemoryRateLimiterTest {

  private MutableClock clock;
  private InMemoryRateLimiter limiter;

  private static ShortenerProperties props() {
    return new ShortenerProperties(
        "http://localhost",
        "random",
        7,
        3,
        new ShortenerProperties.Cache(100, Duration.ofMinutes(1)),
        new ShortenerProperties.Analytics("salt"),
        new ShortenerProperties.RateLimit(true, 3, 5, 1000));
  }

  @BeforeMethod
  public void setUp() {
    clock = new MutableClock(Instant.parse("2026-08-27T00:00:00Z"));
    limiter = new InMemoryRateLimiter(props(), clock);
  }

  @Test
  public void createBucketUsesCreateLimit() {
    for (int i = 0; i < 3; i++) {
      assertThat(limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE).allowed()).isTrue();
    }
    assertThat(limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE).allowed()).isFalse();
  }

  @Test
  public void readBucketIsIndependentOfCreateBucket() {
    for (int i = 0; i < 3; i++) {
      limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE);
    }
    assertThat(limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE).allowed()).isFalse();
    assertThat(limiter.check("1.1.1.1", RateLimiter.Bucket.READ).allowed()).isTrue();
  }

  @Test
  public void clientsAreIsolatedFromEachOther() {
    for (int i = 0; i < 3; i++) {
      limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE);
    }
    assertThat(limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE).allowed()).isFalse();
    assertThat(limiter.check("2.2.2.2", RateLimiter.Bucket.CREATE).allowed()).isTrue();
  }

  @Test
  public void deniedDecisionCarriesRetryAfter() {
    for (int i = 0; i < 3; i++) {
      limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE);
    }
    assertThat(limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE).retryAfterSeconds())
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  public void tokensReturnAsTimeAdvances() {
    for (int i = 0; i < 3; i++) {
      limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE);
    }
    assertThat(limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE).allowed()).isFalse();
    clock.advance(Duration.ofMinutes(1));
    assertThat(limiter.check("1.1.1.1", RateLimiter.Bucket.CREATE).allowed()).isTrue();
  }
}
