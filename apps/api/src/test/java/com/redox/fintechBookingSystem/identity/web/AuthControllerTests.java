package com.redox.fintechBookingSystem.identity.web;

import com.redox.fintechBookingSystem.identity.authentication.AuthenticatedLogin;
import com.redox.fintechBookingSystem.identity.authentication.LoginService;
import com.redox.fintechBookingSystem.identity.authentication.exception.AccountDisabledException;
import com.redox.fintechBookingSystem.identity.authentication.exception.AuthenticationFailedException;
import com.redox.fintechBookingSystem.identity.authentication.exception.EmailNotVerifiedException;
import com.redox.fintechBookingSystem.identity.authentication.exception.MfaRequiredException;
import com.redox.fintechBookingSystem.identity.authentication.jwt.IssuedAccessToken;
import com.redox.fintechBookingSystem.identity.token.GeneratedSecureToken;
import com.redox.fintechBookingSystem.shared.config.SecurityConfig;
import com.redox.fintechBookingSystem.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
    SecurityConfig.class,
    GlobalExceptionHandler.class,
    IdentityExceptionHandler.class
})
@EnableConfigurationProperties({
    com.redox.fintechBookingSystem.identity.config.IdentityProperties.class,
    com.redox.fintechBookingSystem.shared.config.SecurityProperties.class
})
class AuthControllerTests {
  private static final String EMAIL = "client@example.com";
  private static final String PASSWORD = "a sufficiently long password";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private LoginService loginService;
  @MockitoBean private RefreshCookieFactory refreshCookieFactory;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void returnsAccessTokenInBodyAndRefreshTokenOnlyInCookie() throws Exception {
    GeneratedSecureToken refreshToken =
        new GeneratedSecureToken("raw-refresh-token", new byte[32]);
    AuthenticatedLogin authenticated = new AuthenticatedLogin(
        new IssuedAccessToken("signed-access-token", Instant.parse("2026-10-09T15:10:00Z")),
        refreshToken,
        600);
    ResponseCookie cookie = ResponseCookie.from("citafin_refresh", "raw-refresh-token")
        .httpOnly(true)
        .secure(true)
        .sameSite("Strict")
        .path("/api/v1/auth")
        .maxAge(Duration.ofDays(30))
        .build();
    when(loginService.login(EMAIL, PASSWORD)).thenReturn(authenticated);
    when(refreshCookieFactory.create("raw-refresh-token")).thenReturn(cookie);

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(validRequest()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
            containsString("citafin_refresh=raw-refresh-token"),
            containsString("Path=/api/v1/auth"),
            containsString("Max-Age=2592000"),
            containsString("Secure"),
            containsString("HttpOnly"),
            containsString("SameSite=Strict"))))
        .andExpect(jsonPath("$.accessToken").value("signed-access-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(600))
        .andExpect(jsonPath("$.refreshToken").doesNotExist());

    verify(refreshCookieFactory).create("raw-refresh-token");
  }

  @Test
  void rejectsInvalidRequestBeforeCallingLoginService() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email": "not-an-email", "password": ""}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.email").isArray())
        .andExpect(jsonPath("$.errors.password").isArray());

    verifyNoInteractions(loginService, refreshCookieFactory);
  }

  @Test
  void mapsInvalidCredentialsToGenericUnauthorizedProblem() throws Exception {
    when(loginService.login(EMAIL, PASSWORD)).thenThrow(new AuthenticationFailedException());

    performValidLogin()
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.type")
            .value("https://api.citafin.dev/problems/authentication-failed"))
        .andExpect(jsonPath("$.title").value("Authentication failed"))
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

    verifyNoInteractions(refreshCookieFactory);
  }

  @Test
  void mapsDisabledAccountToForbiddenProblem() throws Exception {
    when(loginService.login(EMAIL, PASSWORD)).thenThrow(new AccountDisabledException());

    performValidLogin()
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.type")
            .value("https://api.citafin.dev/problems/account-disabled"))
        .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
  }

  @Test
  void mapsUnverifiedEmailToForbiddenProblem() throws Exception {
    when(loginService.login(EMAIL, PASSWORD)).thenThrow(new EmailNotVerifiedException());

    performValidLogin()
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.type")
            .value("https://api.citafin.dev/problems/email-not-verified"))
        .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
  }

  @Test
  void mapsMfaRequirementToForbiddenProblem() throws Exception {
    when(loginService.login(EMAIL, PASSWORD)).thenThrow(new MfaRequiredException());

    performValidLogin()
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.type")
            .value("https://api.citafin.dev/problems/mfa-required"))
        .andExpect(jsonPath("$.title").value("MFA required"))
        .andExpect(jsonPath("$.code").value("MFA_REQUIRED"));
  }

  @Test
  void allowsCredentialedPreflightOnlyForConfiguredFrontendOrigin() throws Exception {
    mockMvc.perform(options("/api/v1/auth/login")
            .header(HttpHeaders.ORIGIN, "http://localhost:5173")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            "http://localhost:5173"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

    mockMvc.perform(options("/api/v1/auth/login")
            .header(HttpHeaders.ORIGIN, "https://attacker.example")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden());
  }

  private org.springframework.test.web.servlet.ResultActions performValidLogin() throws Exception {
    return mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(validRequest()));
  }

  private String validRequest() {
    return """
        {
          "email": "client@example.com",
          "password": "a sufficiently long password"
        }
        """;
  }
}
