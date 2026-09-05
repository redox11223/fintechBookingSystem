CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash BYTEA NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    -- Both timestamps are optional, but at most one of them may be set.
    consumed_at TIMESTAMPTZ, -- Indicates that the token was used to verify the email address.
    revoked_at TIMESTAMPTZ, -- Indicates that the token was replaced or otherwise invalidated.
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_email_verification_tokens_consumed_revoked CHECK (
        (consumed_at IS NULL AND revoked_at IS NULL) OR
        (consumed_at IS NOT NULL AND revoked_at IS NULL) OR
        (consumed_at IS NULL AND revoked_at IS NOT NULL)
    ),
    CONSTRAINT chk_email_verification_tokens_hash_length CHECK (octet_length(token_hash) = 32),
    CONSTRAINT chk_email_verification_tokens_expiry CHECK (expires_at > created_at),
    CONSTRAINT chk_email_verification_tokens_consumed_at CHECK (consumed_at >= created_at AND consumed_at < expires_at),
    CONSTRAINT chk_email_verification_tokens_revoked_at CHECK (revoked_at >= created_at),
    CONSTRAINT chk_email_verification_tokens_updated_at CHECK (updated_at >= created_at)
);

-- Indexes
CREATE UNIQUE INDEX uq_email_verification_tokens_token_hash ON email_verification_tokens(token_hash);

-- Allows at most one open verification token (neither consumed nor revoked) per user.
-- This is a partial unique index: uniqueness applies only to rows where consumed_at and
-- revoked_at are NULL. Used or revoked tokens remain outside the index as history.
-- Resending verification must revoke the previous token before inserting a new one in the
-- same transaction, or the insert will violate this index.
CREATE UNIQUE INDEX uq_email_verification_tokens_open_user ON email_verification_tokens(user_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
