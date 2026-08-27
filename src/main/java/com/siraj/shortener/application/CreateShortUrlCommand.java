package com.siraj.shortener.application;

import java.time.Instant;

/** Input to {@link UrlService#create}. HTTP-agnostic. */
public record CreateShortUrlCommand(String longUrl, String customAlias, Instant expiresAt) {}
