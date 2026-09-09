# Entrega 1 — Identidad y seguridad

Estado: `EN CURSO`.

## Resultado

Registro y verificación de clientes, autenticación con sesiones rotativas, recuperación de
contraseña, invitación de personal, MFA TOTP y eventos de seguridad, conforme a ADR-002.

## Responsabilidades

- **TÚ:** implementar todo el código de producción de identidad, incluidos controladores,
  persistencia, lógica, seguridad y adaptadores.
- **YO:** preparar conceptos y criterios, implementar pruebas de contrato e infraestructura,
  mantener la documentación y revisar concretamente el código que propongas.
- **JUNTOS:** contratos, modelo de amenazas, decisiones duraderas y diseño de las pruebas de lógica
  de negocio; después de acordarlas, YO las implemento.

Trabajaremos una tarea a la vez. La petición de ejecutar una entrega no autoriza al agente a
implementar las tareas marcadas como **TÚ**; una implementación completa requerirá autorización
explícita para la tarea concreta.

## Tareas y responsables

| Bloque | Trabajo | Responsable | Estado |
| --- | --- | --- | --- |
| 1 | Revisar modelo de amenazas y fijar contratos del primer flujo | JUNTOS | TERMINADO |
| 1 | Incorporar dependencias de JWT, SMTP y TOTP | YO | TERMINADO |
| 1 | Preparar configuración externalizada y sus pruebas | YO | TERMINADO |
| 1 | Diseñar tablas, constraints e índices de identidad | JUNTOS | TERMINADO |
| 1 | Escribir la migración Flyway a partir del diseño acordado | TÚ, con guía y revisión | TERMINADO |
| 2 | Diseñar requests, respuestas y errores de registro/verificación | JUNTOS | TERMINADO |
| 2 | Implementar registro, normalización y ciclo del token de verificación | TÚ, con guía y revisión | EN CURSO |
| 2 | Preparar adaptador SMTP, pruebas y documentación OpenAPI | TÚ código; YO pruebas y documentación | PENDIENTE |
| 3 | Implementar login, bloqueo y emisión del access token | TÚ, con guía y revisión | PENDIENTE |
| 3 | Implementar rotación, detección de reutilización y logout | TÚ, con guía y revisión | PENDIENTE |
| 3 | Preparar cookies, CSRF, CORS y pruebas concurrentes | YO | PENDIENTE |
| 4 | Implementar solicitud y consumo de recuperación | TÚ, con guía y revisión | PENDIENTE |
| 4 | Preparar correo, pruebas de expiración y revocación de sesiones | YO | PENDIENTE |
| 5 | Diseñar e implementar invitaciones y bootstrap inicial | JUNTOS; lógica principal TÚ | PENDIENTE |
| 5 | Preparar integración TOTP, cifrado y pruebas de infraestructura | YO | PENDIENTE |
| 5 | Implementar desafíos MFA, replay y códigos de recuperación | TÚ, con guía y revisión | PENDIENTE |
| 6 | Completar autorización, eventos, OpenAPI y documentación | JUNTOS; soporte YO | PENDIENTE |
| 6 | Ejecutar verificación integral y explicar decisiones | JUNTOS | PENDIENTE |

## Decisiones y valores

- BCrypt con coste inicial 12; contraseñas entre 15 y 64 caracteres y como máximo 72 bytes UTF-8
  después de normalizar a NFC. Nunca se truncan ni se recortan.
- Access JWT HS256 de 10 minutos y sesión refresh con máximo absoluto de 30 días.
- Verificación 24 horas, recuperación 30 minutos, invitación 72 horas y desafío MFA 5 minutos.
- Bloqueo de 15 minutos después de 5 fallos dentro de una ventana de 15 minutos.
- Refresh en cookie HttpOnly/SameSite Strict; protección CSRF para operaciones con cookie. La
  relación de dominios entre web y API se fijará antes de implementar cookies, CORS y CSRF.
- El bootstrap crea una invitación, nunca una contraseña ni un secreto versionado.
- La entrega 1 usa SMTP directo. Outbox, reintentos y operación robusta pertenecen a la Entrega 5.

## Primer flujo: registro y verificación

### Modelo de amenazas

| Riesgo | Control acordado |
| --- | --- |
| Enumerar cuentas por la respuesta | Registro y reenvío usan el mismo `202` para correo nuevo o existente |
| Asignarse un rol privilegiado | El request no contiene rol; el servidor asigna siempre `CLIENT` |
| Crear duplicados bajo carrera | Normalización en aplicación e índice único sobre `lower(email)` como garantía final |
| Exponer contraseñas si se filtra la base | BCrypt con salt, coste medido, blocklist y límite explícito de 72 bytes |
| Adivinar o reutilizar verificaciones | Token aleatorio de 256 bits, hash SHA-256 persistido, expiración y consumo atómico |
| Consumir el token desde un escáner de correo | El enlace abre el frontend y este confirma mediante `POST`; la API no muta con `GET` |
| Inyectar un host en el enlace | El origen del frontend viene de configuración, nunca del encabezado `Host` |
| Filtrar secretos en observabilidad | Contraseñas y tokens no se incluyen en logs, eventos ni Problem Details |
| Perder el correo por fallo SMTP | La cuenta permanece y el usuario puede solicitar reenvío |
| Abusar del reenvío | Enfriamiento de 60 segundos y respuesta pública genérica |

Riesgo aceptado para V1: el rate limiting perimetral por IP se completa en la Entrega 8. El
bloqueo por cuenta del login sí pertenece a esta entrega.

### Contrato acordado

