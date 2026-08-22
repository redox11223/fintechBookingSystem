# Roadmap y responsabilidades

Estados: `PENDIENTE`, `EN CURSO`, `TERMINADO`. Responsables: `TÚ`, `YO`, `JUNTOS`.

## Entrega 0 — Baseline y documentación (`TERMINADO`)

| Trabajo | Responsable | Estado |
| --- | --- | --- |
| Crear monorepo y mover el backend | YO | TERMINADO |
| Crear documentación y ADR iniciales | YO | TERMINADO |
| Rediseñar V1 de Flyway | JUNTOS: TÚ escribes SQL; YO preparo modelo, reviso y pruebo | TERMINADO |
| Actualizar Compose con PostgreSQL y Mailpit | YO, con explicación | TERMINADO |
| Corregir configuración y dejar build verde | YO, con explicación | TERMINADO |
| Incorporar Spring Modulith y prueba de estructura | YO | TERMINADO |
| Incorporar OpenAPI y validación básica | YO | TERMINADO |
| Implementar Problem Details | TÚ, con guía y revisión | TERMINADO |
| Implementar auditoría inicial | TÚ, con guía y revisión | TERMINADO |
| Incorporar CI base | YO | TERMINADO |

**Salida:** repositorio reproducible, documentación base, migración validada en PostgreSQL y
build verde. No incluye completar casos de uso del negocio.

## Entrega 1 — Identidad y seguridad (`PENDIENTE`)

Registro/verificación de clientes, login, refresh rotativo, logout, recuperación, roles,
invitación de personal, MFA TOTP y eventos de seguridad. La división se acordará antes de iniciar.

## Entrega 2 — Organización, catálogo y asesores (`PENDIENTE`)

Sedes, categorías, servicios, políticas, perfiles de asesor y asignación de competencias.

## Entrega 3 — Disponibilidad (`PENDIENTE`)

Horarios recurrentes, ausencias, modalidades/sedes, cálculo de slots y detección de conflictos.

## Entrega 4 — Reservas (`PENDIENTE`)

Creación idempotente, asignación automática, concurrencia, reprogramación, cancelación,
historial, ETag y políticas.

## Entrega 5 — Notificaciones y operación (`PENDIENTE`)

Correo por adaptadores, reuniones simuladas, recordatorios, reintentos, logs, métricas y salud.

## Entrega 6 — Web pública y autenticación (`PENDIENTE`)

React/TypeScript, sistema visual, landing, catálogo, flujo de reserva y cuenta.

## Entrega 7 — Portales (`PENDIENTE`)

Paneles de cliente, agenda diaria/semanal del asesor y administración de catálogo/operación.

## Entrega 8 — Endurecimiento y demo (`PENDIENTE`)

Pruebas E2E y de carga, accesibilidad, seguridad, contenedores, despliegue y runbook.

## Próximo punto de control

Definir la división `TÚ`, `YO` y `JUNTOS` de la Entrega 1 antes de implementar identidad y
seguridad. Acordar primero los casos de uso, contratos y criterios de aceptación del registro y
la autenticación, sin ampliar los límites de la versión 1.
