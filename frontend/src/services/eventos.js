// Eventos del Proyecto Final. El catálogo se sirve desde el backend
// (GET /api/eventos con paginación del servidor) — ya NO se usa
// Ticketmaster como fuente.
import api from './api';

export const ESTADOS_VENTA = {
  A_LA_VENTA: 'A la venta',
  AGOTADO: 'Agotado',
  CANCELADO: 'Cancelado',
  POSPUESTO: 'Pospuesto',
  FINALIZADO: 'Finalizado',
};

export const estadoVentaLabel = (estado) => ESTADOS_VENTA[estado] || estado || 'Por confirmar';

// GET /api/eventos?page=0&size=12&categoria=<id>&q=<texto>&fecha=<YYYY-MM-DD>
// Responde Page JSON {content, totalElements, totalPages, number, size}.
export const fetchEventos = async ({ page = 0, size = 12, categoria = '', q = '', fecha = '' } = {}) => {
  const params = { page, size };
  if (categoria) params.categoria = categoria;
  if (q) params.q = q;
  if (fecha) params.fecha = fecha;
  const { data } = await api.get('/api/eventos', { params });
  return {
    eventos: data.content || [],
    total: data.totalElements ?? 0,
    totalPages: data.totalPages ?? 1,
    page: data.number ?? page,
    size: data.size ?? size,
  };
};

export const fetchEventoById = async (id) => {
  const { data } = await api.get(`/api/eventos/${id}`);
  return data;
};

export const createEvento = async (evento) => {
  const { data } = await api.post('/api/eventos', evento);
  return data;
};

export const updateEvento = async (id, evento) => {
  const { data } = await api.put(`/api/eventos/${id}`, evento);
  return data;
};

export const deleteEvento = async (id) => {
  await api.delete(`/api/eventos/${id}`);
};
