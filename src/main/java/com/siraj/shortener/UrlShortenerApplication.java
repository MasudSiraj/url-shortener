package com.siraj.shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/** Entry point. Layering rule: api -> application -> domain <- infrastructure (ADR-001). */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class UrlShortenerApplication {

  public static void main(String[] args) {
    SpringApplication.run(UrlShortenerApplication.class, args);
  }
}
