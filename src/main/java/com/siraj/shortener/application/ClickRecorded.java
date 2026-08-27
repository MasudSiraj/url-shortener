package com.siraj.shortener.application;

import java.time.Instant;

/**
 * Published after a successful redirect. Carries primitives only — never an entity — so it is safe
 * to hand to another thread (docs/05 §1.8).
 */
public record ClickRecorded(
    long shortUrlId, Instant clickedAt, String referrer, String userAgent, String clientIp) {}
