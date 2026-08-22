# Arquitectura

## Vista general

CitaFin será un monolito modular desplegable como una sola API, acompañado de una aplicación
web independiente. Esta forma reduce complejidad operativa y conserva límites de dominio que
pueden verificarse con Spring Modulith.

```text
React + TypeScript
       |
   REST/OpenAPI
       |
Spring Boot modular monolith
       |
PostgreSQL      proveedores por adaptador
                (correo y reunión virtual)
```

## Monorepo

```text
apps/api       Java 17, Spring Boot, Maven, Flyway
apps/web       React, TypeScript, Vite (Entrega 6)
docs           producto, requisitos, arquitectura y ADR
compose.yaml   servicios locales
```

## Módulos objetivo del backend

- `identity`: cuentas, roles, sesiones, verificación, recuperación y MFA.
- `customer`: perfil mínimo del cliente.
- `organization`: sedes y configuración institucional.
- `catalog`: categorías, servicios y políticas configurables.
- `advisor`: perfil, competencias, horarios y ausencias.
- `booking`: disponibilidad, citas, idempotencia e historial.
- `notification`: plantillas, entregas y reintentos.
- `shared`: piezas técnicas pequeñas y estables; no debe convertirse en un módulo comodín.

Los módulos se comunican mediante API pública explícita y eventos internos cuando exista una
necesidad real de desacoplamiento. No se añadirá infraestructura de eventos persistentes antes
de implementar el primer caso de uso que la requiera.

### Límites actuales verificados

```text
identity ───────────────────────────────→ shared
customer ───────────────→ identity ────→ shared
advisor  ───────────────→ identity ────→ shared
catalog  ───────────────────────────────→ shared
booking  ─→ customer, advisor, catalog ─→ shared
```

Spring Modulith verifica estas dependencias durante las pruebas. `shared` es temporalmente un
módulo abierto porque sus paquetes técnicos (`audit`, `config`, `exception` y `json`) son usados
por los módulos de negocio. Cuando sus contratos se estabilicen se expondrán interfaces nombradas
para cerrarlo. Las relaciones JPA también respetan la dirección del grafo: `Client` y `Advisor`
referencian a `User`, pero `User` no mantiene referencias inversas.

## Persistencia e integridad

- Flyway es la fuente de verdad y Hibernate usa `ddl-auto: validate`.
- Las migraciones serán pequeñas y aparecerán con el módulo que necesita el cambio.
- Las citas usan bloqueo optimista para ediciones y una restricción de exclusión PostgreSQL
  sobre el rango ocupado para la garantía final contra solapamientos.
- La capa de aplicación prevalida conflictos, pero nunca sustituye la restricción de base de datos.
- Desactivar conserva referencias e historial; las eliminaciones en cascada se evaluarán con
  especial cuidado sobre información auditable.

## API

- REST JSON, documentada code-first con OpenAPI.
- En desarrollo, el documento está en `/v3/api-docs` y Swagger UI en `/swagger-ui.html`; ambos se
  deshabilitan mediante el perfil `prod`.
- Problem Details (RFC 9457) para errores uniformes.
- `Idempotency-Key` en creación de citas y `ETag`/`If-Match` en sus mutaciones.
- Paginación por página para conjuntos administrativos y keyset para historiales extensos.
- El cliente TypeScript y mocks se generarán desde OpenAPI para evitar contratos duplicados.

## Seguridad

El access token JWT vive solo en memoria del frontend. El refresh token es opaco, rotativo y
se guarda en cookie HttpOnly/Secure. Las sesiones se pueden revocar. MFA TOTP es obligatorio
para personal. Los endpoints públicos se limitan al catálogo y la disponibilidad necesarios
para reservar; la autorización se valida en el servidor por recurso y rol.

## Operación prevista

- Desarrollo: PostgreSQL y Mailpit mediante Compose.
- Demo: frontend estático, contenedor de API, PostgreSQL administrado y Resend.
- Adaptadores: Mailpit/Resend para correo y proveedor simulado para reuniones.
- Métricas Micrometer/Prometheus, health checks y logs estructurados con trace ID.

Las decisiones que requieren contexto y alternativas quedan registradas en
[`docs/decisiones`](decisiones/README.md).
