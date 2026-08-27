package com.siraj.shortener.application;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Rules shared by generated codes and user-supplied aliases. */
@Component
public class AliasPolicy {

  static final int MIN_LENGTH = 4;
  static final int MAX_LENGTH = 16;
  static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9_-]+$");

  /** Paths owned by the service; an alias must never shadow them. */
  static final Set<String> RESERVED =
      Set.of("api", "actuator", "health", "swagger-ui", "swagger-ui.html", "v3", "h2-console");

  public void validateAlias(String alias) {
    if (alias.length() < MIN_LENGTH || alias.length() > MAX_LENGTH) {
      throw new InvalidAliasException(
          "Alias must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
    }
    if (!ALLOWED.matcher(alias).matches()) {
      throw new InvalidAliasException("Alias may contain only letters, digits, '-' and '_'");
    }
    if (RESERVED.contains(alias.toLowerCase(Locale.ROOT))) {
      throw new InvalidAliasException("Alias is reserved");
    }
  }
}
