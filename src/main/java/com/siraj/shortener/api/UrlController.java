package com.siraj.shortener.api;

import com.siraj.shortener.application.CreateShortUrlCommand;
import com.siraj.shortener.application.ShortUrlView;
import com.siraj.shortener.application.UrlService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Management API. No business logic — maps HTTP to the service and back. */
@RestController
@RequestMapping(path = "/api/v1/urls", produces = "application/json")
public class UrlController {

  private final UrlService service;

  public UrlController(UrlService service) {
    this.service = service;
  }

  @PostMapping(consumes = "application/json")
  public ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest req) {
    ShortUrlView view =
        service.create(
            new CreateShortUrlCommand(req.longUrl(), req.customAlias(), req.expiresAt()));
    return ResponseEntity.created(URI.create(view.shortUrl())).body(ShortUrlResponse.from(view));
  }

  @GetMapping("/{code}")
  public ShortUrlResponse get(@PathVariable String code) {
    return ShortUrlResponse.from(service.get(code));
  }

  @DeleteMapping("/{code}")
  public ResponseEntity<Void> delete(@PathVariable String code) {
    service.delete(code);
    return ResponseEntity.noContent().build();
  }
}
