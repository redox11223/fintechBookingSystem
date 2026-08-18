# ADR-002: Autenticación y sesiones

- Estado: Aceptada
- Fecha: 2026-08-17

## Contexto

Una SPA necesita autenticar clientes y personal. Guardar tokens duraderos en almacenamiento
accesible a JavaScript amplifica el impacto de XSS; usar únicamente JWT dificulta revocación y
rotación. La demostración también necesita cierre de sesión real y control de sesiones.

## Decisión

Usar access JWT de vida breve mantenido en memoria del frontend y refresh tokens opacos,
rotativos y revocables en cookie HttpOnly, Secure y con SameSite apropiado. Detectar la
reutilización de tokens rotados. Exigir MFA TOTP a asesores/administradores y ofrecerlo a clientes.

## Consecuencias

- El backend mantiene estado de sesiones y puede revocarlas.
- La SPA debe renovar el access token después de recargar.
- Cookies implican diseñar CORS, CSRF, atributos por entorno y rotación con cuidado.
- La implementación es mayor que un JWT persistido, pero muestra un modelo de amenaza más sólido.
