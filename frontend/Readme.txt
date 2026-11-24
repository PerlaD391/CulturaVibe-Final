
==============================
CulturaVibe — Proyecto Final (full stack)
==============================

**Materia:** Programación Web
**Maestra:** Adelina Martinez
**Integrantes:**
- Gallardo Vasquez Perla Danae (GVPD)
- Lemus Sánchez Ricardo (LSR)
Equipo: 13
**Entrega:** lunes 24 de noviembre de 2026, 23:59, con demo en vivo.

## Descripción del Proyecto
CulturaVibe es un sistema de eventos culturales en México construido con React y Vite que consume la API propia del Proyecto Final (Spring Boot + MySQL, puerto 8086). Muestra la cartelera del backend con búsqueda, filtros por categoría y por fecha, y paginación del servidor reflejada en la URL. Los usuarios crean una cuenta, inician sesión con JWT, reservan boletos, ven sus reservas y las cancelan. Los organizadores gestionan sus eventos y las reservas; el administrador gestiona usuarios, eventos, categorías y reservas. Todo con un diseño moderno en modo oscuro (Dark Mode).

IMPORTANTE: este frontend final YA NO usa Ticketmaster ni Firebase para autenticación. El catálogo viene de la API propia (/api/eventos) y el login/registro es contra /api/auth/** con JWT. Si un evento trae `url`, se mantiene el enlace "Comprar boletos".

## Enlace del Proyecto Desplegado
El sistema se sirve en la raíz del VPS:

**http://13.140.226.55/** (frontend) — API en **http://13.140.226.55:8086**

## Credenciales de Prueba
- **Admin:** `admin@culturavibe.mx` / `Admin123!`
- **Organizador:** `organizador@culturavibe.mx` / `Organizador123!`
- **Cliente:** `cliente@culturavibe.mx` / `Cliente123!`

## Funcionalidades
- **Catálogo de eventos (público, `/`):** eventos del backend con paginación del servidor (12, 24 o 48 por página, se guarda la elección), búsqueda con pausa al escribir (q), filtro por categoría y por fecha. La paginación y los filtros van en la URL.
- **Detalle del evento (público, `/eventos/:id`):** fecha y hora, sede con ciudad y estado, precio, artistas, estado de venta, enlace "Comprar boletos" si el evento trae url, compartir por WhatsApp o copiando el enlace, y reserva de boletos si hay sesión.
- **Mis reservas (`/mis-reservas`):** reservas del usuario desde GET /api/reservas/mis, con cancelación (modal de confirmación propio).
- **Artistas (`/artistas`):** artistas que traen los eventos del backend, con buscador.
- **Registro y login con JWT:** POST /api/auth/register y /api/auth/login. La contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un carácter especial (se valida en tiempo real bajo el campo). Recuperación de contraseña por correo (/recuperar y /restablecer).
- **Roles:** ADMIN, ORGANIZADOR y CLIENTE. Rutas protegidas por rol: /admin (ADMIN: usuarios, eventos, categorías, reservas) y /organizador (ORGANIZADOR: sus eventos y sus reservas). Sin sesión se redirige a /login; sin el rol, al inicio.
- **Dashboard (`/dashboard`):** total real de eventos, categorías, reservas próximas (7 días) y próximo evento.
- **Configuración (`/settings`):** perfil del backend (nombre y teléfono), cambio de contraseña por correo y preferencias de notificaciones (se guardan en el navegador).
- **Validaciones y errores visibles:** todos los mensajes de error aparecen debajo del campo correspondiente (nunca alert ni confirm nativos). Los errores de red se muestran como "No se pudo conectar con el servidor".
- **Navbar:** visible en toda la parte interna con nombre, correo, avatar por defecto (iniciales) y cerrar sesión.

## API que consume (Proyecto Final, puerto 8086)
- **Auth:** POST /api/auth/register, /api/auth/login, /api/auth/logout, /api/auth/forgot-password, /api/auth/reset-password.
- **Eventos:** GET /api/eventos (público, paginado), GET /api/eventos/{id}, POST/PUT/DELETE (ADMIN u ORGANIZADOR).
- **Categorías:** GET /api/categorias (público), POST/PUT/DELETE (ADMIN).
- **Reservas:** POST /api/reservas, GET /api/reservas/mis, GET /api/reservas (ADMIN/ORGANIZADOR), DELETE /api/reservas/{id}.
- **Usuarios:** GET/PUT/DELETE /api/usuarios (ADMIN).
- El token JWT se guarda en localStorage y se envía como `Authorization: Bearer <token>` en cada petición protegida (interceptor de Axios en `src/services/api.js`).

## Configuración local
```bash
npm install
npm run dev
```

Copia `.env.example` a `.env`. El archivo `.env` nunca se sube al repositorio.

## Variables de entorno (.env)
```
VITE_API_URL=http://localhost:8086
```

En desarrollo, Vite redirige `/api` al backend local (ver `vite.config.js`), así que no hay problemas de CORS.

## Despliegue en el VPS
```bash
npm run build
scp -r .\dist\* root@13.140.226.55:/var/www/html/
```

El build debe hacerse con el `.env` presente (las variables `VITE_*` se incrustan al compilar). El frontend se sirve en la raíz (`base: '/'` en `vite.config.js`).

## Tecnologías y Buenas Prácticas
- **React 19 + Vite 8** para el frontend.
- **React Router DOM 7** para la navegación y protección de rutas por rol.
- **Tailwind CSS v4** para los estilos visuales (modo oscuro).
- **Axios** para las llamadas a la API, con interceptor de token JWT.
- Separación de responsabilidades en carpetas: `/components`, `/pages`, `/services`, `/context`, `/utils`.
- Llamadas a la API centralizadas en `src/services/` (api.js, auth.js, eventos.js, categorias.js, reservas.js, usuarios.js).
- Manejo de errores visible para el usuario y validación de campos debajo de cada input.
- Logo original del equipo en `public/logo.svg` (C estilizada con ondas de sonido), usado en el navbar y en login/registro.
