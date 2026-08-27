package com.siraj.shortener.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.Test;

/** Finding S-1: the development salt must never survive into a production profile. */
public class SaltGuardTest {

  @Test
  public void defaultSaltIsRejectedInAProductionProfile() {
    assertThatThrownBy(() -> SaltGuard.check(SaltGuard.DEFAULT_SALT, new String[] {"docker"}))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SHORTENER_IP_SALT");
  }

  @Test
  public void defaultSaltIsToleratedInDevAndTest() {
    assertThatCode(() -> SaltGuard.check(SaltGuard.DEFAULT_SALT, new String[] {"dev"}))
        .doesNotThrowAnyException();
    assertThatCode(() -> SaltGuard.check(SaltGuard.DEFAULT_SALT, new String[] {"test"}))
        .doesNotThrowAnyException();
    assertThatCode(() -> SaltGuard.check(SaltGuard.DEFAULT_SALT, new String[] {}))
        .doesNotThrowAnyException();
  }

  @Test
  public void configuredSaltIsAlwaysAccepted() {
    assertThatCode(() -> SaltGuard.check("a-real-secret", new String[] {"docker"}))
        .doesNotThrowAnyException();
  }
}
