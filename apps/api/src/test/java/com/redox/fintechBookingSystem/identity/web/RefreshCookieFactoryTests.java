package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshCookieFactoryTests {
  @Mock private IdentityProperties properties;
  @Mock private IdentityProperties.Session sessionProperties;

  @Test
  void createsHostOnlyHttpOnlyCookieWithConfiguredSecurityAndLifetime() {
    when(properties.session()).thenReturn(sessionProperties);
    when(sessionProperties.refreshCookieName()).thenReturn("citafin_refresh");
    when(sessionProperties.secureCookie()).thenReturn(true);
    Instant now = Instant.parse("2026-10-14T15:00:00Z");
    RefreshCookieFactory factory = new RefreshCookieFactory(properties, Clock.fixed(now, ZoneOffset.UTC));

    ResponseCookie cookie = factory.create("raw-refresh-token", now.plus(Duration.ofDays(30)));

    assertThat(cookie.getName()).isEqualTo("citafin_refresh");
    assertThat(cookie.getValue()).isEqualTo("raw-refresh-token");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
    assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
    assertThat(cookie.getDomain()).isNull();
  }
  @Test
  void rotationUsesRemainingLifetimeAndElapsedDeadlineExpiresCookie() {
    Instant now = Instant.parse("2026-10-14T15:00:00Z");
    when(properties.session()).thenReturn(sessionProperties);
    when(sessionProperties.refreshCookieName()).thenReturn("citafin_refresh");
    RefreshCookieFactory factory = new RefreshCookieFactory(properties, Clock.fixed(now, ZoneOffset.UTC));
    assertThat(factory.create("token", now.plus(Duration.ofDays(10))).getMaxAge())
        .isEqualTo(Duration.ofDays(10));
    assertThat(factory.create("token", now).getMaxAge()).isEqualTo(Duration.ZERO);
    assertThat(factory.create("token", now.minusSeconds(1)).getMaxAge()).isEqualTo(Duration.ZERO);
  }
}
