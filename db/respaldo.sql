-- =====================================================================
-- CulturaVibe — Proyecto Final. Respaldo manual de la base de datos.
-- MySQL. Ejecutar con: mysql -u root -p < db/respaldo.sql
-- Crea la base culturavibe_final, las 8 tablas y los datos del seed.
--
-- Las contraseñas NO están en texto plano: son hashes BCrypt generados
-- con BCryptPasswordEncoder. Las contraseñas de prueba son las del
-- README del backend (backend/README.md):
--   admin@culturavibe.mx        / Admin123!        (ADMIN)
--   organizador@culturavibe.mx  / Organizador123!  (ORGANIZADOR)
--   cliente@culturavibe.mx      / Cliente123!      (CLIENTE)
--   maria@, carlos@             / Organizador123!  (ORGANIZADOR)
--   ana@, luis@, sofia@, diego@, valeria@, jorge@, camila@culturavibe.mx
--                               / Cliente123!      (CLIENTE)
-- Los datos del seed son de PRUEBA, no datos reales.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS culturavibe_final
  DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE culturavibe_final;

-- --- Tablas (las mismas de la migración Flyway V1__crear_tablas.sql) ---
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    telefono VARCHAR(20),
    rol_id BIGINT NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
);

CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS eventos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT,
    fecha DATE,
    hora TIME,
    direccion VARCHAR(255),
    ciudad VARCHAR(100),
    estado VARCHAR(100),
    categoria_id BIGINT,
    organizador_id BIGINT,
    precio DECIMAL(10, 2),
    estado_venta VARCHAR(30),
    url VARCHAR(500),
    imagen_url VARCHAR(500),
    artistas TEXT,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eventos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_eventos_organizador FOREIGN KEY (organizador_id) REFERENCES usuarios (id)
);

CREATE TABLE IF NOT EXISTS reservas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    evento_id BIGINT NOT NULL,
    fecha_reserva TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) DEFAULT 'CONFIRMADA',
    cantidad INT NOT NULL,
    CONSTRAINT fk_reservas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_reservas_evento FOREIGN KEY (evento_id) REFERENCES eventos (id)
);

CREATE TABLE IF NOT EXISTS tokens_revocados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiracion TIMESTAMP,
    revocado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tokens_recuperacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiracion TIMESTAMP NOT NULL,
    usado BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_tokens_recuperacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE IF NOT EXISTS notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT,
    tipo VARCHAR(20),
    mensaje TEXT,
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20),
    CONSTRAINT fk_notificaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

