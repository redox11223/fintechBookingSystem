# CitaFin

CitaFin es una plataforma de portafolio para reservar consultorías financieras virtuales y
presenciales en una institución peruana ficticia. El proyecto busca demostrar un desarrollo
profesional de extremo a extremo y, al mismo tiempo, servir como espacio de aprendizaje.

> CitaFin is a portfolio project for booking virtual and in-person financial consultations.
> It is being built as a production-minded learning project, with explicit architectural
> decisions, tests and documentation.

## Estado

El proyecto se encuentra en la **Entrega 0: baseline y documentación**. El código existente
del catálogo es un punto de partida y todavía no representa una API estable ni lista para
producción.

## Estructura

```text
apps/
  api/                 API Spring Boot
  web/                 aplicación React (pendiente)
docs/
  decisiones/          Architecture Decision Records (ADR)
compose.yaml           infraestructura local de desarrollo
```

## Documentación

- [Visión y alcance del producto](docs/producto.md)
- [Requisitos de la versión 1](docs/requisitos.md)
- [Modelo de datos base](docs/modelo-datos.md)
- [Arquitectura](docs/arquitectura.md)
- [Registro de decisiones](docs/decisiones/README.md)

## Desarrollo local

Requisitos: Java 17 y Docker con Compose.

```bash
cp .env.example .env
docker compose up -d --wait
```

Esto inicia PostgreSQL en `localhost:5432`, el servidor SMTP de Mailpit en `localhost:1025`
y su interfaz web en [http://localhost:8025](http://localhost:8025). Los puertos y credenciales
locales se pueden cambiar en `.env`.

El backend se ejecuta desde su propio módulo:

```bash
cd apps/api
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Spring Boot detecta el Compose ubicado en la raíz. Si `5432` ya está ocupado, cambia `DB_PORT`
en `.env` antes de iniciar la infraestructura.

Para detener la infraestructura sin borrar los datos:

```bash
docker compose down
```

El volumen `citafin_postgres-data` conserva PostgreSQL entre reinicios. Ejecutar
`docker compose down --volumes` también elimina esos datos locales.

No uses información personal o financiera real. Las cuentas de demostración serán sintéticas.
