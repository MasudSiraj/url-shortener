package com.siraj.shortener.api;

import com.siraj.shortener.application.AliasConflictException;
import com.siraj.shortener.application.CodeGenerationExhaustedException;
import com.siraj.shortener.application.InvalidAliasException;
import com.siraj.shortener.application.InvalidUrlException;
import com.siraj.shortener.application.ShortUrlGoneException;
import com.siraj.shortener.application.ShortUrlNotFoundException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RFC 7807 problem+json for every error (ADR-006). Persistence and framework exception messages are
 * never echoed to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String TYPE_BASE = "https://siraj.dev/problems/";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail onBeanValidation(MethodArgumentNotValidException ex) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .findFirst()
            .orElse("Request body is invalid");
    return problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed", detail);
  }

  @ExceptionHandler(InvalidUrlException.class)
  ProblemDetail onInvalidUrl(InvalidUrlException ex) {
    HttpStatus status =
        ex.isUnsafeHost() ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_REQUEST;
    return problem(status, "invalid-url", "Invalid URL", ex.getMessage());
  }

  @ExceptionHandler(InvalidAliasException.class)
  ProblemDetail onInvalidAlias(InvalidAliasException ex) {
    return problem(HttpStatus.BAD_REQUEST, "invalid-alias", "Invalid alias", ex.getMessage());
  }

  @ExceptionHandler(AliasConflictException.class)
  ProblemDetail onAliasConflict(AliasConflictException ex) {
    return problem(HttpStatus.CONFLICT, "alias-conflict", "Alias conflict", ex.getMessage());
  }

  @ExceptionHandler(ShortUrlNotFoundException.class)
  ProblemDetail onNotFound(ShortUrlNotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, "not-found", "Not found", ex.getMessage());
  }

  @ExceptionHandler(ShortUrlGoneException.class)
  ProblemDetail onGone(ShortUrlGoneException ex) {
    return problem(HttpStatus.GONE, "gone", "Gone", ex.getMessage());
  }

  @ExceptionHandler(CodeGenerationExhaustedException.class)
  ProblemDetail onExhausted(CodeGenerationExhaustedException ex) {
    log.error("Code generation exhausted", ex);
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "code-generation-exhausted",
        "Service unavailable",
        "Could not allocate a short code; please retry");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail onUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal-error",
        "Internal error",
        "An unexpected error occurred");
  }

  private static ProblemDetail problem(
      HttpStatus status, String type, String title, String detail) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(URI.create(TYPE_BASE + type));
    pd.setTitle(title);
    return pd;
  }
}
