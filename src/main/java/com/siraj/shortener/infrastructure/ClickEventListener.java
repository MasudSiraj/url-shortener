package com.siraj.shortener.infrastructure;

import com.siraj.shortener.application.ClickRecorded;
import com.siraj.shortener.application.IpHasher;
import com.siraj.shortener.config.AnalyticsExecutorConfig;
import com.siraj.shortener.domain.ClickEvent;
import com.siraj.shortener.domain.ClickEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Persists clicks off the request thread. Any failure is logged, never propagated (ADR-004). */
@Component
public class ClickEventListener {

  private static final Logger log = LoggerFactory.getLogger(ClickEventListener.class);
  private static final int MAX_REFERRER = 2048;
  private static final int MAX_UA = 512;

  private final ClickEventRepository repository;
  private final IpHasher hasher;

  public ClickEventListener(ClickEventRepository repository, IpHasher hasher) {
    this.repository = repository;
    this.hasher = hasher;
  }

  @Async(AnalyticsExecutorConfig.EXECUTOR)
  @EventListener
  public void on(ClickRecorded event) {
    try {
      repository.save(
          new ClickEvent(
              event.shortUrlId(),
              event.clickedAt(),
              truncate(event.referrer(), MAX_REFERRER),
              truncate(event.userAgent(), MAX_UA),
              hasher.hash(event.clientIp())));
    } catch (RuntimeException e) {
      // Redirect already returned; analytics must never affect it.
      log.warn("Failed to persist click for shortUrlId={}", event.shortUrlId(), e);
    }
  }

  static String truncate(String s, int max) {
    if (s == null) {
      return null;
    }
    return s.length() <= max ? s : s.substring(0, max);
  }
}
