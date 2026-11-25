-- Sincronización con Ticketmaster: distingue los eventos importados
-- de los locales (semilla o creados en la app) y evita duplicados
-- por el id de Ticketmaster. SQL estándar compatible con MySQL y
-- con H2 en MODE=MySQL.

ALTER TABLE eventos
    ADD COLUMN fuente VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE eventos
    ADD COLUMN external_id VARCHAR(100) NULL;

CREATE UNIQUE INDEX uk_eventos_external_id ON eventos (external_id);
