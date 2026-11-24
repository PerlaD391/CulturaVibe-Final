// Cliente HTTP del Proyecto Final CulturaVibe.
// La API Spring Boot corre en VITE_API_URL (por defecto http://localhost:8086).
// En desarrollo, Vite redirige /api al backend (ver vite.config.js), así
// que no hay problemas de CORS en local. En el VPS, el frontend y la API
// se sirven desde el mismo origen.
// El token JWT se guarda en localStorage y se envía como
// Authorization: Bearer <token> en cada petición protegida.
import axios from 'axios';

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8086';

export const AUTH_STORAGE_KEY = 'culturavibe-auth';

export const getStoredAuth = () => {
  try {
    return JSON.parse(localStorage.getItem(AUTH_STORAGE_KEY) || 'null');
  } catch {
    return null;
  }
};

export const storeAuth = (auth) => {
  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
};

export const clearStoredAuth = () => {
  localStorage.removeItem(AUTH_STORAGE_KEY);
};

// Decodifica el payload del JWT (sin verificar la firma: eso lo hace el
// servidor). Sirve para sacar el id del usuario cuando el backend lo pone
// en el token (sub o userId) y así poder llamar a /api/usuarios/{id}.
export const decodeToken = (token) => {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
};

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const auth = getStoredAuth();
  if (auth?.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

export default api;

// --- Errores del backend -------------------------------------------------
// El backend responde JSON {timestamp, status, error, message, path} y,
// para errores de validación (400), una lista de errores por campo.
// Estas funciones normalizan esas respuestas a algo que la UI puede
// mostrar bajo el campo correspondiente o como mensaje general.

const pickField = (item) => item?.field || item?.campo || item?.name || null;
const pickMessage = (item) => item?.message || item?.mensaje || item?.defaultMessage || String(item || '');

// Extrae { campo: mensaje } de una respuesta de error de validación.
// Soporta: {errors: [{field, message}]}, {errors: {campo: mensaje}},
// {fieldErrors: [...]}, {violations: [...]}.
export const extractFieldErrors = (error) => {
  const data = error?.response?.data;
  if (!data) return {};
  const raw = data.errors || data.fieldErrors || data.violations || null;
  if (Array.isArray(raw)) {
    const out = {};
    raw.forEach((item) => {
      const field = pickField(item);
      if (field) out[field] = pickMessage(item);
    });
    return out;
  }
  if (raw && typeof raw === 'object') {
    const out = {};
    Object.entries(raw).forEach(([key, value]) => {
      out[key] = typeof value === 'string' ? value : pickMessage(value);
    });
    return out;
  }
  return {};
};

export const isNetworkError = (error) => !error?.response;

export const apiErrorMessage = (error, fallback = 'Ocurrió un error. Inténtalo de nuevo.') => {
  if (isNetworkError(error)) {
    return 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.';
  }
  const data = error?.response?.data;
  const status = error?.response?.status;
  if (status === 401) return 'Correo o contraseña incorrectos.';
  if (status === 403) return 'No tienes permiso para hacer eso.';
  if (status === 404) return 'No se encontró lo que buscabas.';
  if (status === 409) return data?.message || 'Ya existe un registro con esos datos.';
  if (data?.message) return data.message;
  if (typeof data === 'string' && data) return data;
  return fallback;
};
