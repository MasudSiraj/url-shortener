package com.siraj.shortener.application;

/** Custom alias is already taken (409). */
public class AliasConflictException extends RuntimeException {
  public AliasConflictException(String alias) {
    super("Alias already in use: " + alias);
  }
}
