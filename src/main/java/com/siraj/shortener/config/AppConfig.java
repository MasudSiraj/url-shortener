package com.siraj.shortener.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Framework-level beans. A Clock bean makes time injectable so tests are deterministic. */
@Configuration
@EnableConfigurationProperties(ShortenerProperties.class)
public class AppConfig {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
