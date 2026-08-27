package com.siraj.shortener.ratelimit;

/**
 * Port for rate limiting. In-process today; a Redis implementation would slot in here (docs/06
 * §1.6).
 */
public interface RateLimiter {

  /**
   * @return a decision for one request by {@code clientId} against the given bucket.
   */
  Decision check(String clientId, Bucket bucket);

  /** Which policy applies to a request. */
  enum Bucket {
    CREATE,
    READ
  }

  /** Outcome of a limit check. {@code retryAfterSeconds} is meaningful only when not allowed. */
  record Decision(boolean allowed, long retryAfterSeconds) {

    public static Decision allow() {
      return new Decision(true, 0);
    }

    public static Decision deny(long retryAfter) {
      return new Decision(false, retryAfter);
    }
  }
}
