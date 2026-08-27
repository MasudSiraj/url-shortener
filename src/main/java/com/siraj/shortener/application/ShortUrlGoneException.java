package com.siraj.shortener.application;

/** Link existed but is expired or deleted (410). */
public class ShortUrlGoneException extends RuntimeException {
  public ShortUrlGoneException(String code) {
    super("Short code no longer available: " + code);
  }
}
