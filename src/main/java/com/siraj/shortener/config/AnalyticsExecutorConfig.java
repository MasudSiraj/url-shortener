package com.siraj.shortener.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded executor for click-event persistence (ADR-004, D-3). Rejection discards the event and
 * increments a counter — never blocks the request thread (CallerRunsPolicy rejected on purpose).
 */
@Configuration
public class AnalyticsExecutorConfig {

  public static final String EXECUTOR = "analyticsExecutor";
  public static final String DROPPED_METRIC = "analytics.events.dropped";

  private static final Logger log = LoggerFactory.getLogger(AnalyticsExecutorConfig.class);

  @Bean(name = EXECUTOR)
  public Executor analyticsExecutor(MeterRegistry registry) {
    Counter dropped = registry.counter(DROPPED_METRIC);
    ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
    ex.setThreadNamePrefix("analytics-");
    ex.setCorePoolSize(2);
    ex.setMaxPoolSize(4);
    ex.setQueueCapacity(1000);
    ex.setRejectedExecutionHandler(
        (r, executor) -> {
          dropped.increment();
          log.warn("Analytics queue full; click event dropped (total={})", (long) dropped.count());
        });
    ex.initialize();
    return ex;
  }
}