`POST /api/v1/auth/register`

```json
{
  "email": "cliente@example.com",
  "password": "una frase suficientemente larga",
  "fullName": "María Quispe",
  "phoneNumber": "+51987654321"
}
```

- `email`: obligatorio, máximo 254 caracteres, se recorta y normaliza a minúsculas.
- `password`: 15–64 puntos de código Unicode y máximo 72 bytes UTF-8 después de NFC; admite
  espacios y Unicode, no se recorta y no exige reglas de composición.
- `fullName`: 2–120 caracteres después de recortar y normalizar espacios.
- `phoneNumber`: E.164 (`+` y entre 8 y 15 dígitos, comenzando por un dígito distinto de cero).
- Un request nunca acepta roles, estado de cuenta ni fecha de verificación.
- Correo nuevo, existente o carrera perdida contra el índice responden `202 Accepted` con el
  mismo mensaje. Un request estructuralmente inválido responde `400` Problem Details.

`POST /api/v1/auth/email-verifications/resend` recibe únicamente `email` y siempre responde
`202`. Para una cuenta existente sin verificar, revoca tokens previos y emite uno nuevo solo si
terminó el enfriamiento; para cuentas inexistentes o verificadas no hace nada observable.

`POST /api/v1/auth/email-verifications/confirm` recibe el token en el cuerpo. Devuelve `204` al
consumirlo y `400` con código `EMAIL_VERIFICATION_TOKEN_INVALID` para token desconocido, vencido,
revocado o usado. El correo usa una URL configurada con fragmento, por ejemplo
`https://app.citafin.dev/verify-email#token=...`; la API nunca acepta el token en un `GET`.

### Secuencia transaccional esperada

1. Validar y normalizar entrada; la contraseña se normaliza a NFC antes de medir caracteres y bytes.
2. Comparar la contraseña completa contra la blocklist y calcular BCrypt.
3. Crear `User`, rol `CLIENT`, `Client` y token de verificación en una sola transacción.
4. Guardar solo el SHA-256 del token aleatorio.
5. Enviar el correo después del commit; un fallo de SMTP no revierte la cuenta.
6. Al confirmar, bloquear/consumir el token y establecer `email_verified_at` atómicamente.

### Criterios de aceptación del flujo

- Registro válido crea exactamente una cuenta, un rol `CLIENT`, un perfil y un token.
- Mayúsculas del correo y peticiones concurrentes no crean duplicados.
- Correo nuevo y existente son indistinguibles desde el contrato público.
- Contraseñas débiles, fuera del rango o mayores de 72 bytes se rechazan sin truncamiento.
- Token válido funciona una vez; vencido, revocado, usado o aleatorio produce el mismo error.
- Reenviar invalida el token anterior y respeta el enfriamiento.
- Fallar SMTP no elimina datos confirmados y no expone secretos.

## Configuración preparada

`IdentityProperties` separa políticas no secretas de `IdentitySecretsProperties`. Los secretos
JWT y MFA no tienen valores versionados y serán validados cuando se activen sus beans
criptográficos. Los perfiles pueden sobrescribir duraciones y costes sin cambiar código.

SMTP usa timeouts finitos de conexión, lectura y escritura. El perfil `prod` fuerza la cookie
`Secure`; el perfil local conserva compatibilidad con HTTP. Esta preparación no habilita todavía
JWT, cookies, CORS, correo ni MFA: solo define y valida su configuración.

### Dependencias verificadas

| Dependencia | Uso futuro | Motivo |
| --- | --- | --- |
| `spring-boot-starter-oauth2-resource-server` | Validar access JWT Bearer | Integra Spring Security e incluye JOSE/Nimbus para firma y validación |
| `spring-boot-starter-mail` | Adaptador SMTP de identidad | Proporciona la abstracción autoconfigurada `JavaMailSender` |
| `dev.samstevens.totp:totp:1.7.1` | Generar/verificar TOTP y recovery codes | Evita implementar RFC 6238; se usa el núcleo, no su starter externo |

Maven resolvió Spring Boot 4.1.0, Spring Security JOSE 7.1.0 y java-totp 1.7.1. La compilación y
las pruebas aisladas de propiedades, modularidad y Problem Details pasan.

### Política de contraseñas implementada

`PasswordPolicy` normaliza a NFC sin recortar la entrada y aplica, en orden determinista, presencia,
límites de puntos de código Unicode y límite de bytes UTF-8. Las pruebas unitarias cubren ambos
extremos de longitud, caracteres suplementarios, el límite de 72 bytes de BCrypt, precedencia de
errores y ausencia de la contraseña rechazada en el mensaje de excepción.

### Generación de tokens de verificación implementada

`EmailVerificationTokenGenerator` crea 32 bytes mediante `SecureRandom`, los representa como
Base64 URL-safe sin padding y calcula SHA-256 sobre esa representación textual. El valor original
solo se usará para construir el correo; la persistencia recibe su hash de 32 bytes. Las pruebas
unitarias fijan formato, entropía, vector SHA-256, correspondencia entre valor y hash, estados
inválidos, copias defensivas y ausencia del token en errores.

## Continuidad

Al retomar: leer `AGENTS.md`, `docs/roadmap.md`, este archivo y ADR-002; revisar `git status` y
continuar en el bloque 2. Los DTO HTTP y el mapeo JPA de `email_verification_tokens` están
preparados; la política de contraseñas y la generación segura del token ya cuentan con pruebas. El
siguiente ejercicio es encapsular BCrypt con el coste configurado; la blocklist se incorporará
después y antes de orquestar el registro. No es necesario releer todos los documentos.
