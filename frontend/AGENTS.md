# CulturaVibe — Proyecto Final (frontend)

Sistema de eventos culturales en México (Programación Web, equipo 13,
maestra Adelina Martinez). React 19 + Vite 8 + Tailwind CSS v4 + React
Router 7 + Axios. Modo oscuro. Archivos `.jsx`
(`src/main.jsx`, `src/App.jsx`, `vite.config.js`).

## Qué hace

- Catálogo de eventos del **backend propio** (Spring Boot + MySQL,
  puerto 8086): GET /api/eventos con paginación del servidor, filtros
  por categoría, búsqueda (q) y fecha. Los eventos de Ticketmaster
  llegan al catálogo por sincronización al backend (el catálogo
  siempre se lee del backend, nunca directo de Ticketmaster). Si un
  evento trae `url`, se mantiene el enlace "Comprar boletos".
- Autenticación real con **JWT** contra /api/auth/** (login, registro,
  logout, forgot/reset password). Roles ADMIN, ORGANIZADOR y CLIENTE,
  con rutas protegidas por rol (/admin, /organizador). El token se
  guarda en localStorage y Axios lo envía como Bearer. Ya NO se usa
  Firebase ni DummyJSON para autenticación (el paquete firebase se
  desinstaló).
- Reservas: POST /api/reservas desde el detalle, "Mis reservas" con
  GET /api/reservas/mis y cancelación con modal de confirmación propio.
- Validaciones visibles DEBAJO de cada input (nunca alert ni
  confirm()): email válido, campos obligatorios y política de
  contraseña (mínimo 8 caracteres, una mayúscula, un número y un
  carácter especial). Los errores 400 del backend se muestran bajo el
  campo correspondiente.
- Navbar en toda la parte interna con nombre, correo, avatar por
  defecto (iniciales) y cerrar sesión. Logo original del equipo en
  `public/logo.svg`.

## Comandos

- `npm run dev` — desarrollo local (el proxy `/api` de
  `vite.config.js` redirige al backend en http://localhost:8086).
- `npm run build` — compila a `dist/` (Vite incrusta las variables
  `VITE_*` del `.env` en ese momento).
- `npm run preview` — sirve el build local.

## Variables de entorno (`.env`, nunca en el repo)

- `VITE_API_URL` — URL base de la API (por defecto
  http://localhost:8086; en el VPS es https://culturavibe.duckdns.org).

## Despliegue (VPS Contabo, Ubuntu)

Se compila en local y se sube el `dist` con
`scp -r .\dist\* root@13.140.226.55:/var/www/html/culturavibe/`.
La app se sirve en la raíz `https://culturavibe.duckdns.org/` (el `base` de
`vite.config.js` es `/`).

## Estructura

- `src/services/api.js` — instancia de Axios (baseURL, interceptor de
  token, normalización de errores del backend).
- `src/services/auth.js` — login/registro/logout/forgot/reset contra
  /api/auth/**.
- `src/services/eventos.js`, `categorias.js`, `reservas.js`,
  `usuarios.js` — llamadas a cada recurso de la API.
- `src/context/AuthContext.jsx` — sesión (token, nombre, email, rol)
  en localStorage.
- `src/utils/validation.js` — email y política de contraseña.
- `src/utils/format.js` — fechas, precios e iniciales del avatar.
- `src/components/` — Layout, EventCard, Icon, ConfirmModal,
  Spinner, FormField, EventoForm.
- `src/pages/` — Login, Register, ForgotPassword, ResetPassword,
  Catalog, EventDetail, Dashboard, MisReservas, Artists, Settings,
  Admin, Organizador.
- `public/logo.svg` — logo original del equipo.
