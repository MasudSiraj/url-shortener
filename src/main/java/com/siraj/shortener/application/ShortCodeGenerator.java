package com.siraj.shortener.application;

/** Strategy port for short-code allocation (ADR-002). Implementations must be thread-safe. */
public interface ShortCodeGenerator {

  String next();
}
