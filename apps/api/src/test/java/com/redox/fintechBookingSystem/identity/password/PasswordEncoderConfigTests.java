package com.redox.fintechBookingSystem.identity.password;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderConfigTests {
  private static final int TEST_BCRYPT_STRENGTH = 10;

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withBean(IdentityProperties.class, PasswordEncoderConfigTests::identityProperties)
      .withUserConfiguration(PasswordEncoderConfig.class);

  @Test
  void registersSingleBcryptPasswordEncoder() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed().hasSingleBean(PasswordEncoder.class);
      assertThat(context.getBean(PasswordEncoder.class))
          .isInstanceOf(BCryptPasswordEncoder.class);
    });
  }

  @Test
  void encodesAndMatchesPasswordUsingConfiguredStrength() {
    contextRunner.run(context -> {
      PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);
      String password = "correct horse battery staple";

      String firstHash = passwordEncoder.encode(password);
      String secondHash = passwordEncoder.encode(password);

      assertThat(firstHash)
          .isNotEqualTo(password)
          .isNotEqualTo(secondHash)
          .matches("^\\$2[aby]\\$10\\$.*");
      assertThat(passwordEncoder.matches(password, firstHash)).isTrue();
      assertThat(passwordEncoder.matches(password, secondHash)).isTrue();
      assertThat(passwordEncoder.matches("different password", firstHash)).isFalse();
    });
  }

  private static IdentityProperties identityProperties() {
    var password = new IdentityProperties.Password(15, 64, 72, TEST_BCRYPT_STRENGTH);
    return new IdentityProperties(password, null, null, null, null, null, List.of());
  }
}
