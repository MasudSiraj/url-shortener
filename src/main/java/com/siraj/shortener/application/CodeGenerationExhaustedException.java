package com.siraj.shortener.application;

/** Generator collided on every retry — should be vanishingly rare (503). */
public class CodeGenerationExhaustedException extends RuntimeException {
  public CodeGenerationExhaustedException(int attempts) {
    super("Could not allocate a unique short code after " + attempts + " attempts");
  }
}
