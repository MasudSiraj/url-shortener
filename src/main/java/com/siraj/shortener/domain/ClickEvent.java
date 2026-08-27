package com.siraj.shortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

/** One redirect. Stores a salted hash of the client IP, never the IP itself (ADR-004). */
@Entity
@Table(name = "click_event")
public class ClickEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "click_event_seq")
  @SequenceGenerator(name = "click_event_seq", sequenceName = "click_event_seq", allocationSize = 1)
  private Long id;

  @Column(name = "short_url_id", nullable = false)
  private Long shortUrlId;

  @Column(name = "clicked_at", nullable = false)
  private Instant clickedAt;

  @Column(name = "referrer", length = 2048)
  private String referrer;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @Column(name = "ip_hash", nullable = false, length = 64)
  private String ipHash;

  protected ClickEvent() {
    // JPA
  }

  public ClickEvent(
      Long shortUrlId, Instant clickedAt, String referrer, String userAgent, String ipHash) {
    this.shortUrlId = shortUrlId;
    this.clickedAt = clickedAt;
    this.referrer = referrer;
    this.userAgent = userAgent;
    this.ipHash = ipHash;
  }

  public Long getId() {
    return id;
  }

  public Long getShortUrlId() {
    return shortUrlId;
  }

  public Instant getClickedAt() {
    return clickedAt;
  }

  public String getReferrer() {
    return referrer;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getIpHash() {
    return ipHash;
  }
}
