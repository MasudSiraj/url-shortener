package com.siraj.shortener.application;

import com.siraj.shortener.domain.ShortUrl;
import java.time.Instant;

/** Read model returned by the service; the api layer maps it to JSON. */
public record ShortUrlView(
    String code,
    String shortUrl,
    String longUrl,
    boolean customAlias,
    Instant createdAt,
    Instant expiresAt,
    Instant deletedAt) {

  static ShortUrlView from(ShortUrl e, String baseUrl) {
    return new ShortUrlView(
        e.getCode(),
        baseUrl + "/" + e.getCode(),
        e.getLongUrl(),
        e.isCustomAlias(),
        e.getCreatedAt(),
        e.getExpiresAt(),
        e.getDeletedAt());
  }
}
