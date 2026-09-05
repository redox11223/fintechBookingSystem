package com.redox.fintechBookingSystem.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Secret material is bound separately so it cannot accidentally acquire a committed default.
 * Each feature validates the corresponding key when its cryptographic bean is activated.
 */
@ConfigurationProperties("citafin.identity.secrets")
public record IdentitySecretsProperties(
    String jwtHmacKey,
    String mfaEncryptionKey
) {
}
