# Registro de decisiones de arquitectura

Un ADR conserva el contexto de una decisión difícil de revertir. No documenta cada detalle de
implementación. Estados usados: `Propuesta`, `Aceptada`, `Reemplazada`.

| ADR | Decisión | Estado |
| --- | --- | --- |
| [ADR-001](ADR-001-monolito-modular-y-monorepo.md) | Monolito modular dentro de un monorepo | Aceptada |
| [ADR-002](ADR-002-autenticacion-y-sesiones.md) | JWT breve y refresh token opaco rotativo | Aceptada |
| [ADR-003](ADR-003-integridad-de-reservas.md) | Integridad concurrente de las citas en PostgreSQL | Aceptada |
| [ADR-004](ADR-004-contrato-api.md) | OpenAPI code-first y errores Problem Details | Aceptada |

Para una nueva decisión, copia la estructura contexto → decisión → consecuencias y enlázala
en esta tabla. Si cambia una decisión, crea otro ADR y marca el anterior como reemplazado.
