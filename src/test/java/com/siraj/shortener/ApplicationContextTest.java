package com.siraj.shortener;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/** Smoke test: context boots and Flyway V1 applies on H2 (task A2/A4 acceptance). */
@SpringBootTest
@ActiveProfiles("test")
public class ApplicationContextTest extends AbstractTestNGSpringContextTests {

  @Autowired private ApplicationContext context;

  @Test
  public void contextLoadsAndMigrationsApply() {
    assertThat(context).isNotNull();
    assertThat(context.containsBean("flyway")).isTrue();
  }
}
