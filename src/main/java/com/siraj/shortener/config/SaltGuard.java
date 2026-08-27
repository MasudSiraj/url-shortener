package com.siraj.shortener.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Finding S-1 (docs/08): the IP-hash salt has a development default so the app boots without
 * configuration. If that default survives into a non-dev profile the hashes become reversible by
 * anyone who reads this repository — a privacy control that silently does nothing. Refuse to run.
 */
@Component
public class SaltGuard {

  static final String DEFAULT_SALT = "dev-only-salt-change-me";
  static final List<String> ALLOWED_PROFILES = List.of("dev", "test");

  private static final Logger log = LoggerFactory.getLogger(SaltGuard.class);

  private final ShortenerProperties props;
  private final Environment environment;

  public SaltGuard(ShortenerProperties props, Environment environment) {
    this.props = props;
    this.environment = environment;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void verify() {
    check(props.analytics().ipHashSalt(), environment.getActiveProfiles());
  }

  static void check(String salt, String[] activeProfiles) {
    if (!DEFAULT_SALT.equals(salt)) {
      return;
    }
    boolean development =
        activeProfiles.length == 0
            || List.of(activeProfiles).stream().anyMatch(ALLOWED_PROFILES::contains);
    if (development) {
      log.warn(
          "Using the built-in development IP-hash salt. Set SHORTENER_IP_SALT before deploying.");
      return;
    }
    throw new IllegalStateException(
        "shortener.analytics.ip-hash-salt is still the built-in development default. "
            + "Set SHORTENER_IP_SALT to a secret value; click IP hashes are reversible without it.");
  }
}
