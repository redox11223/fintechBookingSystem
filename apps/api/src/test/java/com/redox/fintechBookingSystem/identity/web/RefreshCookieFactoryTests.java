package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

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
    when(sessionProperties.refreshTokenTtl()).thenReturn(Duration.ofDays(30));
    RefreshCookieFactory factory = new RefreshCookieFactory(properties);

    ResponseCookie cookie = factory.create("raw-refresh-token");

    assertThat(cookie.getName()).isEqualTo("citafin_refresh");
    assertThat(cookie.getValue()).isEqualTo("raw-refresh-token");
    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
    assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
    assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
    assertThat(cookie.getDomain()).isNull();
  }
}
