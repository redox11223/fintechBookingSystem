package com.redox.fintechBookingSystem.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TimeConfigTests {
  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(TimeConfig.class);

  @Test
  void registersUtcSystemClock() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed().hasSingleBean(Clock.class);
      assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
    });
  }
}
