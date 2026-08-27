package com.siraj.shortener.api;

import com.siraj.shortener.application.StatsView;
import java.time.LocalDate;
import java.util.List;

/** Response body for GET /api/v1/urls/{code}/stats. */
public record StatsResponse(
    String shortCode,
    long totalClicks,
    List<DayCount> clicksByDay,
    List<ReferrerCount> topReferrers) {

  public record DayCount(LocalDate date, long count) {}

  public record ReferrerCount(String referrer, long count) {}

  static StatsResponse from(StatsView v) {
    return new StatsResponse(
        v.code(),
        v.totalClicks(),
        v.clicksByDay().stream().map(d -> new DayCount(d.date(), d.count())).toList(),
        v.topReferrers().stream().map(r -> new ReferrerCount(r.referrer(), r.count())).toList());
  }
}
