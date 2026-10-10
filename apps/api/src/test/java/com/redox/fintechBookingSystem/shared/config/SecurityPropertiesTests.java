package com.redox.fintechBookingSystem.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityPropertiesTests {
  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(PropertiesConfiguration.class);

  @Test
  void bindsExactAllowedOrigins() {
    contextRunner
        .withPropertyValues(
            "citafin.security.allowed-origins[0]=http://localhost:5173",
            "citafin.security.allowed-origins[1]=https://citafin.example.com")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(SecurityProperties.class).allowedOrigins())
              .containsExactly("http://localhost:5173", "https://citafin.example.com");
        });
  }

  @Test
  void rejectsMissingAllowedOrigins() {
    contextRunner.run(context -> assertThat(context).hasFailed());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(SecurityProperties.class)
  static class PropertiesConfiguration {
  }
}
