package com.siraj.shortener.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.siraj.shortener.domain.ClickEvent;
import com.siraj.shortener.domain.ClickEventRepository;
import com.siraj.shortener.domain.ShortUrl;
import com.siraj.shortener.domain.ShortUrlRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AnalyticsServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

  private ShortUrlRepository urls;
  private ClickEventRepository clicks;
  private AnalyticsService service;
  private ShortUrl link;

  @BeforeMethod
  public void setUp() throws Exception {
    urls = mock(ShortUrlRepository.class);
    clicks = mock(ClickEventRepository.class);
    service = new AnalyticsService(urls, clicks, Clock.fixed(NOW, ZoneOffset.UTC));
    link = new ShortUrl("abc1234", "https://example.com", false, NOW, null);
    var id = ShortUrl.class.getDeclaredField("id");
    id.setAccessible(true);
    id.set(link, 42L);
    when(urls.findByCode("abc1234")).thenReturn(Optional.of(link));
  }

  private static ClickEvent click(Instant at, String referrer) {
    return new ClickEvent(42L, at, referrer, "ua", "hash");
  }

  @Test
  public void emptyStatsForLinkWithNoClicks() {
    when(clicks.countByShortUrlId(42L)).thenReturn(0L);
    when(clicks.findByShortUrlIdAndClickedAtGreaterThanEqual(eq(42L), any())).thenReturn(List.of());

    StatsView v = service.statsFor("abc1234");

    assertThat(v.totalClicks()).isZero();
    assertThat(v.clicksByDay()).isEmpty();
    assertThat(v.topReferrers()).isEmpty();
  }

  @Test
  public void groupsByDayAndRanksReferrers() {
    Instant d1 = NOW.minus(Duration.ofDays(2));
    Instant d2 = NOW.minus(Duration.ofDays(1));
    when(clicks.countByShortUrlId(42L)).thenReturn(5L);
    when(clicks.findByShortUrlIdAndClickedAtGreaterThanEqual(eq(42L), any()))
        .thenReturn(
            List.of(
                click(d1, "https://a.example"),
                click(d1, "https://b.example"),
                click(d2, "https://a.example"),
                click(d2, null),
                click(d2, "")));

    StatsView v = service.statsFor("abc1234");

    assertThat(v.totalClicks()).isEqualTo(5);
    assertThat(v.clicksByDay())
        .containsExactly(
            new StatsView.DayCount(LocalDate.ofInstant(d1, ZoneOffset.UTC), 2),
            new StatsView.DayCount(LocalDate.ofInstant(d2, ZoneOffset.UTC), 3));
    assertThat(v.topReferrers())
        .containsExactly(
            new StatsView.ReferrerCount("https://a.example", 2),
            new StatsView.ReferrerCount("https://b.example", 1));
  }

  @Test
  public void queriesOnlyTheLastThirtyDays() {
    when(clicks.countByShortUrlId(42L)).thenReturn(0L);
    when(clicks.findByShortUrlIdAndClickedAtGreaterThanEqual(
            42L, NOW.minus(Duration.ofDays(AnalyticsService.WINDOW_DAYS))))
        .thenReturn(List.of());

    assertThat(service.statsFor("abc1234").clicksByDay()).isEmpty();
  }

  @Test
  public void unknownCodeIsNotFound() {
    when(urls.findByCode("nope")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.statsFor("nope"))
        .isInstanceOf(ShortUrlNotFoundException.class);
  }
}
