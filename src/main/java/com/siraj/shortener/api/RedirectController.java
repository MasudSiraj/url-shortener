package com.siraj.shortener.api;

import com.siraj.shortener.application.ClickRecorded;
import com.siraj.shortener.application.ResolvedTarget;
import com.siraj.shortener.application.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hot path. The path regex keeps this mapping from swallowing /api, /actuator, /swagger-ui etc.
 * Publishes a {@link ClickRecorded} event; persistence happens asynchronously (ADR-004).
 */
@RestController
public class RedirectController {

  private final UrlService service;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public RedirectController(UrlService service, ApplicationEventPublisher events, Clock clock) {
    this.service = service;
    this.events = events;
    this.clock = clock;
  }

  @GetMapping("/{code:[A-Za-z0-9_-]{4,16}}")
  public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
    ResolvedTarget target = service.resolve(code);
    events.publishEvent(
        new ClickRecorded(
            target.shortUrlId(),
            clock.instant(),
            request.getHeader(HttpHeaders.REFERER),
            request.getHeader(HttpHeaders.USER_AGENT),
            request.getRemoteAddr()));
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, URI.create(target.longUrl()).toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .build();
  }
}
