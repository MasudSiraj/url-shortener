package com.siraj.shortener.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AliasPolicyTest {

  private final AliasPolicy policy = new AliasPolicy();

  @DataProvider
  public Object[][] valid() {
    return new Object[][] {{"abcd"}, {"my-link"}, {"my_link_2026"}, {"A1b2C3d4E5f6G7h8"}};
  }

  @Test(dataProvider = "valid")
  public void acceptsWellFormedAliases(String alias) {
    assertThatCode(() -> policy.validateAlias(alias)).doesNotThrowAnyException();
  }

  @DataProvider
  public Object[][] invalid() {
    return new Object[][] {
      {"abc", "too short"},
      {"a".repeat(17), "too long"},
      {"has space", "space"},
      {"has/slash", "slash"},
      {"émoji", "non-ascii"},
      {"api", "reserved (and short)"},
      {"actuator", "reserved"},
      {"ACTUATOR", "reserved, case-insensitive"},
      {"swagger-ui", "reserved"},
      {"h2-console", "reserved"},
    };
  }

  @Test(dataProvider = "invalid")
  public void rejectsBadAliases(String alias, String why) {
    assertThatThrownBy(() -> policy.validateAlias(alias))
        .as(why)
        .isInstanceOf(InvalidAliasException.class);
  }
}
