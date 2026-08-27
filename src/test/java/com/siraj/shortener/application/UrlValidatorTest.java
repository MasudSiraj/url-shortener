package com.siraj.shortener.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/** Table-driven policy tests for task B2. No Spring context. */
public class UrlValidatorTest {

  private final UrlValidator validator = new UrlValidator();

  @DataProvider
  public Object[][] accepted() {
    return new Object[][] {
      {"https://example.com"},
      {"http://example.com/path?q=1#frag"},
      {"HTTPS://EXAMPLE.COM/UPPER"},
      {"https://sub.domain.example.org:8443/x"},
      {"https://xn--bcher-kva.example"}, // IDN (punycode)
      {"https://8.8.8.8/dns"}, // public literal IP is fine
      {"https://[2606:4700:4700::1111]/"}, // public IPv6
    };
  }

  @Test(dataProvider = "accepted")
  public void acceptsPublicHttpUrls(String url) {
    assertThatCode(() -> validator.validate(url)).doesNotThrowAnyException();
  }

  @DataProvider
  public Object[][] rejectedSyntax() {
    return new Object[][] {
      {null},
      {""},
      {"   "},
      {"example.com"}, // not absolute
      {"ftp://example.com/file"},
      {"file:///etc/passwd"},
      {"javascript:alert(1)"},
      {"mailto:a@b.c"},
      {"http:///nohost"},
      {"http://exa mple.com"},
      {"https://" + "a".repeat(2050)},
    };
  }

  @Test(dataProvider = "rejectedSyntax")
  public void rejectsMalformedOrDisallowedScheme(String url) {
    assertThatThrownBy(() -> validator.validate(url))
        .isInstanceOf(InvalidUrlException.class)
        .matches(e -> !((InvalidUrlException) e).isUnsafeHost(), "not flagged as unsafe host");
  }

  @DataProvider
  public Object[][] rejectedUnsafeHost() {
    return new Object[][] {
      {"http://localhost/admin"},
      {"http://LOCALHOST:8080"},
      {"http://app.localhost"},
      {"http://127.0.0.1"},
      {"http://127.1.2.3/x"},
      {"http://0.0.0.0"},
      {"http://10.0.0.1"},
      {"http://172.16.5.5"},
      {"http://192.168.1.1"},
      {"http://169.254.169.254/latest/meta-data"},
      {"http://169.254.1.1"},
      {"http://100.64.0.1"}, // CGNAT
      {"http://224.0.0.1"}, // multicast
      {"http://[::1]/"},
      {"http://[fe80::1]/"},
      {"http://metadata.google.internal/"},
      {"http://user:pass@example.com/"},
    };
  }

  @Test(dataProvider = "rejectedUnsafeHost")
  public void rejectsInternalOrCredentialedHosts(String url) {
    assertThatThrownBy(() -> validator.validate(url))
        .isInstanceOf(InvalidUrlException.class)
        .matches(e -> ((InvalidUrlException) e).isUnsafeHost(), "flagged as unsafe host");
  }
}
