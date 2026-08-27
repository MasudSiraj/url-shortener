package com.siraj.shortener.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request body for POST /api/v1/urls. Bean Validation covers shape; UrlValidator covers policy. */
public record CreateShortUrlRequest(
    @NotBlank @Size(max = 2048) String longUrl,
    @Size(min = 4, max = 16) String customAlias,
    Instant expiresAt) {}
