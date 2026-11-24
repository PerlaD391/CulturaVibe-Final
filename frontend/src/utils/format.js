// Formato de fechas y precios para la UI (es-MX).

export const formatFecha = (fecha) => {
  if (!fecha) return 'Fecha por confirmar';
  const date = new Date(`${fecha}T00:00:00`);
  if (Number.isNaN(date.getTime())) return fecha;
  return date.toLocaleDateString('es-MX', { day: 'numeric', month: 'long', year: 'numeric' });
};

export const formatFechaCorta = (fecha) => {
  if (!fecha) return '—';
  const date = new Date(`${fecha}T00:00:00`);
  if (Number.isNaN(date.getTime())) return fecha;
  return date.toLocaleDateString('es-MX', { day: '2-digit', month: 'short', year: 'numeric' });
};

export const formatHora = (hora) => {
  if (!hora) return '';
  return String(hora).slice(0, 5);
};

export const formatPrecio = (precio) => {
  if (precio === null || precio === undefined || precio === '') return 'Consultar';
  const value = Number(precio);
  if (Number.isNaN(value)) return String(precio);
  if (value === 0) return 'Evento gratuito';
  return `$${value.toLocaleString('es-MX')} MXN`;
};

export const diasPara = (fecha) => {
  if (!fecha) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const date = new Date(`${fecha}T00:00:00`);
  if (Number.isNaN(date.getTime())) return null;
  return Math.round((date - today) / (1000 * 60 * 60 * 24));
};

// Iniciales para el avatar por defecto (sin foto).
export const iniciales = (nombre = '') => {
  const parts = String(nombre).trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'C';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
};
