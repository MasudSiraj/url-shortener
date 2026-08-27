package com.siraj.shortener.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.siraj.shortener.application.ShortCodeGenerator;
import com.siraj.shortener.config.ShortenerProperties;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testng.annotations.Test;

public class SequenceBase62GeneratorTest extends ShortCodeGeneratorContract {

  private static ShortenerProperties props() {
    return new ShortenerProperties(
        "http://localhost",
        "sequence",
        7,
        3,
        new ShortenerProperties.Cache(100, Duration.ofMinutes(1)),
        new ShortenerProperties.Analytics("salt"),
        new ShortenerProperties.RateLimit(false, 10, 100, 1000));
  }

  /** In-memory stand-in for the DB sequence. */
  private static JdbcTemplate fakeSequence(long start) {
    AtomicLong counter = new AtomicLong(start);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    when(jdbc.queryForObject(anyString(), eq(Long.class)))
        .thenAnswer(inv -> counter.getAndIncrement());
    return jdbc;
  }

  private final ShortCodeGenerator gen =
      new SequenceBase62Generator(fakeSequence(14_776_336L), props());

  @Override
  protected ShortCodeGenerator generator() {
    return gen;
  }

  @Test
  public void encodesKnownValues() {
    assertThat(SequenceBase62Generator.encode(0, 1)).isEqualTo("0");
    assertThat(SequenceBase62Generator.encode(61, 1)).isEqualTo("z");
    assertThat(SequenceBase62Generator.encode(62, 1)).isEqualTo("10");
    assertThat(SequenceBase62Generator.encode(14_776_336L, 1)).isEqualTo("10000");
  }

  @Test
  public void padsToConfiguredLength() {
    assertThat(SequenceBase62Generator.encode(1, 7)).isEqualTo("0000001");
  }

  @Test
  public void isMonotonicAndDeterministic() {
    ShortCodeGenerator g = new SequenceBase62Generator(fakeSequence(100), props());
    assertThat(g.next()).isEqualTo("000001c");
    assertThat(g.next()).isEqualTo("000001d");
  }
}
