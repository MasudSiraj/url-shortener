package com.siraj.shortener.application;

import com.siraj.shortener.config.ShortenerProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Salted SHA-256 of a client IP. The raw IP is never persisted or logged (D-4). */
@Component
public class IpHasher {

  private final String salt;

  public IpHasher(ShortenerProperties props) {
    this.salt = props.analytics().ipHashSalt();
  }

  public String hash(String ip) {
    String input = salt + "|" + (ip == null ? "" : ip);
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
