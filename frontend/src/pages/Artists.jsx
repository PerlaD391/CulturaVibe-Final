import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchEventos } from '../services/eventos';
import { apiErrorMessage } from '../services/api';
import Icon from '../components/Icon';
import Spinner from '../components/Spinner';

// Artistas de la cartelera: se juntan los artistas que trae cada evento
// del backend (campo `artistas` del DTO), se quitan duplicados y se
// ordenan por cantidad de eventos próximos.
export default function Artists() {
  const navigate = useNavigate();
  const [artists, setArtists] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');

  useEffect(() => {
    let cancelled = false;
    fetchEventos({ page: 0, size: 100 })
      .then(({ eventos }) => {
        if (cancelled) return;
        const byName = new Map();
        (eventos || []).forEach((event) => {
          const names = Array.isArray(event.artistas)
            ? event.artistas.map((a) => (typeof a === 'string' ? a : a?.nombre)).filter(Boolean)
            : typeof event.artistas === 'string'
              ? event.artistas.split(',').map((a) => a.trim()).filter(Boolean)
              : [];
          names.forEach((name) => {
            const existing = byName.get(name) || { name, events: [] };
            existing.events.push({ id: event.id, titulo: event.titulo, fecha: event.fecha });
            byName.set(name, existing);
          });
        });
        const list = [...byName.values()]
          .map((artist) => ({
            ...artist,
            eventCount: artist.events.length,
            nextDate: artist.events.map((e) => e.fecha).filter(Boolean).sort()[0] || '',
          }))
          .sort((a, b) => b.eventCount - a.eventCount || a.name.localeCompare(b.name));
        setArtists(list);
      })
      .catch((err) => {
        if (!cancelled) setError(apiErrorMessage(err, 'No se pudieron cargar los artistas.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const filtered = artists.filter((artist) => artist.name.toLowerCase().includes(query.trim().toLowerCase()));

  return (
    <div className="mx-auto max-w-[1440px]">
      <div className="mb-8">
        <p className="mb-1 text-sm font-bold uppercase tracking-widest text-fuchsia-500">Artistas</p>
        <h2 className="text-3xl font-extrabold text-white">Artistas de la cartelera</h2>
        <p className="mt-2 text-slate-400">Quienes se presentan en los próximos eventos. Toca “Ver sus eventos” para ver su cartelera.</p>
      </div>

      <div className="mb-8 flex items-center gap-3 rounded-xl border border-white/10 bg-black/40 px-4 py-3 transition-all focus-within:border-violet-500">
        <Icon name="search" size={19} className="text-slate-500" />
        <input
          type="text"
          placeholder="Buscar artista..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="w-full bg-transparent text-sm text-white outline-none placeholder:text-slate-600"
        />
      </div>

      {loading ? (
        <Spinner texto="Cargando artistas..." />
      ) : error ? (
        <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-center text-sm text-red-300">{error}</p>
      ) : filtered.length === 0 ? (
        <p className="py-20 text-center text-slate-400">No hay artistas con ese nombre en la cartelera actual.</p>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {filtered.map((artist) => (
            <div
              key={artist.name}
              className="group overflow-hidden rounded-3xl border border-white/5 bg-white/5 backdrop-blur-md transition-all hover:-translate-y-1 hover:bg-white/10"
            >
              <div className="relative h-48 w-full overflow-hidden">
                <div className="absolute inset-0 z-10 bg-gradient-to-t from-[#0a0a0a] to-transparent" />
                <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-violet-600/40 to-fuchsia-600/40">
                  <Icon name="users" size={48} className="text-white/60" />
                </div>
                <span className="absolute right-3 top-3 z-20 flex items-center gap-1 rounded-full border border-white/10 bg-fuchsia-600/80 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-white shadow-lg backdrop-blur-md">
                  <Icon name="star" size={12} /> Artista
                </span>
              </div>
              <div className="relative z-20 p-5">
                <h3 className="mb-1 text-lg font-bold text-white">{artist.name}</h3>
                <p className="mb-3 text-sm font-medium text-fuchsia-400">Artista</p>
                <p className="mb-4 text-xs text-slate-400">
                  {artist.eventCount} {artist.eventCount === 1 ? 'evento próximo' : 'eventos próximos'}
                  {artist.nextDate ? ` · Próximo: ${artist.nextDate}` : ''}
                </p>
                <button
                  onClick={() => navigate(`/?q=${encodeURIComponent(artist.name)}`)}
                  className="w-full rounded-xl border border-white/10 bg-white/10 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-fuchsia-600/80"
                >
                  Ver sus eventos
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
