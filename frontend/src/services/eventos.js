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

// El estado de venta se guarda con tres vocabularios distintos:
// "onsale" (semilla y valor por defecto del backend), "En venta" /
// "Agotado" / "Cancelado" (sincronización de Ticketmaster) y
// "A_LA_VENTA" / "AGOTADO" / ... (formulario de evento). Aquí se
// normalizan para que el catálogo, el detalle y la reserva decidan igual.
const ESTADO_VENTA_NORMALIZADO = {
  A_LA_VENTA: 'A_LA_VENTA',
  ONSALE: 'A_LA_VENTA',
  'EN VENTA': 'A_LA_VENTA',
  AGOTADO: 'AGOTADO',
  OFFSALE: 'AGOTADO',
  CANCELADO: 'CANCELADO',
  CANCELLED: 'CANCELADO',
  POSPUESTO: 'POSPUESTO',
  POSTPONED: 'POSPUESTO',
  FINALIZADO: 'FINALIZADO',
};

export const normalizarEstadoVenta = (estado) => {
  if (!estado) return 'A_LA_VENTA';
  return ESTADO_VENTA_NORMALIZADO[String(estado).trim().toUpperCase()] || null;
};

export const estaALaVenta = (estado) => normalizarEstadoVenta(estado) === 'A_LA_VENTA';

export const estadoVentaLabel = (estado) => {
  const normalizado = normalizarEstadoVenta(estado);
  if (normalizado) return ESTADOS_VENTA[normalizado];
  return estado || 'Por confirmar';
};

// Mis eventos (ORGANIZADOR): GET /api/eventos/mis, paginado en el servidor.
export const fetchMisEventos = async ({ page = 0, size = 12 } = {}) => {
  const { data } = await api.get('/api/eventos/mis', { params: { page, size } });
  return {
    eventos: data.content || [],
    total: data.totalElements ?? 0,
    totalPages: data.totalPages ?? 1,
    page: data.number ?? page,
    size: data.size ?? size,
  };
};

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
