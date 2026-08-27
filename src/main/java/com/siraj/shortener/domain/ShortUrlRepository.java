package com.siraj.shortener.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence port for {@link ShortUrl}. Lives in domain as the port; JPA is the adapter. */
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

  Optional<ShortUrl> findByCode(String code);
}
