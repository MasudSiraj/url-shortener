package com.siraj.shortener.infrastructure;

import com.siraj.shortener.application.ShortCodeGenerator;
import com.siraj.shortener.config.ShortenerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Alternative generator (ADR-002): base62 encoding of a DB sequence. Collision-free by construction
 * but enumerable — selected with {@code shortener.generator=sequence}. Codes are left-padded to the
 * configured length.
 */
@Component
@ConditionalOnProperty(prefix = "shortener", name = "generator", havingValue = "sequence")
public class SequenceBase62Generator implements ShortCodeGenerator {

  static final String ALPHABET = RandomBase62Generator.ALPHABET;
  private static final String NEXT = "SELECT nextval('short_code_seq')";

  private final JdbcTemplate jdbc;
  private final int length;

  public SequenceBase62Generator(JdbcTemplate jdbc, ShortenerProperties props) {
    this.jdbc = jdbc;
    this.length = props.codeLength();
  }

  @Override
  public String next() {
    Long value = jdbc.queryForObject(NEXT, Long.class);
    return encode(value == null ? 0L : value, length);
  }

  static String encode(long value, int minLength) {
    StringBuilder sb = new StringBuilder();
    long v = value;
    do {
      sb.append(ALPHABET.charAt((int) (v % 62)));
      v /= 62;
    } while (v > 0);
    while (sb.length() < minLength) {
      sb.append('0');
    }
    return sb.reverse().toString();
  }
}
