# ADR-003: Integridad concurrente de reservas

- Estado: Aceptada
- Fecha: 2026-08-17

## Contexto

Dos clientes pueden intentar reservar simultáneamente el mismo asesor. Una consulta previa
`exists` no es una garantía porque ambas transacciones pueden observar el slot libre. Además,
una cita ocupa su duración más el búfer aplicable.

## Decisión

Prevalidar en la aplicación para ofrecer errores claros y usar PostgreSQL como última garantía
mediante un rango temporal y una restricción de exclusión para citas activas del mismo asesor.
Usar control optimista para ediciones y `Idempotency-Key` para reintentos de creación.

## Consecuencias

- La base evita dobles reservas incluso bajo carrera o múltiples instancias de API.
- La aplicación debe traducir la violación de restricción a un conflicto HTTP estable.
- El modelo y la migración quedan vinculados a capacidades de PostgreSQL, una dependencia aceptada.
- Deben existir pruebas de integración concurrentes, no solo pruebas unitarias.
