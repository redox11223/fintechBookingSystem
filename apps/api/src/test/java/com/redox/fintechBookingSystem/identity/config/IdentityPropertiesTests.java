package com.redox.fintechBookingSystem.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityPropertiesTests {
  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(PropertiesConfiguration.class)
      .withPropertyValues(
          "citafin.identity.password.min-characters=15",
          "citafin.identity.password.max-characters=64",
          "citafin.identity.password.max-utf8-bytes=72",
          "citafin.identity.password.bcrypt-strength=12",
          "citafin.identity.tokens.email-verification-ttl=PT24H",
          "citafin.identity.tokens.password-reset-ttl=PT30M",
          "citafin.identity.tokens.staff-invitation-ttl=PT72H",
          "citafin.identity.tokens.mfa-challenge-ttl=PT5M",
          "citafin.identity.tokens.resend-cooldown=PT1M",
          "citafin.identity.login.max-failures=5",
          "citafin.identity.login.failure-window=PT15M",
          "citafin.identity.login.lock-duration=PT15M",
          "citafin.identity.session.access-token-ttl=PT10M",
          "citafin.identity.session.refresh-token-ttl=P30D",
          "citafin.identity.session.refresh-cookie-name=citafin_refresh",
          "citafin.identity.session.secure-cookie=false",
          "citafin.identity.mfa.code-length=6",
          "citafin.identity.mfa.time-step-seconds=30",
          "citafin.identity.mfa.allowed-time-step-drift=1",
          "citafin.identity.mfa.recovery-code-count=10",
          "citafin.identity.mail.from=no-reply@citafin.local",
          "citafin.identity.mail.frontend-base-url=http://localhost:5173",
          "citafin.identity.allowed-origins[0]=http://localhost:5173",
          "citafin.identity.secrets.jwt-hmac-key=",
          "citafin.identity.secrets.mfa-encryption-key="
      );

  @Test
  void bindsIdentityPolicyAndKeepsSecretsSeparate() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();

      IdentityProperties policy = context.getBean(IdentityProperties.class);
      IdentitySecretsProperties secrets = context.getBean(IdentitySecretsProperties.class);

      assertThat(policy.password().minCharacters()).isEqualTo(15);
      assertThat(policy.password().maxUtf8Bytes()).isEqualTo(72);
      assertThat(policy.password().bcryptStrength()).isEqualTo(12);
      assertThat(policy.tokens().emailVerificationTtl()).isEqualTo(Duration.ofHours(24));
      assertThat(policy.session().refreshTokenTtl()).isEqualTo(Duration.ofDays(30));
      assertThat(policy.allowedOrigins()).containsExactly("http://localhost:5173");
      assertThat(secrets.jwtHmacKey()).isEmpty();
      assertThat(secrets.mfaEncryptionKey()).isEmpty();
    });
  }

  @Test
  void rejectsBcryptStrengthBelowTheDocumentedMinimum() {
    contextRunner
        .withPropertyValues("citafin.identity.password.bcrypt-strength=9")
        .run(context -> assertThat(context).hasFailed());
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({IdentityProperties.class, IdentitySecretsProperties.class})
  static class PropertiesConfiguration {
  }
}
