import { useNavigate } from 'react-router-dom';
import Icon from './Icon';
import { formatFechaCorta, formatPrecio } from '../utils/format';
import { estadoVentaLabel, estaALaVenta } from '../services/eventos';

// Tarjeta del catálogo. El evento ya viene del backend con el DTO del
// Proyecto Final: {id, titulo, descripcion, fecha, hora, direccion,
// ciudad, estado, categoriaId, categoriaNombre, precio, estadoVenta,
// url, imagenUrl, artistas, organizadorId}.
export default function EventCard({ event }) {
  const navigate = useNavigate();

  const imageUrl =
    event.imagenUrl ||
    'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&q=80';
  const title = event.titulo || 'Evento sin título';
  const date = formatFechaCorta(event.fecha);
  const place = [event.direccion, event.ciudad, event.estado].filter(Boolean).join(', ') || 'Lugar por confirmar';
  const category = event.categoriaNombre || 'Cultura';
  const price = formatPrecio(event.precio);
  const aLaVenta = estaALaVenta(event.estadoVenta);

  return (
    <div
      onClick={() => navigate(`/eventos/${event.id}`)}
      className="group relative flex cursor-pointer flex-col overflow-hidden rounded-2xl border border-white/5 bg-[#0a0a0a] transition-all hover:-translate-y-1 hover:border-fuchsia-500/50 hover:shadow-[0_0_20px_rgba(217,70,239,0.15)]"
    >
      <div className="relative h-48 w-full overflow-hidden">
        <div className="absolute inset-0 z-10 bg-gradient-to-t from-[#0a0a0a] via-transparent to-transparent" />
        <img
          src={imageUrl}
          alt={title}
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
        />
        <span className="absolute right-3 top-3 z-20 flex items-center gap-1 rounded-full border border-fuchsia-500/30 bg-black/60 px-3 py-1 text-[10px] font-bold text-fuchsia-300 shadow-lg backdrop-blur-md">
          <Icon name="star" size={12} /> {category}
        </span>
        {!aLaVenta && (
          <span className="absolute left-3 top-3 z-20 rounded-full border border-amber-500/40 bg-black/60 px-3 py-1 text-[10px] font-bold text-amber-300 backdrop-blur-md">
            {estadoVentaLabel(event.estadoVenta)}
          </span>
        )}
      </div>

      <div className="z-20 flex flex-1 flex-col p-5">
        <h3 className="mb-2 line-clamp-2 text-lg font-bold text-white transition-colors group-hover:text-fuchsia-300">
          {title}
        </h3>

        <div className="mt-auto space-y-2 text-sm text-slate-400">
          <p className="flex items-center gap-2">
            <Icon name="calendar" size={16} /> {date}
            {event.hora ? ` · ${String(event.hora).slice(0, 5)}` : ''}
          </p>
          <p className="flex items-center gap-2">
            <Icon name="map-pin" size={16} /> {place}
          </p>
          <p className="flex items-center gap-2 font-medium text-emerald-400">
            <Icon name="briefcase" size={16} /> {price}
          </p>
        </div>

        <div className="mt-5 w-full rounded-xl bg-white/5 py-2 text-center text-xs font-semibold text-white transition group-hover:bg-fuchsia-600">
          Ver detalles y reservar
        </div>
      </div>
    </div>
  );
}
