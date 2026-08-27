package com.siraj.shortener.infrastructure;

import com.siraj.shortener.application.ShortCodeGenerator;
import com.siraj.shortener.config.ShortenerProperties;
import java.security.SecureRandom;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default generator (ADR-002): N characters drawn uniformly from base62 with SecureRandom.
 * Uniqueness is guaranteed by the DB unique index plus service-level retry, not here.
 */
@Component
@ConditionalOnProperty(
    prefix = "shortener",
    name = "generator",
    havingValue = "random",
    matchIfMissing = true)
public class RandomBase62Generator implements ShortCodeGenerator {

  static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  private final SecureRandom random = new SecureRandom();
  private final int length;

  public RandomBase62Generator(ShortenerProperties props) {
    this.length = props.codeLength();
  }

  @Override
  public String next() {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return sb.toString();
  }
}
