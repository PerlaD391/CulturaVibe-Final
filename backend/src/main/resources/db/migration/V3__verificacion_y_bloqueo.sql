-- Verificación de correo/SMS con código y bloqueo por intentos fallidos.
-- SQL estándar compatible con MySQL y con H2 en MODE=MySQL.
-- El esquema lo crea Flyway; Hibernate solo valida (ddl-auto=validate).

-- Bloqueo: contador de intentos fallidos de login por usuario.
ALTER TABLE usuarios
    ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0;

-- Verificación de contacto: el login NO se bloquea por no estar
-- verificado (para no romper la demo ni a los usuarios de Gmail,
-- a los que el correo de bienvenida les rebota).
ALTER TABLE usuarios
    ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE usuarios
    ADD COLUMN telefono_verificado BOOLEAN NOT NULL DEFAULT FALSE;

-- Las cuentas semilla (@culturavibe.mx) ya existen en vivo y quedan
-- verificadas. En una base nueva esta sentencia no afecta a nadie y
-- el seed (DataInitializer) marca las cuentas semilla al crearlas.
UPDATE usuarios SET email_verificado = TRUE WHERE email LIKE '%@culturavibe.mx';

-- Códigos de verificación de 6 dígitos, válidos 15 minutos, de un
-- solo uso. Al generar uno nuevo, los anteriores se marcan usados.
CREATE TABLE IF NOT EXISTS codigos_verificacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    codigo VARCHAR(10) NOT NULL,
    expiracion TIMESTAMP NOT NULL,
    usado BOOLEAN DEFAULT FALSE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_codigos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);
