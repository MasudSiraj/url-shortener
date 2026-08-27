package com.siraj.shortener.application;

/** Custom alias violates charset, length, or reserved-word rules (400). */
public class InvalidAliasException extends RuntimeException {
  public InvalidAliasException(String message) {
    super(message);
  }
}
