package com.siraj.shortener.api;

import com.siraj.shortener.application.ShortUrlView;
import java.time.Instant;

/** Response body for create/get. Kept separate from the read model so API shape can evolve. */
public record ShortUrlResponse(
    String shortCode,
    String shortUrl,
    String longUrl,
    boolean customAlias,
    Instant createdAt,
    Instant expiresAt,
    Instant deletedAt) {

  static ShortUrlResponse from(ShortUrlView v) {
    return new ShortUrlResponse(
        v.code(),
        v.shortUrl(),
        v.longUrl(),
        v.customAlias(),
        v.createdAt(),
        v.expiresAt(),
        v.deletedAt());
  }
}
