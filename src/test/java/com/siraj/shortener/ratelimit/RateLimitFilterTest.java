package com.siraj.shortener.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.Test;

public class RateLimitFilterTest {

  private static MockHttpServletRequest req(String method, String uri) {
    MockHttpServletRequest r = new MockHttpServletRequest(method, uri);
    r.setRemoteAddr("9.9.9.9");
    return r;
  }

  @Test
  public void actuatorAndDocsAreExempt() {
    RateLimitFilter filter =
        new RateLimitFilter(mock(RateLimiter.class), new SimpleMeterRegistry(), new ObjectMapper());
    assertThat(filter.shouldNotFilter(req("GET", "/actuator/health"))).isTrue();
    assertThat(filter.shouldNotFilter(req("GET", "/v3/api-docs"))).isTrue();
    assertThat(filter.shouldNotFilter(req("GET", "/abc1234"))).isFalse();
    assertThat(filter.shouldNotFilter(req("POST", "/api/v1/urls"))).isFalse();
  }

  @Test
  public void mapsRequestsToTheRightBucket() {
    assertThat(RateLimitFilter.bucketFor(req("POST", "/api/v1/urls")))
        .isEqualTo(RateLimiter.Bucket.CREATE);
    assertThat(RateLimitFilter.bucketFor(req("GET", "/abc1234")))
        .isEqualTo(RateLimiter.Bucket.READ);
    assertThat(RateLimitFilter.bucketFor(req("GET", "/api/v1/urls/abc1234/stats")))
        .isEqualTo(RateLimiter.Bucket.READ);
    assertThat(RateLimitFilter.bucketFor(req("DELETE", "/api/v1/urls/abc1234")))
        .isEqualTo(RateLimiter.Bucket.READ);
  }

  @Test
  public void failsOpenWhenLimiterThrows() throws Exception {
    RateLimiter broken = mock(RateLimiter.class);
    when(broken.check(anyString(), any())).thenThrow(new IllegalStateException("boom"));
    FilterChain chain = mock(FilterChain.class);
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RateLimitFilter(broken, new SimpleMeterRegistry(), new ObjectMapper())
        .doFilter(req("GET", "/abc1234"), response, chain);

    verify(chain).doFilter(any(), any());
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  public void rejectionWritesProblemJsonWithRetryAfterAndMetric() throws Exception {
    RateLimiter denying = mock(RateLimiter.class);
    when(denying.check(anyString(), any())).thenReturn(RateLimiter.Decision.deny(42));
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RateLimitFilter(denying, registry, new ObjectMapper())
        .doFilter(req("POST", "/api/v1/urls"), response, mock(FilterChain.class));

    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getHeader("Retry-After")).isEqualTo("42");
    assertThat(response.getContentAsString()).contains("rate-limited").doesNotContain("Exception");
    assertThat(registry.counter(RateLimitFilter.METRIC, "bucket", "CREATE").count()).isEqualTo(1.0);
  }
}
