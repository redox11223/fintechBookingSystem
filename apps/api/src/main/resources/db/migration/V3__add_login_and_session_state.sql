--check constraints will only allow to modify or insert if the condition is true
ALTER TABLE users
    ADD COLUMN failed_login_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN failure_window_started_at TIMESTAMPTZ,
    ADD COLUMN locked_until TIMESTAMPTZ,
    ADD CONSTRAINT chk_users_failed_login_count_non_negative CHECK (failed_login_count >= 0),
    ADD CONSTRAINT chk_users_failure_window_state CHECK (
    failure_window_started_at IS NULL AND failed_login_count = 0
    OR failure_window_started_at IS NOT NULL AND failed_login_count > 0),
    ADD CONSTRAINT chk_users_locked_state CHECK (
    locked_until IS NULL OR failed_login_count > 0),
    ADD CONSTRAINT chk_users_locked_until_after_failure_window_started CHECK (
          locked_until IS NULL
      OR (
          failure_window_started_at IS NOT NULL
          AND locked_until > failure_window_started_at
      )
    );

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    absolute_expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    revoked_at TIMESTAMPTZ, -- Indicates that the session was revoked.
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_auth_sessions_expiry CHECK (absolute_expires_at > created_at),
    CONSTRAINT chk_auth_sessions_revoked_at CHECK (revoked_at IS NULL OR revoked_at >= created_at),
    CONSTRAINT chk_auth_sessions_updated_at CHECK (updated_at >= created_at)
);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions(user_id);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    token_hash BYTEA NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    consumed_at TIMESTAMPTZ, -- Indicates that the token was used to refresh the session.
    revoked_at TIMESTAMPTZ, -- Indicates that the token was revoked without having being consumed.
    CONSTRAINT fk_refresh_tokens_session FOREIGN KEY (session_id) REFERENCES auth_sessions(id) ON DELETE CASCADE,
    CONSTRAINT chk_refresh_tokens_hash_length CHECK (octet_length(token_hash) = 32),
    CONSTRAINT chk_refresh_tokens_consumed_revoked CHECK (consumed_at IS NULL OR revoked_at IS NULL),
    CONSTRAINT chk_refresh_tokens_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_refresh_tokens_revoked_at CHECK (revoked_at IS NULL OR revoked_at >= created_at),
    CONSTRAINT chk_refresh_tokens_consumed_at CHECK (
        consumed_at IS NULL OR (consumed_at >= created_at AND consumed_at < expires_at)),
    CONSTRAINT chk_refresh_tokens_updated_at CHECK (updated_at >= created_at)
);

CREATE UNIQUE INDEX uq_refresh_tokens_token_hash ON refresh_tokens(token_hash);
-- Allows at most one open refresh token (neither consumed nor revoked) per session.
CREATE UNIQUE INDEX uq_refresh_tokens_open_session ON refresh_tokens(session_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
-- to search for all refresh tokens belonging to a session, regardless of their state (open, consumed, revoked)
CREATE INDEX idx_refresh_tokens_session_id ON refresh_tokens(session_id);