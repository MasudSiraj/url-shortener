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
    Analytics analytics) {

  public record Cache(@Min(1) long maxSize, Duration ttl) {}

  public record Analytics(@NotBlank String ipHashSalt) {}
}
