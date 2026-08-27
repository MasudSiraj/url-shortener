package com.siraj.shortener.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.siraj.shortener.config.ShortenerProperties;
import com.siraj.shortener.domain.ShortUrl;
import com.siraj.shortener.domain.ShortUrlRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for task B4/B5 with a fixed clock and mocked collaborators. */
public class UrlServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
  private static final String BASE = "http://short.test";

  private ShortUrlRepository repository;
  private ShortCodeGenerator generator;
  private UrlService service;

  @BeforeMethod
  public void setUp() {
    repository = mock(ShortUrlRepository.class);
    generator = mock(ShortCodeGenerator.class);
    ShortenerProperties props =
        new ShortenerProperties(
            BASE,
            "random",
            7,
            3,
            new ShortenerProperties.Cache(100, Duration.ofMinutes(1)),
            new ShortenerProperties.Analytics("salt"));
    service =
        new UrlService(
            repository,
            generator,
            new UrlValidator(),
            new AliasPolicy(),
            Clock.fixed(NOW, ZoneOffset.UTC),
            props);
    // Default: echo back whatever is saved.
    when(repository.saveAndFlush(any(ShortUrl.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  public void createsWithGeneratedCode() {
    when(generator.next()).thenReturn("abc1234");

    ShortUrlView view =
        service.create(new CreateShortUrlCommand("https://example.com/x", null, null));

    assertThat(view.code()).isEqualTo("abc1234");
    assertThat(view.shortUrl()).isEqualTo(BASE + "/abc1234");
    assertThat(view.customAlias()).isFalse();
    assertThat(view.createdAt()).isEqualTo(NOW);
    verify(repository, times(1)).saveAndFlush(any());
  }

  @Test
  public void createsWithCustomAlias() {
    ShortUrlView view =
        service.create(new CreateShortUrlCommand("https://example.com", " my-link ", null));

    assertThat(view.code()).isEqualTo("my-link");
    assertThat(view.customAlias()).isTrue();
    verify(generator, never()).next();
  }

  @Test
  public void retriesOnCollisionThenSucceeds() {
    when(generator.next()).thenReturn("dup0001", "dup0002", "fresh01");
    when(repository.saveAndFlush(any(ShortUrl.class)))
        .thenThrow(new DataIntegrityViolationException("dup"))
        .thenThrow(new DataIntegrityViolationException("dup"))
        .thenAnswer(inv -> inv.getArgument(0));

    ShortUrlView view =
        service.create(new CreateShortUrlCommand("https://example.com", null, null));

    assertThat(view.code()).isEqualTo("fresh01");
    verify(repository, times(3)).saveAndFlush(any());
  }

  @Test
  public void failsAfterExhaustingRetries() {
    when(generator.next()).thenReturn("x");
    when(repository.saveAndFlush(any(ShortUrl.class)))
        .thenThrow(new DataIntegrityViolationException("dup"));

    assertThatThrownBy(
            () -> service.create(new CreateShortUrlCommand("https://example.com", null, null)))
        .isInstanceOf(CodeGenerationExhaustedException.class);
    verify(repository, times(3)).saveAndFlush(any());
  }

  @Test
  public void rejectsExpiryInThePast() {
    assertThatThrownBy(
            () ->
                service.create(
                    new CreateShortUrlCommand(
                        "https://example.com", null, NOW.minus(Duration.ofSeconds(1)))))
        .isInstanceOf(InvalidUrlException.class);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  public void rejectsUnsafeUrlBeforeTouchingRepository() {
    assertThatThrownBy(
            () -> service.create(new CreateShortUrlCommand("http://10.0.0.1/", null, null)))
        .isInstanceOf(InvalidUrlException.class);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  public void resolvesLiveLink() throws Exception {
    ShortUrl live = new ShortUrl("live001", "https://example.com/t", false, NOW, null);
    var id = ShortUrl.class.getDeclaredField("id");
    id.setAccessible(true);
    id.set(live, 99L);
    when(repository.findByCode("live001")).thenReturn(Optional.of(live));

    ResolvedTarget target = service.resolve("live001");

    assertThat(target.longUrl()).isEqualTo("https://example.com/t");
    assertThat(target.shortUrlId()).isEqualTo(99L);
  }

  @Test
  public void resolveThrowsNotFoundForUnknownCode() {
    when(repository.findByCode("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resolve("nope")).isInstanceOf(ShortUrlNotFoundException.class);
  }

  @Test
  public void resolveThrowsGoneWhenExpired() {
    when(repository.findByCode("old0001"))
        .thenReturn(
            Optional.of(
                new ShortUrl("old0001", "https://example.com", false, NOW.minusSeconds(100), NOW)));

    assertThatThrownBy(() -> service.resolve("old0001")).isInstanceOf(ShortUrlGoneException.class);
  }

  @Test
  public void resolveThrowsGoneWhenDeleted() {
    ShortUrl deleted = new ShortUrl("del0001", "https://example.com", false, NOW, null);
    deleted.markDeleted(NOW);
    when(repository.findByCode("del0001")).thenReturn(Optional.of(deleted));

    assertThatThrownBy(() -> service.resolve("del0001")).isInstanceOf(ShortUrlGoneException.class);
  }

  @Test
  public void deleteIsIdempotent() {
    ShortUrl url = new ShortUrl("del0002", "https://example.com", false, NOW, null);
    when(repository.findByCode("del0002")).thenReturn(Optional.of(url));

    service.delete("del0002");
    Instant first = url.getDeletedAt();
    service.delete("del0002");

    assertThat(first).isEqualTo(NOW);
    assertThat(url.getDeletedAt()).isEqualTo(first);
  }

  @Test
  public void getReturnsMetadataIncludingDeletedLinks() {
    ShortUrl url = new ShortUrl("meta001", "https://example.com", true, NOW, null);
    url.markDeleted(NOW);
    when(repository.findByCode("meta001")).thenReturn(Optional.of(url));

    ShortUrlView view = service.get("meta001");

    assertThat(view.deletedAt()).isEqualTo(NOW);
    assertThat(view.customAlias()).isTrue();
  }
}
