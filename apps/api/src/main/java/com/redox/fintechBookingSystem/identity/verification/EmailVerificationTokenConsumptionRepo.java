package com.redox.fintechBookingSystem.identity.verification;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class EmailVerificationTokenConsumptionRepo {
  private final JdbcClient jdbcClient;

  Optional<UUID> consumeIfValid(byte[] tokenHash, Instant now){
    String sql="""
        UPDATE email_verification_tokens
        SET consumed_at = :now,
            updated_at = :now
        WHERE token_hash = :token_hash
        AND consumed_at IS NULL
        AND revoked_at IS NULL
        AND expires_at > :now
        RETURNING user_id
      """;
    return jdbcClient.sql(sql)
            .param("token_hash",tokenHash)
            .param("now",now)
            .query(UUID.class)
            .optional();
  }
}
