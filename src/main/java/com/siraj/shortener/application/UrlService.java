package com.siraj.shortener.application;

import com.siraj.shortener.config.ShortenerProperties;
import com.siraj.shortener.domain.ShortUrl;
import com.siraj.shortener.domain.ShortUrlRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for short links. No HTTP types here. Transaction boundaries are per method; {@link
 * #create} deliberately runs saveAndFlush outside a class-level transaction so a unique-index
 * violation surfaces immediately and can be retried (ADR-002).
 */
@Service
public class UrlService {

  private static final Logger log = LoggerFactory.getLogger(UrlService.class);

  private final ShortUrlRepository repository;
  private final ShortCodeGenerator generator;
  private final UrlValidator urlValidator;
  private final AliasPolicy aliasPolicy;
  private final Clock clock;
  private final ShortenerProperties props;

  public UrlService(
      ShortUrlRepository repository,
      ShortCodeGenerator generator,
      UrlValidator urlValidator,
      AliasPolicy aliasPolicy,
      Clock clock,
      ShortenerProperties props) {
    this.repository = repository;
    this.generator = generator;
    this.urlValidator = urlValidator;
    this.aliasPolicy = aliasPolicy;
    this.clock = clock;
    this.props = props;
  }

  public ShortUrlView create(CreateShortUrlCommand cmd) {
    urlValidator.validate(cmd.longUrl());
    Instant now = clock.instant();
    if (cmd.expiresAt() != null && !cmd.expiresAt().isAfter(now)) {
      throw new InvalidUrlException("expiresAt must be in the future", false);
    }
    if (cmd.customAlias() != null && !cmd.customAlias().isBlank()) {
      return createWithAlias(cmd, now);
    }
    return createWithGeneratedCode(cmd, now);
  }

  private ShortUrlView createWithAlias(CreateShortUrlCommand cmd, Instant now) {
    String alias = cmd.customAlias().trim();
    aliasPolicy.validateAlias(alias);
    ShortUrl saved =
        repository.saveAndFlush(new ShortUrl(alias, cmd.longUrl(), true, now, cmd.expiresAt()));
    return ShortUrlView.from(saved, props.baseUrl());
  }

  private ShortUrlView createWithGeneratedCode(CreateShortUrlCommand cmd, Instant now) {
    int attempts = props.collisionRetries();
    for (int i = 1; i <= attempts; i++) {
      String code = generator.next();
      try {
        ShortUrl saved =
            repository.saveAndFlush(new ShortUrl(code, cmd.longUrl(), false, now, cmd.expiresAt()));
        return ShortUrlView.from(saved, props.baseUrl());
      } catch (DataIntegrityViolationException e) {
        log.warn("Short code collision on attempt {}/{} for code={}", i, attempts, code);
      }
    }
    throw new CodeGenerationExhaustedException(attempts);
  }

  /** Hot path. Returns the target (and link id for analytics) or throws 404/410 semantics. */
  @Transactional(readOnly = true)
  public ResolvedTarget resolve(String code) {
    ShortUrl url = find(code);
    if (url.isGone(clock.instant())) {
      throw new ShortUrlGoneException(code);
    }
    return new ResolvedTarget(url.getId(), url.getLongUrl());
  }

  @Transactional(readOnly = true)
  public ShortUrlView get(String code) {
    return ShortUrlView.from(find(code), props.baseUrl());
  }

  @Transactional
  public void delete(String code) {
    ShortUrl url = find(code);
    if (url.getDeletedAt() == null) {
      url.markDeleted(clock.instant());
    }
  }

  private ShortUrl find(String code) {
    return repository.findByCode(code).orElseThrow(() -> new ShortUrlNotFoundException(code));
  }
}
