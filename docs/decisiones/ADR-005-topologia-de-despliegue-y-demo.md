# ADR-005: Topología de despliegue y demostración

- Estado: Aceptada
- Fecha: 2026-09-09

## Contexto

CitaFin debe poder probarse desde el portafolio sin asumir un coste permanente ni exponer datos
personales reales. La SPA usa un access token en memoria y un refresh token en cookie, por lo que
la relación entre los dominios de la web y la API afecta CORS, CSRF y `SameSite`. El GitHub Student
Developer Pack disponible incluye un dominio y créditos temporales de Heroku.

## Decisión

Mantener web y API como despliegues independientes del mismo monorepo y publicarlos en orígenes
distintos bajo un único dominio registrable de marca personal:

- el dominio raíz alojará el portafolio Astro;
- `citafin.<dominio>` alojará la SPA React como contenido estático en Cloudflare Pages;
- `api.citafin.<dominio>` alojará Spring Boot en un dyno Heroku Basic con Heroku Postgres
  Essential-0.

No se añadirá un proxy entre la SPA y la API. La API permitirá únicamente el origen exacto de la
SPA, aceptará credenciales solo para ese origen y protegerá los endpoints basados en cookies con
CSRF y validación de `Origin`. La cookie refresh será `HttpOnly`, `Secure`, `SameSite=Strict`,
host-only y tendrá la ruta más limitada que permita el flujo de renovación y logout.

La demostración pública usará cuentas y datos sintéticos reiniciables. El registro se implementará
y probará conforme al contrato, pero un modo de demo configurable lo deshabilitará públicamente con
un Problem Details estable. El flujo completo de correo se evidenciará mediante pruebas y material
del portafolio sin pedir datos personales al visitante.

La infraestructura no se aprovisionará hasta preparar la demostración. Antes de activarla se
comprobarán en las cuentas las condiciones vigentes, la renovación del dominio y los créditos
disponibles. La configuración seguirá externalizada, Flyway será la fuente del esquema y el
despliegue continuará siendo portable a otro proveedor.

## Consecuencias

- El monorepo permite evolucionar el contrato de React y Spring de forma atómica sin obligarlos a
  compartir un artefacto de despliegue.
- Web y API son del mismo sitio, pero no del mismo origen; CORS sigue siendo necesario y no se debe
  confundir `SameSite` con una defensa CSRF completa.
- No existe complejidad ni cuota adicional de un proxy perimetral, pero la dirección de la API es
  pública y todos sus controles deben aplicarse en el backend.
- Los beneficios estudiantiles reducen temporalmente el coste, pero no son una dependencia
  permanente: al vencer se podrá pagar, reducir capacidad o migrar sin cambiar el dominio público.
- Heroku Basic y Essential-0 son suficientes para una demo, pero sus límites de memoria,
  conexiones y almacenamiento deben reflejarse en la configuración operativa.
- Datadog se evaluará en la Entrega 8 para observabilidad; Testmail se reserva para pruebas y no
  sustituye al proveedor de correo transaccional.
