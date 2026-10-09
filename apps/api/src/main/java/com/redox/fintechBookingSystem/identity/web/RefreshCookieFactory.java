package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class RefreshCookieFactory {
  private final static String REFRESH_COOKIE_PATH="/api/v1/auth";
  private final IdentityProperties properties;

  public ResponseCookie create(String rawRefreshToken){
    Assert.hasText(rawRefreshToken,"Refresh token is required");
    var session=properties.session();
    // HttpOnly prevents JavaScript from reading the refresh token through document.cookie.
    // Omitting Domain makes this a host-only cookie, so it is returned only to the API host.
    // The SPA and API are different origins but same-site sibling subdomains; SameSite=Strict
    // permits that deployment while CORS and Origin/CSRF checks provide the remaining controls.
    return ResponseCookie
            .from(session.refreshCookieName(),rawRefreshToken)
            .httpOnly(true)
            .secure(session.secureCookie())
            .sameSite("Strict")
            .path(REFRESH_COOKIE_PATH)
            .maxAge(session.refreshTokenTtl())
            .build();
  }
}
