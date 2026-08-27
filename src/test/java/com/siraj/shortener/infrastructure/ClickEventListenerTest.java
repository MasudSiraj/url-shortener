package com.siraj.shortener.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.siraj.shortener.application.ClickRecorded;
import com.siraj.shortener.application.IpHasher;
import com.siraj.shortener.domain.ClickEvent;
import com.siraj.shortener.domain.ClickEventRepository;
import java.time.Instant;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

public class ClickEventListenerTest {

  private static final Instant AT = Instant.parse("2026-08-27T12:00:00Z");

  @Test
  public void persistsHashedIpNotRawIp() {
    ClickEventRepository repo = mock(ClickEventRepository.class);
    IpHasher hasher = mock(IpHasher.class);
    when(hasher.hash("203.0.113.9")).thenReturn("h".repeat(64));

    new ClickEventListener(repo, hasher)
        .on(new ClickRecorded(7L, AT, "https://ref.example", "UA/1", "203.0.113.9"));

    ArgumentCaptor<ClickEvent> saved = ArgumentCaptor.forClass(ClickEvent.class);
    verify(repo).save(saved.capture());
    assertThat(saved.getValue().getIpHash()).isEqualTo("h".repeat(64));
    assertThat(saved.getValue().getReferrer()).isEqualTo("https://ref.example");
    assertThat(saved.getValue().getShortUrlId()).isEqualTo(7L);
  }

  @Test
  public void truncatesOverlongHeaders() {
    ClickEventRepository repo = mock(ClickEventRepository.class);
    IpHasher hasher = mock(IpHasher.class);
    when(hasher.hash(any())).thenReturn("x");

    new ClickEventListener(repo, hasher)
        .on(new ClickRecorded(1L, AT, "r".repeat(5000), "u".repeat(5000), "1.1.1.1"));

    ArgumentCaptor<ClickEvent> saved = ArgumentCaptor.forClass(ClickEvent.class);
    verify(repo).save(saved.capture());
    assertThat(saved.getValue().getReferrer()).hasSize(2048);
    assertThat(saved.getValue().getUserAgent()).hasSize(512);
  }

  @Test
  public void swallowsPersistenceFailures() {
    ClickEventRepository repo = mock(ClickEventRepository.class);
    IpHasher hasher = mock(IpHasher.class);
    when(hasher.hash(any())).thenReturn("x");
    when(repo.save(any())).thenThrow(new RuntimeException("db down"));

    assertThatCode(
            () ->
                new ClickEventListener(repo, hasher)
                    .on(new ClickRecorded(1L, AT, null, null, "1.1.1.1")))
        .doesNotThrowAnyException();
  }
}
