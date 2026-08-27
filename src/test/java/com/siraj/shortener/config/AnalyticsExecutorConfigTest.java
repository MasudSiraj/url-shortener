package com.siraj.shortener.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

/**
 * Finding T-2 (docs/08) / planned in docs/05 §1.7: a saturated analytics queue must drop events and
 * count them, never block the caller and never throw.
 */
public class AnalyticsExecutorConfigTest {

  @Test
  public void saturatedQueueDropsEventsAndIncrementsCounterWithoutThrowing() throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    Executor executor = new AnalyticsExecutorConfig().analyticsExecutor(registry);

    CountDownLatch block = new CountDownLatch(1);
    CountDownLatch started = new CountDownLatch(2);

    // Occupy both core threads so everything else queues.
    for (int i = 0; i < 2; i++) {
      executor.execute(
          () -> {
            started.countDown();
            try {
              block.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          });
    }
    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

    // Queue capacity is 1000 and max pool is 4; push well past both.
    assertThatCode(
            () -> {
              for (int i = 0; i < 3000; i++) {
                executor.execute(() -> {});
              }
            })
        .as("rejection must never propagate to the caller")
        .doesNotThrowAnyException();

    assertThat(registry.counter(AnalyticsExecutorConfig.DROPPED_METRIC).count())
        .as("dropped events are counted so loss is visible")
        .isGreaterThan(0.0);

    block.countDown();
  }
}
