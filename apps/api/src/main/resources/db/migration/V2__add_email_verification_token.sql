CREATE TABLE email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash BYTEA NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    -- ambos consumed y revoked son opcionales, pero solo uno de ellos puede estar establecido a la vez
    consumed_at TIMESTAMPTZ, --indica que el token ha sido usado para verificar la dirección de correo electrónico
    revoked_at TIMESTAMPTZ, --indica que el token ha sido reemplazado por un nuevo token o invalidado por alguna razón
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL ,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_email_verification_tokens_consumed_revoked CHECK (
        (consumed_at IS NULL AND revoked_at IS NULL) OR
        (consumed_at IS NOT NULL AND revoked_at IS NULL) OR
        (consumed_at IS NULL AND revoked_at IS NOT NULL)
    ),
    CONSTRAINT chk_email_verification_tokens_hash_length CHECK (octet_length(token_hash) = 32),
    CONSTRAINT chk_email_verification_tokens_expiry CHECK ( expires_at > created_at ),
    CONSTRAINT chk_email_verification_tokens_consumed_at CHECK ( consumed_at >= created_at AND consumed_at < expires_at ),
    CONSTRAINT chk_email_verification_tokens_revoked_at CHECK ( revoked_at >= created_at ),
    CONSTRAINT chk_email_verification_tokens_updated_at CHECK ( updated_at >= created_at )
);

--INDICES
CREATE UNIQUE INDEX uq_email_verification_tokens_token_hash ON email_verification_tokens(token_hash);

-- Garantiza como máximo UN token de verificación "vivo" (no consumido ni revocado) por usuario.
-- Es un índice único PARCIAL: la restricción de unicidad solo aplica a las filas donde
-- consumed_at y revoked_at son NULL; los tokens ya usados o revocados quedan fuera y pueden
-- acumularse libremente como historial.
-- Al reenviar verificación, el flujo debe REVOCAR el token anterior antes de insertar uno nuevo
-- (en la misma transacción), o el INSERT chocará contra esta restricción.
CREATE UNIQUE INDEX uq_email_verification_tokens_open_user ON email_verification_tokens(user_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
