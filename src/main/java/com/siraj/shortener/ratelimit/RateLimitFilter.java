package com.siraj.shortener.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces limits ahead of controller dispatch (docs/06 Q3), so rejected requests never reach the
 * database. Actuator is exempt (Q2). Client identity is {@code getRemoteAddr()} — {@code
 * X-Forwarded-For} is deliberately NOT trusted (Q5). Fails open (Q12).
 */
@Component
@Order(1)
@ConditionalOnProperty(prefix = "shortener.rate-limit", name = "enabled", havingValue = "true")
public class RateLimitFilter extends OncePerRequestFilter {

  static final String METRIC = "ratelimit.rejected";
  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

  private final RateLimiter limiter;
  private final MeterRegistry registry;
  private final ObjectMapper mapper;

  public RateLimitFilter(RateLimiter limiter, MeterRegistry registry, ObjectMapper mapper) {
    this.limiter = limiter;
    this.registry = registry;
    this.mapper = mapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/actuator")
        || path.startsWith("/swagger-ui")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/h2-console");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    RateLimiter.Decision decision;
    try {
      decision = limiter.check(request.getRemoteAddr(), bucketFor(request));
    } catch (RuntimeException e) {
      // Fail open: a broken limiter must not become an outage (docs/06 Q12).
      log.error("Rate limiter failed; allowing request", e);
      chain.doFilter(request, response);
      return;
    }

    if (decision.allowed()) {
      chain.doFilter(request, response);
      return;
    }
    reject(request, response, decision.retryAfterSeconds());
  }

  static RateLimiter.Bucket bucketFor(HttpServletRequest request) {
    boolean create =
        "POST".equalsIgnoreCase(request.getMethod())
            && request.getRequestURI().startsWith("/api/v1/urls");
    return create ? RateLimiter.Bucket.CREATE : RateLimiter.Bucket.READ;
  }

  private void reject(HttpServletRequest request, HttpServletResponse response, long retryAfter)
      throws IOException {
    registry.counter(METRIC, "bucket", bucketFor(request).name()).increment();
    log.warn("Rate limit exceeded for {} on {}", request.getRemoteAddr(), request.getRequestURI());

    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded; retry in " + retryAfter + "s");
    pd.setType(URI.create("https://siraj.dev/problems/rate-limited"));
    pd.setTitle("Too many requests");
    pd.setInstance(URI.create(request.getRequestURI()));

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
    mapper.writeValue(response.getOutputStream(), pd);
  }
}
