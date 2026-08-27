package com.siraj.shortener.application;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Validates long URLs before storage. Two concerns:
 *
 * <ul>
 *   <li>Syntax/policy: absolute, http(s) only, bounded length.
 *   <li>Safety (SSRF / internal-network redirect): rejects loopback, private, link-local,
 *       unspecified, multicast and cloud-metadata targets when the host is an IP literal or a
 *       well-known local name.
 * </ul>
 *
 * <p>Decision (engineer to confirm, see docs/04): hostnames are NOT DNS-resolved here. Resolving
 * would make validation non-deterministic and slow; a DNS-rebinding attacker could bypass it
 * anyway. The redirect is a 302 to the client's own browser, not a server-side fetch, so the SSRF
 * exposure is limited to users being sent to internal addresses — which literal-IP blocking covers.
 */
@Component
public class UrlValidator {

  static final int MAX_LENGTH = 2048;
  private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
  private static final Set<String> BLOCKED_HOSTNAMES =
      Set.of("localhost", "localhost.localdomain", "metadata.google.internal");
  private static final String AWS_METADATA_IP = "169.254.169.254";

  public void validate(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new InvalidUrlException("URL must not be blank", false);
    }
    if (raw.length() > MAX_LENGTH) {
      throw new InvalidUrlException("URL exceeds " + MAX_LENGTH + " characters", false);
    }
    URI uri = parse(raw);
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    if (!ALLOWED_SCHEMES.contains(scheme)) {
      throw new InvalidUrlException("Only http and https URLs are accepted", false);
    }
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new InvalidUrlException("URL must contain a host", false);
    }
    if (uri.getRawUserInfo() != null) {
      throw new InvalidUrlException("Credentials in URL are not accepted", true);
    }
    rejectUnsafeHost(host.toLowerCase(Locale.ROOT));
  }

  private URI parse(String raw) {
    try {
      URI uri = new URI(raw.trim());
      if (!uri.isAbsolute()) {
        throw new InvalidUrlException("URL must be absolute", false);
      }
      return uri;
    } catch (URISyntaxException e) {
      throw new InvalidUrlException("URL is not well-formed", false);
    }
  }

  private void rejectUnsafeHost(String host) {
    if (BLOCKED_HOSTNAMES.contains(host) || host.endsWith(".localhost")) {
      throw new InvalidUrlException("Host is not permitted", true);
    }
    String literal = stripBrackets(host);
    if (AWS_METADATA_IP.equals(literal)) {
      throw new InvalidUrlException("Host is not permitted", true);
    }
    if (looksLikeIpLiteral(literal) && isInternalAddress(toAddress(literal))) {
      throw new InvalidUrlException("Host is not permitted", true);
    }
  }

  /** Address ranges that must never be a redirect target. Extracted to keep complexity <= 10. */
  private static boolean isInternalAddress(InetAddress addr) {
    return addr.isLoopbackAddress()
        || addr.isSiteLocalAddress()
        || addr.isLinkLocalAddress()
        || addr.isAnyLocalAddress()
        || addr.isMulticastAddress()
        || isCgnat(addr);
  }

  private static String stripBrackets(String host) {
    return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
  }

  private static boolean looksLikeIpLiteral(String host) {
    return host.chars().allMatch(c -> Character.isDigit(c) || c == '.') || host.contains(":");
  }

  private static InetAddress toAddress(String literal) {
    try {
      // getByName on a pure literal does not hit DNS.
      return InetAddress.getByName(literal);
    } catch (UnknownHostException e) {
      throw new InvalidUrlException("Host is not permitted", true);
    }
  }

  /** 100.64.0.0/10 (RFC 6598) is not covered by isSiteLocalAddress. */
  private static boolean isCgnat(InetAddress addr) {
    byte[] b = addr.getAddress();
    return b.length == 4 && (b[0] & 0xFF) == 100 && ((b[1] & 0xFF) & 0xC0) == 0x40;
  }
}
