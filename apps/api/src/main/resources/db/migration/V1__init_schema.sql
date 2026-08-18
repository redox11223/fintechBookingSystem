---------------------------------------------------------
-- 0. EXTENSIONES
---------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS btree_gist;

---------------------------------------------------------
-- 1. SEGURIDAD Y USUARIOS (Spring Security Core)
---------------------------------------------------------
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(254) NOT NULL, -- estandar RFC 5321 para longitud max de un correo
                       password_hash VARCHAR(255) NOT NULL,
                       email_verified_at TIMESTAMPTZ,
                       is_active BOOLEAN DEFAULT TRUE NOT NULL,
                       created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL

);

CREATE TABLE user_roles(
                      user_id UUID NOT NULL,
                      role VARCHAR(30) NOT NULL,
                      CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
                      CONSTRAINT fk_user_roles FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                      CONSTRAINT chk_user_roles CHECK (role IN ('CLIENT', 'ADVISOR', 'ADMIN'))

);

---------------------------------------------------------
-- 2. PERFILES (Herencia 1 a 1 desde Users)
---------------------------------------------------------
CREATE TABLE clients (
                         id UUID PRIMARY KEY,
                         user_id UUID NOT NULL,
                         full_name VARCHAR(120) NOT NULL,
                         phone_number VARCHAR(16) NOT NULL,
                         created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

                         CONSTRAINT uq_clients_user_id UNIQUE (user_id),
                         CONSTRAINT fk_client_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE advisors (
                          id UUID PRIMARY KEY,
                          user_id UUID NOT NULL,
                          full_name VARCHAR(120) NOT NULL,
                          internal_code VARCHAR(30) NOT NULL,
                          specialization VARCHAR(120) NOT NULL DEFAULT 'General',
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

                          CONSTRAINT uq_advisors_user_id UNIQUE (user_id),
                          CONSTRAINT fk_advisor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

---------------------------------------------------------
-- 3. CATÁLOGO DE SERVICIOS
---------------------------------------------------------
CREATE TABLE service_categories (
                                    id UUID PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL,
                                    description VARCHAR(500),
                                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE financial_services (
                                    id UUID PRIMARY KEY,
                                    category_id UUID NOT NULL,
                                    name VARCHAR(120) NOT NULL,
                                    description VARCHAR(1000),
                                    duration_minutes INTEGER NOT NULL,
                                    is_active BOOLEAN DEFAULT TRUE NOT NULL,
                                    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                    CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES service_categories(id) ON DELETE RESTRICT,
                                    CONSTRAINT chk_duration_valid CHECK (duration_minutes > 0
                                        AND duration_minutes <= 480
                                        AND duration_minutes % 15=0
                                        )
);

---------------------------------------------------------
-- 4. MOTOR DE DISPONIBILIDAD (Horarios y Feriados)
---------------------------------------------------------
CREATE TABLE advisor_schedules (
                                   id UUID PRIMARY KEY,
                                   advisor_id UUID NOT NULL,
                                   day_of_week INTEGER NOT NULL, -- 1=Lunes, 7=Domingo
                                   start_time TIME NOT NULL,
                                   end_time TIME NOT NULL,
                                   created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                   CONSTRAINT fk_schedule_advisor FOREIGN KEY (advisor_id) REFERENCES advisors(id) ON DELETE RESTRICT,
                                   CONSTRAINT chk_valid_day CHECK (day_of_week BETWEEN 1 AND 7),
                                   CONSTRAINT chk_valid_time CHECK (start_time < end_time)
);

CREATE TABLE time_offs (
                           id UUID PRIMARY KEY,
                           advisor_id UUID, -- NULL = feriado nacional/global
                           starts_at TIMESTAMPTZ NOT NULL,
                           ends_at TIMESTAMPTZ NOT NULL,
                           reason VARCHAR(500) NOT NULL,
                           created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
                           updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

                           CONSTRAINT chk_valid_offtime CHECK (starts_at < ends_at),
                           CONSTRAINT fk_timeoff_advisor FOREIGN KEY (advisor_id) REFERENCES advisors(id) ON DELETE RESTRICT
);

---------------------------------------------------------
-- 5. TRANSACCIONAL: RESERVAS
---------------------------------------------------------
CREATE TABLE appointments (
                              id UUID PRIMARY KEY,
                              client_id UUID NOT NULL,
                              advisor_id UUID NOT NULL,
                              service_id UUID NOT NULL,

                              starts_at TIMESTAMPTZ NOT NULL,
                              ends_at TIMESTAMPTZ NOT NULL,
                              blocked_until TIMESTAMPTZ NOT NULL,
                              status VARCHAR(30) NOT NULL,
                              reason VARCHAR(500) NOT NULL,
                              client_comment VARCHAR(1000),

                              version BIGINT NOT NULL DEFAULT 0,
                              consented_at TIMESTAMPTZ NOT NULL,
                              consent_version VARCHAR(30) NOT NULL,
                              created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
                              updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,

                              CONSTRAINT fk_appointment_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
                              CONSTRAINT fk_appointment_advisor FOREIGN KEY (advisor_id) REFERENCES advisors(id) ON DELETE RESTRICT,
                              CONSTRAINT fk_appointment_service FOREIGN KEY (service_id) REFERENCES financial_services(id) ON DELETE RESTRICT,
                              CONSTRAINT chk_time_validity CHECK (starts_at < ends_at),
                              CONSTRAINT chk_buffer_validity CHECK (blocked_until >= ends_at),
                              CONSTRAINT chk_appointment_status CHECK (status IN ('CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
                              CONSTRAINT exclude_advisor_double_booking EXCLUDE USING gist (
                                        advisor_id WITH =,
                                        tstzrange(starts_at, blocked_until, '[)') WITH &&
                                    ) WHERE (status = 'CONFIRMED')
);

-- Índices
-- Indice funcional unico para email:
-- 1. Garantiza unicidad case-insensitive (evita registrar 'Juan@x.com' y 'juan@x.com').
-- 2. Optimiza lecturas, convierte a minusculas solo para la comparacion .
-- 3. Mantiene el texto original (mayusculas/minusculas) guardado intacto en la tabla.
-- 4. No usar "email UNIQUE" adicional para evitar mantener dos índices equivalentes.
CREATE UNIQUE INDEX uq_users_email_lower ON users(lower(email));
-- Indice compuesto, Unicidad case-insensitive por categoría
-- 1. Garantiza que dentro de la misma categoría no existan dos servicios duplicados
-- 2. Pero sí permite repetirlos entre categorías distintas
CREATE UNIQUE INDEX uq_financial_services_category_name_lower
    ON financial_services (category_id, lower(name));
CREATE UNIQUE INDEX uq_advisors_internal_code_lower ON advisors(lower(internal_code));
CREATE UNIQUE INDEX uq_service_categories_name_lower ON service_categories(lower(name));
CREATE UNIQUE INDEX uq_advisor_schedules_exact_block ON advisor_schedules(advisor_id,day_of_week,
    start_time,end_time);
CREATE INDEX idx_time_offs_advisor_start ON time_offs(advisor_id,starts_at);
-- Indice compuesto optimizado para el historial del cliente:
-- 1. Agrupa rápido por cliente (client_id).
-- 2. Devuelve los resultados ya ordenados cronológicamente (starts_at DESC).
-- 3. Usa 'id DESC' como desempate único para evitar saltos o duplicados al paginar.
-- Nota: Solo se activa si la consulta filtra por 'client_id' (regla del extremo izquierdo).
CREATE INDEX idx_appointments_client_history ON appointments(client_id,starts_at DESC, id DESC);
CREATE INDEX idx_appointments_advisor_agenda ON appointments(advisor_id,starts_at,id);
CREATE INDEX idx_appointments_advisor_confirmed ON appointments(advisor_id,starts_at,blocked_until)
   WHERE status = 'CONFIRMED';
