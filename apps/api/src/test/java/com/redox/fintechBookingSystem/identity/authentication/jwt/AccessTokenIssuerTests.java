package com.redox.fintechBookingSystem.identity.authentication.jwt;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.user.Roles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenIssuerTests {
  private static final Instant NOW = Instant.parse("2026-10-09T15:00:00Z");
  private static final UUID USER_ID = UUID.fromString("01165b85-38e2-4ff6-bde6-13884c426b85");
  private static final UUID SESSION_ID = UUID.fromString("a0fb490e-96c2-4de2-a895-31b9101f246e");

  @Mock private JwtEncoder jwtEncoder;
  @Mock private IdentityProperties properties;
  @Mock private IdentityProperties.Session sessionProperties;

  private AccessTokenIssuer issuer;

  @BeforeEach
  void setUp() {
    issuer = new AccessTokenIssuer(
        jwtEncoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void signsShortLivedTokenWithIdentitySessionAndSortedRoles() {
    when(properties.session()).thenReturn(sessionProperties);
    when(sessionProperties.accessTokenTtl()).thenReturn(Duration.ofMinutes(10));
    when(sessionProperties.issuer()).thenReturn("citafin-api");
    when(sessionProperties.audience()).thenReturn("citafin-api");
    Jwt encoded = Jwt.withTokenValue("signed-access-token")
        .header("alg", "HS256")
        .subject(USER_ID.toString())
        .issuedAt(NOW)
        .expiresAt(NOW.plus(Duration.ofMinutes(10)))
        .build();
    when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encoded);

    IssuedAccessToken result = issuer.issue(
        USER_ID, SESSION_ID, Set.of(Roles.CLIENT, Roles.ADMIN));

    assertThat(result.value()).isEqualTo("signed-access-token");
    assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));

    ArgumentCaptor<JwtEncoderParameters> parameters =
        ArgumentCaptor.forClass(JwtEncoderParameters.class);
    verify(jwtEncoder).encode(parameters.capture());
    assertThat(parameters.getValue().getJwsHeader().getAlgorithm().getName()).isEqualTo("HS256");
    assertThat(parameters.getValue().getClaims().getClaimAsString("iss"))
        .isEqualTo("citafin-api");
    assertThat(parameters.getValue().getClaims().getAudience()).containsExactly("citafin-api");
    assertThat(parameters.getValue().getClaims().getSubject()).isEqualTo(USER_ID.toString());
    assertThat(parameters.getValue().getClaims().getIssuedAt()).isEqualTo(NOW);
    assertThat(parameters.getValue().getClaims().getExpiresAt())
        .isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    assertThat(parameters.getValue().getClaims().getId()).isNotBlank();
    assertThat(parameters.getValue().getClaims().getClaimAsString("sid"))
        .isEqualTo(SESSION_ID.toString());
    assertThat(parameters.getValue().getClaims().getClaimAsStringList("roles"))
        .isEqualTo(List.of("ADMIN", "CLIENT"));
  }
}