-- --- Datos del seed (3 roles, 12 usuarios, 5 categorías, 12 eventos, 12 reservas) ---
INSERT INTO roles (id, nombre, descripcion) VALUES
  (1, 'ADMIN', 'Administra usuarios, categorías y eventos.'),
  (2, 'ORGANIZADOR', 'Crea y gestiona sus propios eventos.'),
  (3, 'CLIENTE', 'Reserva boletos para eventos.')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO usuarios (id, nombre, email, password, telefono, rol_id, activo) VALUES
  (1, 'Admin CulturaVibe', 'admin@culturavibe.mx', '$2a$10$LEZcmPCDZ6AzlV/.bD6iM.7B2fSGocAIHX3n16TZzOekmV/ZpYQ3G', '+52 951 100 0001', 1, TRUE),
  (2, 'Ricardo Lemus Sánchez', 'organizador@culturavibe.mx', '$2a$10$VdYmOPP04G4HOKqJ3xw1B.BsRdfx3kp8JnbiXX6jQO.dQFkDVrXKi', '+52 951 100 0002', 2, TRUE),
  (3, 'Perla Danae Gallardo Vasquez', 'cliente@culturavibe.mx', '$2a$10$tuR/F8FKq9vrGi/crrDbM.jYhQ2Hcbv815ek1Q2xkIWtlYGNVZgCm', '+52 951 100 0003', 3, TRUE),
  (4, 'María Fernanda López', 'maria@culturavibe.mx', '$2a$10$VdYmOPP04G4HOKqJ3xw1B.BsRdfx3kp8JnbiXX6jQO.dQFkDVrXKi', '+52 951 100 0004', 2, TRUE),
  (5, 'Carlos Hernández Ruiz', 'carlos@culturavibe.mx', '$2a$10$VdYmOPP04G4HOKqJ3xw1B.BsRdfx3kp8JnbiXX6jQO.dQFkDVrXKi', '+52 951 100 0005', 2, TRUE),
  (6, 'Ana Paola Mendoza', 'ana@culturavibe.mx', '$2a$10$tuR/F8FKq9vrGi/crrDbM.jYhQ2Hcbv815ek1Q2xkIWtlYGNVZgCm', '+52 951 100 0006', 3, TRUE),
  (7, 'Luis Alberto Torres', 'luis@culturavibe.mx', '$2a$10$tuR/F8FKq9vrGi/crrDbM.jYhQ2Hcbv815ek1Q2xkIWtlYGNVZgCm', '+52 951 100 0007', 3, TRUE),
  (8, 'Sofía Ramírez Cruz', 'sofia@culturavibe.mx', '$2a$10$tuR/F8FKq9vrGi/crrDbM.jYhQ2Hcbv815ek1Q2xkIWtlYGNVZgCm', '+52 951 100 0008', 3, TRUE),
  (9, 'Diego Morales Vega', 'diego@culturavibe.mx', '$2a$10$tuR/F8FKq9vrGi/crrDbM.jYhQ2Hcbv815ek1Q2xkIWtlYGNVZgCm', '+52 951 100 0009', 3, TRUE),
  (10, 'Valeria Ortiz Sánchez', 'valeria@culturavibe.mx', '$2a$10$tuR/F8FKq9vrGi/crrDbM.jYhQ2Hcbv815ek1Q2xkIWtlYGNVZgCm', '+52 951 100 0010', 3, TRUE),
  (11, 'Jorge Castillo León', 'jorge@culturavibe.mx', '$2a$10$tuR/F8FKq9vrGi/crrDbM.jYhQ2Hcbv815ek1Q2xkIWtlYGNVZgCm', '+52 951 100 0011', 3, TRUE),
  (12, 'Camila Duarte Reyes', 'camila@culturavibe.mx', '$2a$10$tuR/F8FKq9vrGi/crrDbM.jYhQ2Hcbv815ek1Q2xkIWtlYGNVZgCm', '+52 951 100 0012', 3, TRUE)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO categorias (id, nombre, descripcion) VALUES
  (1, 'Música', 'Conciertos y festivales de música en vivo.'),
  (2, 'Teatro', 'Obras y puestas en escena.'),
  (3, 'Danza', 'Espectáculos de danza clásica y folclórica.'),
  (4, 'Arte', 'Exposiciones de arte y cultura visual.'),
  (5, 'Festivales', 'Festivales culturales y ferias.')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO eventos (id, titulo, descripcion, fecha, hora, direccion, ciudad, estado, categoria_id, organizador_id, precio, estado_venta, url, artistas) VALUES
  (1, 'Concierto Sinfónico de Gala', 'Gala sinfónica con obras de compositores mexicanos e invitados internacionales.', '2026-11-12', '20:00:00', 'Av. Juárez s/n, Centro Histórico', 'Ciudad de México', 'CDMX', 1, 2, 450.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Orquesta Sinfónica Nacional'),
  (2, 'Festival Internacional de Cine', 'Proyecciones de cine nacional e internacional con funciones al aire libre.', '2026-11-22', '18:00:00', 'Av. Periférico Norte 1695', 'Guadalajara', 'JAL', 5, 4, 180.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Cineastas nacionales e internacionales'),
  (3, 'Noche de Jazz en el Barrio Antiguo', 'Una noche de jazz en vivo con músicos locales y artistas invitados.', '2026-11-25', '21:00:00', 'Calle Morelos 300, Barrio Antiguo', 'Monterrey', 'NL', 1, 5, 250.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Jazz Quartet MX'),
  (4, 'Exposición de Arte Contemporáneo', 'Muestra de arte contemporáneo de creadores mexicanos.', '2026-11-28', '11:00:00', 'Av. 2 Oriente 607, Centro Histórico', 'Puebla', 'PUE', 4, 4, 0.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Artistas contemporáneos mexicanos'),
  (5, 'Feria del Libro', 'Venta y presentación de libros con autores invitados y talleres.', '2026-12-02', '10:00:00', 'Av. Morelos Sur 100, Centro', 'Cuernavaca', 'MOR', 5, 5, 0.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Autores invitados'),
  (6, 'Festival de Teatro Clásico', 'Puesta en escena de clásicos del teatro universal.', '2026-12-05', '19:00:00', 'Av. Costera Miguel Alemán 100', 'Acapulco', 'GRO', 2, 2, 320.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Compañía Nacional de Teatro'),
  (7, 'Concierto de Rock en Español', 'Las bandas más representativas del rock en español en una sola noche.', '2026-12-08', '20:30:00', 'Av. Paseo de la Reforma 50', 'Ciudad de México', 'CDMX', 1, 2, 850.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Caifanes, Café Tacvba'),
  (8, 'Obra: Don Juan Tenorio', 'La clásica obra de José Zorrilla en el teatro más emblemático de Oaxaca.', '2026-12-10', '19:00:00', 'Av. Independencia 900, Centro', 'Oaxaca de Juárez', 'OAX', 2, 4, 280.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Compañía Oaxaqueña de Teatro'),
  (9, 'Festival de Danza Folclórica', 'Bailes tradicionales de todas las regiones de México en un solo escenario.', '2026-12-12', '18:00:00', 'Donceles 36, Centro Histórico', 'Ciudad de México', 'CDMX', 3, 5, 200.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Ballet Folclórico de México'),
  (10, 'Noche de Mariachi en Garibaldi', 'Mariachi en vivo en la plaza más famosa de México, con serenata incluida.', '2026-12-14', '21:00:00', 'Plaza Garibaldi s/n, Centro', 'Ciudad de México', 'CDMX', 1, 2, 150.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Mariachi Vargas de Tecalitlán'),
  (11, 'Exposición: Frida Kahlo y el arte mexicano', 'Recorrido por la obra de Frida Kahlo y sus contemporáneos en la Casa Azul.', '2026-12-16', '10:00:00', 'Londres 247, Coyoacán', 'Ciudad de México', 'CDMX', 4, 4, 120.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Frida Kahlo'),
  (12, 'Festival Cultural de Invierno Oaxaca', 'Festival de cierre de año con música, gastronomía y artesanías oaxaqueñas.', '2026-12-18', '17:00:00', 'Cerro del Fortín s/n', 'Oaxaca de Juárez', 'OAX', 5, 5, 0.00, 'onsale', 'https://www.ticketmaster.com.mx', 'Artistas oaxaqueños')
ON DUPLICATE KEY UPDATE titulo = VALUES(titulo);

INSERT INTO reservas (id, usuario_id, evento_id, estado, cantidad) VALUES
  (1, 3, 1, 'CONFIRMADA', 2),
  (2, 3, 3, 'CONFIRMADA', 1),
  (3, 6, 2, 'CONFIRMADA', 4),
  (4, 7, 1, 'CANCELADA', 2),
  (5, 8, 6, 'CONFIRMADA', 3),
  (6, 9, 7, 'CONFIRMADA', 2),
  (7, 10, 8, 'CONFIRMADA', 1),
  (8, 11, 4, 'CONFIRMADA', 2),
  (9, 12, 9, 'CANCELADA', 2),
  (10, 6, 10, 'CONFIRMADA', 3),
  (11, 7, 12, 'CONFIRMADA', 4),
  (12, 3, 11, 'CONFIRMADA', 1)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);
