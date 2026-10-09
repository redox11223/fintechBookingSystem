package com.redox.fintechBookingSystem.identity.authentication.jwt;

import com.redox.fintechBookingSystem.identity.config.IdentityProperties;
import com.redox.fintechBookingSystem.identity.config.IdentitySecretsProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class JwtConfig {
  /**
   * Decodes a Base64 JWT HMAC key from configuration, validates its minimum
   * length requirement (at least 32 bytes/256 bits), and wraps the raw bytes
   * into a SecretKeySpec.
   * NOTE ON SecretKeySpec:
   * This class performs no cryptographic calculations. It acts as a passive
   * container that binds the raw bytes with the specific algorithm name
   * ("HmacSHA256"). This provides the JCA/JWT library with the necessary
   * context to safely execute the actual HMAC signature operations later.
   */
  @Bean("jwtSecretKey")
  public SecretKey jwtSecretKey(IdentitySecretsProperties secrets){
    String configuredSecret=secrets.jwtHmacKey();
    Assert.hasText(configuredSecret,"JWT HMAC key is required");
    byte[] jwtBytes;
    try{
      jwtBytes=Base64.getDecoder().decode(configuredSecret);
    }catch (IllegalArgumentException e){
      throw new IllegalStateException("JWT HMAC key must be valid Base64");
    }
    if(jwtBytes.length<32){
      throw new IllegalStateException("JWT HMAC key must contain at least 32 bytes");
    }
    return new SecretKeySpec(jwtBytes, "HmacSHA256");
  }

  /**
   * Configures the Spring Security bean responsible for signing and generating JWTs.
   * It uses Nimbus (Spring's default OAuth2/JWT engine) to create a token encoder,
   * binding the provided SecretKey with the HMAC-SHA256 (HS256) signature algorithm.
   */
  @Bean
  public JwtEncoder jwtEncoder(@Qualifier("jwtSecretKey") SecretKey secretKey){
    return NimbusJwtEncoder
            .withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS256)
            .build();
  }

  @Bean
  public JwtDecoder jwtDecoder(@Qualifier("jwtSecretKey") SecretKey secretKey, IdentityProperties properties){
    // CRYPTO VALIDATION: Nimbus acts as Spring's underlying crypto engine.
    // It verifies the cryptographic signature using the HmacSHA256 SecretKeySpec wrapper.
    // If the token was tampered with, execution stops right here (401 Unauthorized).
    NimbusJwtDecoder decoder=NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    // StandardValidator checks timestamps (exp, iat) and matching issuer (iss).
    OAuth2TokenValidator<Jwt> standardValidator=JwtValidators.createDefaultWithIssuer(properties.session().issuer());
    // AudienceValidator ensures the token was specifically intended for this API client (aud).
    OAuth2TokenValidator<Jwt> audienceValidator=new JwtClaimValidator<List<String>>
            ("aud", audiences->!audiences.isEmpty() &&
                    audiences.contains(properties.session().audience()));
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(standardValidator,audienceValidator));
    return decoder;
  }
}
