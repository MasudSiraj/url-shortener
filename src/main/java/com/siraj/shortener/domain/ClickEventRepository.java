package com.siraj.shortener.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for click events. Aggregation is done in the service at prototype scale. */
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

  long countByShortUrlId(Long shortUrlId);

  List<ClickEvent> findByShortUrlIdAndClickedAtGreaterThanEqual(Long shortUrlId, Instant since);
}
