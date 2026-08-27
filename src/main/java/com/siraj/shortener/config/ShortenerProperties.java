package com.siraj.shortener.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Typed binding for the {@code shortener.*} block in application.yml. */
@Validated
@ConfigurationProperties(prefix = "shortener")
public record ShortenerProperties(
    @NotBlank String baseUrl,
    @NotBlank String generator,
    @Min(4) int codeLength,
    @Min(1) int collisionRetries,
    Cache cache,
    Analytics analytics,
    RateLimit rateLimit) {

  public record Cache(@Min(1) long maxSize, Duration ttl) {}

  public record Analytics(@NotBlank String ipHashSalt) {}

  /**
   * Rate-limit policy (docs/06 §1.3). Limits are configuration, not constants, so they can be tuned
   * without a rebuild (Q9).
   */
  public record RateLimit(
      boolean enabled,
      @Min(1) int createPerMinute,
      @Min(1) int redirectPerMinute,
      @Min(1) long maxTrackedClients) {}
}
