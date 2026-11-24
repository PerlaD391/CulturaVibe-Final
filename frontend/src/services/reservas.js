// Reservas del Proyecto Final.
import api from './api';

export const ESTADOS_RESERVA = {
  CONFIRMADA: 'Confirmada',
  PENDIENTE: 'Pendiente',
  CANCELADA: 'Cancelada',
};

export const estadoReservaLabel = (estado) => ESTADOS_RESERVA[estado] || estado || '—';

export const createReserva = async ({ eventoId, cantidad }) => {
  const { data } = await api.post('/api/reservas', { eventoId, cantidad });
  return data;
};

// Mis reservas (del usuario en sesión). Responde Page JSON.
export const fetchMisReservas = async ({ page = 0, size = 12 } = {}) => {
  const { data } = await api.get('/api/reservas/mis', { params: { page, size } });
  return {
    reservas: data.content || [],
    total: data.totalElements ?? 0,
    totalPages: data.totalPages ?? 1,
    page: data.number ?? page,
  };
};

// Todas las reservas (ADMIN / ORGANIZADOR), con filtros opcionales.
export const fetchReservas = async ({ page = 0, size = 12, eventoId = '', estado = '' } = {}) => {
  const params = { page, size };
  if (eventoId) params.eventoId = eventoId;
  if (estado) params.estado = estado;
  const { data } = await api.get('/api/reservas', { params });
  return {
    reservas: data.content || [],
    total: data.totalElements ?? 0,
    totalPages: data.totalPages ?? 1,
    page: data.number ?? page,
  };
};

// Cancelar una reserva. El backend responde 200 con el DTO actualizado
// (estado CANCELADA).
export const cancelReserva = async (id) => {
  const { data } = await api.delete(`/api/reservas/${id}`);
  return data;
};
