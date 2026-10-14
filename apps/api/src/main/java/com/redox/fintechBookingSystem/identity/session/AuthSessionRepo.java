package com.redox.fintechBookingSystem.identity.session;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepo extends JpaRepository<AuthSession, UUID> {
  // All generations of refresh tokens share this session row. Locking it serializes
  // rotation and reuse revocation, including requests carrying different generations.
  // Consumed tokens remain searchable so their reuse can revoke the session.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
SELECT session FROM AuthSession session
JOIN RefreshToken token
ON session=token.session
WHERE token.tokenHash=:tokenHash
""")
  Optional<AuthSession> findByTokenHashForUpdate(@Param("tokenHash") byte[] tokenHash);
}
