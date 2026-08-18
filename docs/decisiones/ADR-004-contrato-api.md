# ADR-004: Contrato de la API

- Estado: Aceptada
- Fecha: 2026-08-17

## Contexto

API y SPA evolucionarán juntas. Mantener DTO TypeScript, mocks y documentación a mano crearía
copias divergentes. Los errores ad hoc dificultan una experiencia consistente y pruebas fiables.

## Decisión

Generar OpenAPI desde controladores y modelos anotados, validar el documento en CI y usarlo como
entrada de Orval para cliente Fetch/TanStack Query y mocks MSW. Representar errores con Problem
Details (RFC 9457) y códigos de negocio estables.

## Consecuencias

- Cambios incompatibles quedan visibles y pueden bloquear CI.
- Las anotaciones de API forman parte del trabajo de cada endpoint.
- Code-first puede producir contratos pobres si no se revisan; se añadirá una prueba del documento.
- El frontend reduce código repetitivo y comparte el contrato sin acoplarse a entidades JPA.
