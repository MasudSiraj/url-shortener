package com.siraj.shortener.api;

import com.siraj.shortener.application.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Analytics read API (task C4). */
@RestController
@RequestMapping(path = "/api/v1/urls", produces = "application/json")
public class StatsController {

  private final AnalyticsService analytics;

  public StatsController(AnalyticsService analytics) {
    this.analytics = analytics;
  }

  @GetMapping("/{code}/stats")
  public StatsResponse stats(@PathVariable String code) {
    return StatsResponse.from(analytics.statsFor(code));
  }
}
