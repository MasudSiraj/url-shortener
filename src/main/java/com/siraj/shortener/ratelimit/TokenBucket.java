package com.siraj.shortener.ratelimit;

import java.time.Duration;
import java.time.Instant;

/**
 * Token bucket with steady refill (docs/06 Q8). Chosen over a fixed window because a fixed window
 * permits a 2x burst across the boundary. Not thread-safe on its own — callers synchronize.
 */
public class TokenBucket {

  private final int capacity;
  private final double tokensPerSecond;

  private double tokens;
  private Instant lastRefill;

  public TokenBucket(int capacityPerMinute, Instant now) {
    this.capacity = capacityPerMinute;
    this.tokensPerSecond = capacityPerMinute / 60.0;
    this.tokens = capacityPerMinute;
    this.lastRefill = now;
  }

  /**
   * @return true if a token was consumed; false if the bucket is empty.
   */
  public synchronized boolean tryConsume(Instant now) {
    refill(now);
    if (tokens >= 1.0) {
      tokens -= 1.0;
      return true;
    }
    return false;
  }

  /** Seconds until at least one token is available; never less than 1 (docs/06 Q11). */
  public synchronized long retryAfterSeconds(Instant now) {
    refill(now);
    if (tokens >= 1.0) {
      return 0;
    }
    return Math.max(1L, (long) Math.ceil((1.0 - tokens) / tokensPerSecond));
  }

  private void refill(Instant now) {
    long millis = Duration.between(lastRefill, now).toMillis();
    if (millis <= 0) {
      return;
    }
    tokens = Math.min(capacity, tokens + (millis / 1000.0) * tokensPerSecond);
    lastRefill = now;
  }
}
