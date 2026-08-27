package com.siraj.shortener.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.siraj.shortener.config.ShortenerProperties;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.testng.annotations.Test;

public class RandomBase62GeneratorTest {

  private static ShortenerProperties props(int length) {
    return new ShortenerProperties(
        "http://localhost",
        "random",
        length,
        3,
        new ShortenerProperties.Cache(100, Duration.ofMinutes(1)),
        new ShortenerProperties.Analytics("salt"));
  }

  @Test
  public void generatesCodesOfConfiguredLengthAndAlphabet() {
    RandomBase62Generator gen = new RandomBase62Generator(props(7));
    for (int i = 0; i < 10_000; i++) {
      assertThat(gen.next()).matches("[0-9A-Za-z]{7}");
    }
  }

  @Test
  public void respectsCodeLength() {
    assertThat(new RandomBase62Generator(props(5)).next()).hasSize(5);
    assertThat(new RandomBase62Generator(props(10)).next()).hasSize(10);
  }

  @Test
  public void producesNoDuplicatesInASmallSample() {
    RandomBase62Generator gen = new RandomBase62Generator(props(7));
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 10_000; i++) {
      seen.add(gen.next());
    }
    // 62^7 keyspace: a duplicate in 10k draws would indicate a broken RNG, not bad luck.
    assertThat(seen).hasSize(10_000);
  }
}
