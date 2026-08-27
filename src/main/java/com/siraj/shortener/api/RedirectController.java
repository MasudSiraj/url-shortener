package com.siraj.shortener.api;

import com.siraj.shortener.application.UrlService;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hot path. The path regex keeps this mapping from swallowing /api, /actuator, /swagger-ui etc.
 * Click analytics publishing is added in the brownfield scenario (task C3), not here.
 */
@RestController
public class RedirectController {

  private final UrlService service;

  public RedirectController(UrlService service) {
    this.service = service;
  }

  @GetMapping("/{code:[A-Za-z0-9_-]{4,16}}")
  public ResponseEntity<Void> redirect(@PathVariable String code) {
    String target = service.resolve(code);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, URI.create(target).toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .build();
  }
}
