# Producto y alcance

## Visión

CitaFin permite que una persona descubra servicios de orientación financiera y reserve una
cita con un asesor de una única institución. La experiencia debe reducir la coordinación
manual, mostrar disponibilidad confiable y permitir que clientes y personal administren el
ciclo de vida de la cita.

Es una marca y una institución ficticias. No es un marketplace, no presta asesoría financiera
por sí misma y no debe almacenar información financiera sensible.

## Actores

- **Visitante:** consulta categorías, servicios y disponibilidad pública.
- **Cliente:** administra su cuenta y sus propias citas.
- **Asesor:** consulta su agenda y registra el resultado operativo de una cita.
- **Administrador:** gestiona sedes, catálogo, asesores, horarios y excepciones.

Un usuario puede tener más de un rol. Los asesores y administradores son invitados por un
administrador; los clientes pueden registrarse por sí mismos.

## Propuesta de valor de V1

1. Explorar un catálogo claro de servicios financieros.
2. Consultar horarios por servicio, modalidad, sede y asesor opcional.
3. Reservar con un asesor concreto o dejar que el sistema asigne uno compatible.
4. Reprogramar o cancelar dentro de políticas explícitas.
5. Operar agendas y excepciones sin producir dobles reservas.
6. Recibir correos transaccionales y recordatorios.

## Reglas acordadas

- Modalidades virtual y presencial; una cita presencial pertenece a una sede.
- Confirmación inmediata, sin aprobación pendiente.
- Intervalos base de 15 minutos, anticipación mínima de 4 horas, horizonte de 60 días,
  límite de cancelación/reprogramación de 12 horas y búfer posterior de 15 minutos.
- Los valores anteriores son predeterminados y un servicio puede sobrescribirlos.
- La reasignación automática elige un asesor compatible con menor carga diaria y un
  desempate estable.
- Reprogramar conserva el identificador de la cita y registra un evento histórico.
- Estados finales de V1: `CONFIRMED`, `COMPLETED`, `CANCELLED` y `NO_SHOW`.
- Cambiar horarios o ausencias no puede invalidar citas confirmadas: se rechaza la operación
  y se muestran los conflictos.
- El cliente aporta un motivo breve obligatorio, comentario opcional y consentimiento.
- El asesor puede guardar una nota operativa interna breve, no una ficha financiera.
- No existe eliminación genérica de datos maestros: se desactivan.

## Datos personales mínimos

Nombre, correo verificado y teléfono normalizado en formato E.164. V1 no solicita documento
nacional, adjuntos, ingresos, deudas, números de cuenta ni otra información financiera.

## Fuera de alcance de V1

- Pagos.
- SMS o WhatsApp.
- Carga de documentos y formularios dinámicos.
- Integración real con Meet, Teams, Zoom o calendarios externos.
- Lista de espera, citas grupales o recurrentes.
- Expediente financiero, CRM o recomendaciones de inversión.
- Varias instituciones, microservicios, modo oscuro y analítica avanzada.

## Criterio de éxito del portafolio

Una demostración desplegada con datos sintéticos debe cubrir el recorrido visitante → cliente
→ reserva → gestión por asesor, y evidenciar diseño modular, integridad concurrente, seguridad,
contratos API, pruebas automatizadas, observabilidad y decisiones justificadas.
