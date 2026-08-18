# ADR-001: Monolito modular y monorepo

- Estado: Aceptada
- Fecha: 2026-08-17

## Contexto

CitaFin necesita backend y frontend, pero será construido y operado por un desarrollador como
proyecto de aprendizaje. Los microservicios añadirían despliegues, redes, consistencia distribuida
y observabilidad sin aportar valor al alcance actual. Aun así, un único paquete sin límites haría
difícil razonar y evolucionar el dominio.

## Decisión

Mantener API, web, documentación e infraestructura en un monorepo. La API será un monolito
modular con límites verificables mediante Spring Modulith. Los módulos se organizan por capacidad
de negocio y exponen únicamente una API explícita.

## Consecuencias

- Un solo despliegue y una transacción local simplifican desarrollo y operación.
- El monorepo permite cambios atómicos de contrato entre API y web.
- Los límites requieren disciplina y pruebas; no ofrecen aislamiento de procesos.
- Separar un módulo en el futuro será posible, pero no es una meta de V1.
