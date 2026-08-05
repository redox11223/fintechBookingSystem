---------------------------------------------------------
-- 1. SEGURIDAD Y USUARIOS (Spring Security Core)
---------------------------------------------------------
CREATE TABLE users (
                       id UUID PRIMARY KEY, -- UUIDv7 generado por la aplicación
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       is_active BOOLEAN DEFAULT TRUE NOT NULL,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

                       CONSTRAINT chk_user_role CHECK (role IN ('ROLE_CLIENT', 'ROLE_ADVISOR', 'ROLE_ADMIN'))
);

---------------------------------------------------------
-- 2. PERFILES (Herencia 1 a 1 desde Users)
---------------------------------------------------------
CREATE TABLE clients (
                         id UUID PRIMARY KEY,
                         user_id UUID NOT NULL UNIQUE,
                         full_name VARCHAR(100) NOT NULL,
                         document_type VARCHAR(20) NOT NULL,
                         document_number VARCHAR(20) NOT NULL,
                         phone_number VARCHAR(20) NOT NULL,
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

                         CONSTRAINT fk_client_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                         CONSTRAINT uq_client_document UNIQUE (document_type, document_number)
);

CREATE TABLE advisors (
                          id UUID PRIMARY KEY,
                          user_id UUID NOT NULL UNIQUE,
                          full_name VARCHAR(100) NOT NULL,
                          internal_code VARCHAR(20) UNIQUE,
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

                          CONSTRAINT fk_advisor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

---------------------------------------------------------
-- 3. CATÁLOGO DE SERVICIOS
---------------------------------------------------------
CREATE TABLE service_categories (
                                    id UUID PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL UNIQUE,
                                    description VARCHAR(255),
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE financial_services (
                                    id UUID PRIMARY KEY,
                                    category_id UUID NOT NULL,
                                    name VARCHAR(100) NOT NULL,
                                    duration_minutes INT NOT NULL,
                                    is_active BOOLEAN DEFAULT TRUE NOT NULL,
                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                    CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES service_categories(id) ON DELETE RESTRICT,
                                    CONSTRAINT chk_duration_positive CHECK (duration_minutes > 0)
);

---------------------------------------------------------
-- 4. MOTOR DE DISPONIBILIDAD (Horarios y Feriados)
---------------------------------------------------------
CREATE TABLE advisor_schedules (
                                   id UUID PRIMARY KEY,
                                   advisor_id UUID NOT NULL,
                                   day_of_week INT NOT NULL, -- 1=Lunes, 7=Domingo
                                   start_time TIME NOT NULL,
                                   end_time TIME NOT NULL,
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

                                   CONSTRAINT fk_schedule_advisor FOREIGN KEY (advisor_id) REFERENCES advisors(id) ON DELETE CASCADE,
                                   CONSTRAINT chk_valid_day CHECK (day_of_week BETWEEN 1 AND 7),
                                   CONSTRAINT chk_valid_time CHECK (start_time < end_time)
);

CREATE TABLE time_offs (
                           id UUID PRIMARY KEY,
                           advisor_id UUID, -- NULL = feriado nacional/global
                           off_date DATE NOT NULL,
                           reason VARCHAR(255),
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                           updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,


                           CONSTRAINT fk_timeoff_advisor FOREIGN KEY (advisor_id) REFERENCES advisors(id) ON DELETE CASCADE
);

---------------------------------------------------------
-- 5. TRANSACCIONAL: RESERVAS
---------------------------------------------------------
CREATE TABLE appointments (
                              id UUID PRIMARY KEY,
                              client_id UUID NOT NULL,
                              advisor_id UUID NOT NULL,
                              service_id UUID NOT NULL,

                              start_time TIMESTAMP WITH TIME ZONE NOT NULL,
                              end_time TIMESTAMP WITH TIME ZONE NOT NULL,
                              status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',

                              version BIGINT NOT NULL DEFAULT 0,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

                              CONSTRAINT fk_appointment_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT,
                              CONSTRAINT fk_appointment_advisor FOREIGN KEY (advisor_id) REFERENCES advisors(id) ON DELETE RESTRICT,
                              CONSTRAINT fk_appointment_service FOREIGN KEY (service_id) REFERENCES financial_services(id) ON DELETE RESTRICT,
                              CONSTRAINT chk_time_validity CHECK (start_time < end_time),
                              CONSTRAINT chk_appointment_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'))
);

-- Índices críticos para calcular disponibilidad y autenticar
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_appointments_search ON appointments(advisor_id, start_time, end_time) WHERE status = 'SCHEDULED';
CREATE INDEX idx_time_offs_date ON time_offs(off_date);
CREATE INDEX idx_appointments_client ON appointments(client_id);
CREATE INDEX idx_advisor_schedules_advisor ON advisor_schedules(advisor_id);