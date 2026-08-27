package com.siraj.shortener.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.siraj.shortener.config.ShortenerProperties;
import java.time.Duration;
import org.testng.annotations.Test;

public class IpHasherTest {

  private static IpHasher hasher(String salt) {
    return new IpHasher(
        new ShortenerProperties(
            "http://localhost",
            "random",
            7,
            3,
            new ShortenerProperties.Cache(100, Duration.ofMinutes(1)),
            new ShortenerProperties.Analytics(salt),
            new ShortenerProperties.RateLimit(false, 10, 100, 1000)));
  }

  @Test
  public void producesSixtyFourHexCharsAndNeverContainsTheIp() {
    String h = hasher("s1").hash("203.0.113.7");
    assertThat(h).hasSize(64).matches("[0-9a-f]{64}").doesNotContain("203.0.113");
  }

  @Test
  public void isDeterministicForSameSaltAndIp() {
    assertThat(hasher("s1").hash("1.2.3.4")).isEqualTo(hasher("s1").hash("1.2.3.4"));
  }

  @Test
  public void differentSaltGivesDifferentHash() {
    assertThat(hasher("s1").hash("1.2.3.4")).isNotEqualTo(hasher("s2").hash("1.2.3.4"));
  }

  @Test
  public void toleratesNullIp() {
    assertThat(hasher("s1").hash(null)).hasSize(64);
  }
}
