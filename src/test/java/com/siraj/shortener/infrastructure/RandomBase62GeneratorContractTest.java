package com.siraj.shortener.infrastructure;

import com.siraj.shortener.application.ShortCodeGenerator;
import com.siraj.shortener.config.ShortenerProperties;
import java.time.Duration;

public class RandomBase62GeneratorContractTest extends ShortCodeGeneratorContract {

  private final ShortCodeGenerator gen =
      new RandomBase62Generator(
          new ShortenerProperties(
              "http://localhost",
              "random",
              7,
              3,
              new ShortenerProperties.Cache(100, Duration.ofMinutes(1)),
              new ShortenerProperties.Analytics("salt")));

  @Override
  protected ShortCodeGenerator generator() {
    return gen;
  }
}
