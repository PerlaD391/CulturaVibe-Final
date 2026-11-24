# CulturaVibe — Proyecto Final (Backend)

API REST con Spring Boot 3.5.16 + Java 17, MySQL, Flyway, Spring Security + JWT,
correo (Postfix) y notificaciones SMS (Vonage o Twilio) y WhatsApp (Meta WhatsApp Cloud API).

- **Equipo 13:** Perla Danae Gallardo Vasquez (GVPD) y Ricardo Lemus Sánchez (LSR)
- **Maestra:** Adelina Martinez — **Entrega:** lunes 24 de noviembre de 2026, 23:59
- **Puerto:** 8086 (Act1 8081, Act2 8083, Act3 8084, Act4 8085)

## Cómo correrlo

### En Windows (máquina de Rin, con red para Maven)

```powershell
.\mvnw.cmd spring-boot:run
```

Variables de entorno necesarias (ver `src/main/resources/application.properties.example`):

| Variable | Descripción |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | MySQL (base `culturavibe_final`, se crea sola) |
| `JWT_SECRET` | **Obligatorio.** Secreto JWT en Base64URL de 32 bytes. Generar con: `openssl rand -base64 32 | tr '+/' '-_' | tr -d '='` |
| `MAIL_FROM`, `APP_FRONTEND_URL` | Correo y URL del frontend para el enlace de recuperación |
| `VONAGE_API_KEY`, `VONAGE_API_SECRET`, `VONAGE_FROM_NUMBER` | Vonage (SMS, va primero; prueba gratis con número virtual); si están vacías, se usa Twilio |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_PHONE_NUMBER` | Twilio (solo SMS, respaldo si Vonage no está configurado); ojo: en trial pide upgrade para comprar número |
| `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_TEMPLATE_NAME` | WhatsApp Cloud API (Meta, gratis; plantilla por defecto `hello_world`); si están vacías, el WhatsApp se omite |
| `CORS_ALLOWED_ORIGINS` | Orígenes separados por comas |

### Pruebas locales sin MySQL (perfil `test`, H2 en memoria)

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=test
```

## Base de datos

- El esquema lo crea **Flyway** (`src/main/resources/db/migration/V1__crear_tablas.sql`).
  Hibernate solo valida (`ddl-auto=validate`, nunca `create`).
- 8 tablas: `roles`, `usuarios`, `categorias`, `eventos`, `reservas`,
  `tokens_revocados`, `tokens_recuperacion`, `notificaciones`.
- Respaldo manual: `../db/respaldo.sql` (CREATE TABLE + INSERTs con los datos del seed).
- El seed (`DataInitializer`) es **de prueba** y es idempotente: si ya hay roles, no siembra.

## Usuarios de prueba (seed)

Todos cumplen la política de contraseña (mín. 8 caracteres, una mayúscula,
un número y un carácter especial):

| Correo | Contraseña | Rol |
|---|---|---|
| `admin@culturavibe.mx` | `Admin123!` | ADMIN |
| `organizador@culturavibe.mx` | `Organizador123!` | ORGANIZADOR (Ricardo Lemus Sánchez) |
| `cliente@culturavibe.mx` | `Cliente123!` | CLIENTE (Perla Danae Gallardo Vasquez) |
| `maria@culturavibe.mx`, `carlos@culturavibe.mx` | `Organizador123!` | ORGANIZADOR |
| `ana@`, `luis@`, `sofia@`, `diego@`, `valeria@`, `jorge@`, `camila@culturavibe.mx` | `Cliente123!` | CLIENTE |

El seed también crea 5 categorías, 12 eventos (6 de la Act3 + 6 nuevos de
diciembre 2026) y 12 reservas (algunas CANCELADA).

## Endpoints

Públicos: `POST /api/auth/register`, `POST /api/auth/login`,
`POST /api/auth/forgot-password`, `POST /api/auth/reset-password`,
`GET /api/eventos` (filtros `categoria`, `q`, `fecha`, paginación),
`GET /api/eventos/{id}`, `GET /api/categorias`, `GET /api/categorias/{id}`.

Autenticados (Bearer JWT):

| Método | Ruta | Rol |
|---|---|---|
| POST | `/api/auth/logout` | cualquiera (revoca el token) |
| POST/PUT/DELETE | `/api/eventos` | ADMIN u ORGANIZADOR (dueño del evento) |
| POST/PUT/DELETE | `/api/categorias` | ADMIN |
| POST | `/api/reservas` | CLIENTE, ADMIN u ORGANIZADOR |
| GET | `/api/reservas/mis` | cualquiera autenticado |
| GET | `/api/reservas` (filtros `eventoId`, `estado`) | ADMIN / ORGANIZADOR (solo sus eventos) |
| DELETE | `/api/reservas/{id}` | dueño, ADMIN u ORGANIZADOR del evento (cancela) |
| GET/PUT/DELETE | `/api/usuarios` | ADMIN (GET `/{id}` también el propio usuario) |

Los errores salen en JSON: `{timestamp, status, error, message, path}`
y en validación además `errores` con un mensaje por campo.

## Despliegue en el VPS (13.140.226.55)

```bash
# En la máquina de Rin (PowerShell), compilar y subir:
.\mvnw.cmd clean package -DskipTests
scp target\culturavibe-final-0.0.1-SNAPSHOT.jar root@13.140.226.55:/opt/springboot-final/
# En el VPS (bash), con las variables de entorno en el mismo comando:
mkdir -p /opt/springboot-final
JWT_SECRET=... DB_PASSWORD=... nohup java -jar /opt/springboot-final/culturavibe-final-0.0.1-SNAPSHOT.jar > /opt/springboot-final/final.log 2>&1 &
# Abrir el puerto:
ufw allow 8086
```
