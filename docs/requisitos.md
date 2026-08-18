# Requisitos de la versión 1

Este documento expresa capacidades y condiciones verificables. Los contratos concretos de
HTTP vivirán en OpenAPI cuando la API se estabilice.

## Requisitos funcionales

### Identidad y acceso

- **RF-01:** un cliente puede registrarse, verificar su correo e iniciar sesión.
- **RF-02:** una sesión puede renovarse, cerrarse y recuperarse mediante correo.
- **RF-03:** asesor y administrador usan MFA TOTP obligatorio; para cliente es opcional.
- **RF-04:** solo un administrador invita y activa personal.
- **RF-05:** autorización por roles, con acceso exclusivo del cliente a sus propios recursos.

### Organización y catálogo

- **RF-06:** el administrador gestiona sedes activas y sus datos públicos.
- **RF-07:** el administrador gestiona categorías y servicios, duración, modalidades y políticas.
- **RF-08:** el administrador asigna a cada asesor los servicios que puede atender.
- **RF-09:** visitantes consultan únicamente elementos activos del catálogo.

### Disponibilidad

- **RF-10:** el administrador define bloques recurrentes por asesor, modalidad y sede física.
- **RF-11:** puede registrar ausencias o bloqueos globales y por asesor.
- **RF-12:** el sistema calcula slots para un rango máximo de 31 días respetando horarios,
  duración, búferes, políticas, ausencias y citas existentes.
- **RF-13:** un cambio de agenda que choque con citas confirmadas se rechaza mostrando conflictos.

### Citas

- **RF-14:** un cliente autenticado reserva un slot disponible con asesor elegido o automático.
- **RF-15:** crear una cita requiere `Idempotency-Key` para no duplicar reservas por reintentos.
- **RF-16:** el cliente consulta su historial y próximas citas.
- **RF-17:** el cliente cancela o reprograma dentro de la política aplicable.
- **RF-18:** asesor o administrador pueden actuar fuera de política con motivo obligatorio.
- **RF-19:** modificar una cita exige control optimista mediante `ETag` e `If-Match`.
- **RF-20:** el asesor marca una cita como completada o no atendida y puede añadir una nota
  operativa breve.
- **RF-21:** cada transición o reprogramación relevante queda en un historial auditable.

### Notificaciones

- **RF-22:** el sistema envía confirmaciones, cambios, cancelaciones y recordatorios por correo.
- **RF-23:** un proveedor abstracto crea un enlace virtual simulado para citas remotas.
- **RF-24:** fallar al enviar un correo no revierte una reserva válida; el envío puede reintentarse.

## Requisitos no funcionales

- **RNF-01 Seguridad:** contraseñas con algoritmo adaptativo, tokens de acceso breves en memoria
  del navegador y refresh tokens opacos, rotativos y revocables en cookie HttpOnly/Secure.
- **RNF-02 Privacidad:** minimización de datos y prohibición de PII real en la demostración.
- **RNF-03 Integridad:** PostgreSQL debe impedir dos citas activas superpuestas para un asesor;
  la aplicación también prevalidará para responder con un error comprensible.
- **RNF-04 Contratos:** errores HTTP con Problem Details (RFC 9457), API documentada en OpenAPI
  y validada en integración continua.
- **RNF-05 Rendimiento local:** p95 orientativo de CRUD ≤ 300 ms, disponibilidad ≤ 750 ms y
  reserva ≤ 500 ms con una mezcla de 50 solicitudes por segundo.
- **RNF-06 Escala objetivo:** aproximadamente 100 asesores, 50 000 clientes y un millón de
  citas históricas.
- **RNF-07 Accesibilidad:** interfaz responsive conforme a WCAG 2.2 AA.
- **RNF-08 Observabilidad:** logs JSON con identificador de correlación, health checks y métricas.
- **RNF-09 Calidad:** build reproducible, migraciones verificadas y pruebas proporcionales al riesgo.
- **RNF-10 Zona horaria:** instantes persistidos en UTC y reglas de negocio interpretadas con
  una zona explícita; la interfaz presenta la zona al usuario.

## Estrategia de paginación y consulta

- Catálogos y pantallas administrativas usan paginación por página, ordenamiento limitado a
  campos permitidos y filtros tipados.
- Historiales crecientes de citas usan cursor/keyset con desempate por identificador.
- Disponibilidad no se pagina: se consulta por intervalo con un máximo de 31 días.
- Nunca se exponen directamente propiedades arbitrarias de entidad como criterios de orden.
