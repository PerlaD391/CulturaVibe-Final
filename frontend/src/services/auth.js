// Autenticación real contra la API del Proyecto Final (/api/auth/**).
// El backend devuelve JWT: {token, tipo: "Bearer", email, nombre, rol}.
// La sesión se guarda en localStorage (clave culturavibe-auth) y el
// interceptor de Axios la envía como Authorization: Bearer en cada
// petición protegida. Ya NO se usa Firebase ni DummyJSON.
import api, { storeAuth, clearStoredAuth, decodeToken } from './api';

export const ROLES = {
  ADMIN: 'ADMIN',
  ORGANIZADOR: 'ORGANIZADOR',
  CLIENTE: 'CLIENTE',
};

export const ROLE_LABELS = {
  ADMIN: 'Administrador',
  ORGANIZADOR: 'Organizador',
  CLIENTE: 'Cliente',
};

export const roleLabel = (rol) => ROLE_LABELS[rol] || rol || 'Cliente';

// Normaliza la respuesta de login/registro a {token, nombre, email, rol, id?}.
const normalizeAuth = (data) => {
  const payload = data?.token ? decodeToken(data.token) : null;
  const id = payload?.id ?? payload?.userId ?? payload?.sub ?? null;
  return {
    token: data.token,
    nombre: data.nombre || data.name || '',
    email: data.email || '',
    rol: data.rol || data.role || 'CLIENTE',
    id: typeof id === 'number' || (typeof id === 'string' && /^\d+$/.test(id)) ? Number(id) : null,
  };
};

export const login = async ({ email, password }) => {
  const { data } = await api.post('/api/auth/login', { email, password });
  const auth = normalizeAuth(data);
  storeAuth(auth);
  return auth;
};

export const register = async ({ nombre, email, password, telefono, rol }) => {
  const { data } = await api.post('/api/auth/register', {
    nombre,
    email,
    password,
    ...(telefono ? { telefono } : {}),
    ...(rol ? { rol } : {}),
  });
  const auth = normalizeAuth(data);
  storeAuth(auth);
  return auth;
};

export const logout = async () => {
  try {
    await api.post('/api/auth/logout');
  } catch {
    // Aunque el servidor falle, la sesión local se limpia igual.
  }
  clearStoredAuth();
};

export const forgotPassword = async (email) => {
  const { data } = await api.post('/api/auth/forgot-password', { email });
  return data?.mensaje || data?.message || 'Si ese correo existe, recibirás las instrucciones para recuperar tu contraseña.';
};

export const resetPassword = async ({ token, nuevaPassword }) => {
  const { data } = await api.post('/api/auth/reset-password', { token, nuevaPassword });
  return data?.mensaje || data?.message || 'Contraseña actualizada. Ya puedes iniciar sesión.';
};
