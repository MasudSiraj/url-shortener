package com.siraj.shortener.application;

/** The supplied long URL failed syntactic or safety validation (400 / 422). */
public class InvalidUrlException extends RuntimeException {

  private final boolean unsafeHost;

  public InvalidUrlException(String message, boolean unsafeHost) {
    super(message);
    this.unsafeHost = unsafeHost;
  }

  public boolean isUnsafeHost() {
    return unsafeHost;
  }
}
