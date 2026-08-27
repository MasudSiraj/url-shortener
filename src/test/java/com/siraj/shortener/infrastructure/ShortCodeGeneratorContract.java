package com.siraj.shortener.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.siraj.shortener.application.ShortCodeGenerator;
import java.util.HashSet;
import java.util.Set;
import org.testng.annotations.Test;

/** Behavioural contract for every ShortCodeGenerator (task C7). Subclasses supply the instance. */
public abstract class ShortCodeGeneratorContract {

  protected abstract ShortCodeGenerator generator();

  @Test
  public void codesUseOnlyBase62AlphabetAndFitAliasPolicy() {
    for (int i = 0; i < 500; i++) {
      assertThat(generator().next()).matches("[0-9A-Za-z]{4,16}");
    }
  }

  @Test
  public void codesAreUniqueAcrossASample() {
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 500; i++) {
      assertThat(seen.add(generator().next())).isTrue();
    }
  }
}
