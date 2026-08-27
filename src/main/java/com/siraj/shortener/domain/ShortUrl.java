package com.siraj.shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

/** Aggregate root for a shortened link. Maps to V1__create_short_url.sql. No Lombok by design. */
@Entity
@Table(name = "short_url")
public class ShortUrl {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shorturl_seq")
  @SequenceGenerator(name = "shorturl_seq", sequenceName = "shorturl_seq", allocationSize = 1)
  private Long id;

  @Column(name = "code", nullable = false, length = 16)
  private String code;

  @Column(name = "long_url", nullable = false, length = 2048)
  private String longUrl;

  @Column(name = "custom_alias", nullable = false)
  private boolean customAlias;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected ShortUrl() {
    // JPA
  }

  public ShortUrl(
      String code, String longUrl, boolean customAlias, Instant createdAt, Instant expiresAt) {
    this.code = code;
    this.longUrl = longUrl;
    this.customAlias = customAlias;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  /** Domain rule: a link is unusable if soft-deleted or past its expiry. */
  public boolean isGone(Instant now) {
    return deletedAt != null || (expiresAt != null && !expiresAt.isAfter(now));
  }

  public void markDeleted(Instant now) {
    this.deletedAt = now;
  }

  public Long getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public String getLongUrl() {
    return longUrl;
  }

  public boolean isCustomAlias() {
    return customAlias;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
