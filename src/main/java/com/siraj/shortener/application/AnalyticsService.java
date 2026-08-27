package com.siraj.shortener.application;

import com.siraj.shortener.domain.ClickEvent;
import com.siraj.shortener.domain.ClickEventRepository;
import com.siraj.shortener.domain.ShortUrl;
import com.siraj.shortener.domain.ShortUrlRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates click events for the stats endpoint. Aggregation is in-memory over the last 30 days —
 * acceptable at prototype scale, bounded by the index on (short_url_id, clicked_at). Deleted or
 * expired links still report stats (ADR-007, D-5).
 */
@Service
public class AnalyticsService {

  static final int WINDOW_DAYS = 30;
  static final int TOP_REFERRERS = 5;

  private final ShortUrlRepository urls;
  private final ClickEventRepository clicks;
  private final Clock clock;

  public AnalyticsService(ShortUrlRepository urls, ClickEventRepository clicks, Clock clock) {
    this.urls = urls;
    this.clicks = clicks;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public StatsView statsFor(String code) {
    ShortUrl url = urls.findByCode(code).orElseThrow(() -> new ShortUrlNotFoundException(code));
    Instant now = clock.instant();
    Instant since = now.minus(Duration.ofDays(WINDOW_DAYS));
    List<ClickEvent> recent =
        clicks.findByShortUrlIdAndClickedAtGreaterThanEqual(url.getId(), since);

    Map<LocalDate, Long> byDay =
        recent.stream()
            .collect(
                Collectors.groupingBy(
                    e -> LocalDate.ofInstant(e.getClickedAt(), ZoneOffset.UTC),
                    TreeMap::new,
                    Collectors.counting()));

    List<StatsView.ReferrerCount> top =
        recent.stream()
            .filter(e -> e.getReferrer() != null && !e.getReferrer().isBlank())
            .collect(Collectors.groupingBy(ClickEvent::getReferrer, Collectors.counting()))
            .entrySet()
            .stream()
            .sorted(
                Comparator.comparingLong((Map.Entry<String, Long> e) -> e.getValue())
                    .reversed()
                    .thenComparing(Map.Entry::getKey))
            .limit(TOP_REFERRERS)
            .map(e -> new StatsView.ReferrerCount(e.getKey(), e.getValue()))
            .toList();

    return new StatsView(
        code,
        clicks.countByShortUrlId(url.getId()),
        byDay.entrySet().stream()
            .map(e -> new StatsView.DayCount(e.getKey(), e.getValue()))
            .toList(),
        top);
  }
}
