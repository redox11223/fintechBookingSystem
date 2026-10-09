package com.redox.fintechBookingSystem.identity.authentication.jwt;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.user.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccessTokenIssuer {
  private final JwtEncoder jwtEncoder;
  private final IdentityProperties properties;
  private final Clock clock;

  public IssuedAccessToken issue(UUID userId, UUID sessionId, Set<Roles>roles){
    Objects.requireNonNull(userId);
    Objects.requireNonNull(sessionId);
    Objects.requireNonNull(roles);
    if(roles.isEmpty()){
      throw new IllegalArgumentException("At least one role is required");
    }
    Instant issuedAt = Instant.now(clock);
    Instant expiresAt=issuedAt.plus(properties.session().accessTokenTtl());
    List<String> rolesNames=roles.stream().map(Enum::name).sorted().toList();
    JwtClaimsSet claims=JwtClaimsSet.builder()
            .issuer(properties.session().issuer())
            .audience(List.of(properties.session().audience()))
            .subject(userId.toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim("sid",sessionId.toString())
            .claim("roles",rolesNames)
            .build();
    JwsHeader header= JwsHeader
            .with(MacAlgorithm.HS256)
            .type("JWT")
            .build();

    Jwt jwt=jwtEncoder.encode(JwtEncoderParameters.from(header,claims));
    return new IssuedAccessToken(jwt.getTokenValue(),expiresAt);
  }
}
