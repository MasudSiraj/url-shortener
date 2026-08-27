package com.siraj.shortener.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.siraj.shortener.config.ShortenerProperties;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Per-client token buckets held in a bounded, expiring cache. The bound matters: an unbounded map
 * keyed by client IP would itself be a memory-exhaustion vector (docs/06 Q14).
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

  private final Cache<String, TokenBucket> buckets;
  private final Clock clock;
  private final int createPerMinute;
  private final int redirectPerMinute;

  public InMemoryRateLimiter(ShortenerProperties props, Clock clock) {
    this.clock = clock;
    this.createPerMinute = props.rateLimit().createPerMinute();
    this.redirectPerMinute = props.rateLimit().redirectPerMinute();
    this.buckets =
        Caffeine.newBuilder()
            .maximumSize(props.rateLimit().maxTrackedClients())
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
  }

  @Override
  public Decision check(String clientId, Bucket bucket) {
    int capacity = bucket == Bucket.CREATE ? createPerMinute : redirectPerMinute;
    var now = clock.instant();
    TokenBucket tb = buckets.get(clientId + ":" + bucket, k -> new TokenBucket(capacity, now));
    return tb.tryConsume(now) ? Decision.allow() : Decision.deny(tb.retryAfterSeconds(now));
  }
}
