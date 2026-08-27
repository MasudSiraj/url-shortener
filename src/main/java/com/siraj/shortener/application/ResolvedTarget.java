package com.siraj.shortener.application;

/** Result of resolving a short code: the target and the link id for downstream analytics (D-2). */
public record ResolvedTarget(long shortUrlId, String longUrl) {}
