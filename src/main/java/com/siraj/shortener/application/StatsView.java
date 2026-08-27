package com.siraj.shortener.application;

import java.time.LocalDate;
import java.util.List;

/** Read model for GET /api/v1/urls/{code}/stats. */
public record StatsView(
    String code, long totalClicks, List<DayCount> clicksByDay, List<ReferrerCount> topReferrers) {

  public record DayCount(LocalDate date, long count) {}

  public record ReferrerCount(String referrer, long count) {}
}
