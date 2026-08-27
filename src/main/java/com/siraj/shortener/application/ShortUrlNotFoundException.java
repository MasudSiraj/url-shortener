package com.siraj.shortener.application;

/** No link exists for the code (404). */
public class ShortUrlNotFoundException extends RuntimeException {
  public ShortUrlNotFoundException(String code) {
    super("Unknown short code: " + code);
  }
}
