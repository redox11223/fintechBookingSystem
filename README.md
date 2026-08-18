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
producción. Consulta el [roadmap](docs/roadmap.md) para ver el avance y los responsables.

## Estructura

```text
apps/
  api/                 API Spring Boot
  web/                 aplicación React (pendiente)
docs/
  decisiones/          Architecture Decision Records (ADR)
compose.yaml           infraestructura de desarrollo (pendiente de revisión)
```

## Documentación

- [Visión y alcance del producto](docs/producto.md)
- [Requisitos de la versión 1](docs/requisitos.md)
- [Arquitectura](docs/arquitectura.md)
- [Roadmap y responsabilidades](docs/roadmap.md)
- [Acuerdo de colaboración y aprendizaje](docs/colaboracion.md)
- [Registro de decisiones](docs/decisiones/README.md)

## Backend local

Requisitos actuales: Java 17 y Docker. La configuración y el Compose aún serán corregidos
como parte de la Entrega 0; por eso estos comandos solo documentan la ubicación del módulo:

```bash
cd apps/api
./mvnw test
```

No uses información personal o financiera real. Las cuentas de demostración serán sintéticas.
