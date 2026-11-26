import { useEffect, useRef, useState } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';
import { fetchEventos } from '../services/eventos';
import { fetchCategorias } from '../services/categorias';
import { apiErrorMessage } from '../services/api';
import Icon from '../components/Icon';
import EventCard from '../components/EventCard';
import Spinner from '../components/Spinner';

const LIMIT_OPTIONS = [12, 24, 48];
const LIMIT_STORAGE_KEY = 'culturavibe-events-per-page';

const readStoredLimit = () => {
  const stored = Number(localStorage.getItem(LIMIT_STORAGE_KEY));
  return LIMIT_OPTIONS.includes(stored) ? stored : 12;
};

export default function Catalog() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  // Aviso de una sola vez (por ejemplo, tras eliminar la cuenta):
  // llega por navigate(..., { state: { mensaje } }).
  const [aviso, setAviso] = useState(location.state?.mensaje || '');
  const [eventos, setEventos] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchInput, setSearchInput] = useState(searchParams.get('q') || '');
  const searchTimer = useRef(null);

  // La API pagina en base 0; la URL muestra la página en base 1.
  const rawPage = Number(searchParams.get('page') || '1');
  const page = Number.isFinite(rawPage) && rawPage >= 1 ? Math.floor(rawPage) : 1;
  const rawLimit = Number(searchParams.get('limit') || '');
  const limit = LIMIT_OPTIONS.includes(rawLimit) ? rawLimit : readStoredLimit();
  const search = searchParams.get('q') || '';
  const categoria = searchParams.get('categoria') || '';
  const fecha = searchParams.get('fecha') || '';

  useEffect(() => {
    fetchCategorias().then(setCategorias).catch(() => setCategorias([]));
  }, []);

  // Si se entra al catálogo sin límite en la URL, se restaura el último
  // elegido (12, 24 o 48) para que la configuración se mantenga.
  useEffect(() => {
    if (!searchParams.get('limit')) {
      const stored = readStoredLimit();
      const next = new URLSearchParams(searchParams);
      next.set('limit', String(stored));
      setSearchParams(next, { replace: true });
    }
    // Solo al entrar a la página.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const updateParams = (key, value) => {
    const newParams = new URLSearchParams(searchParams);
    if (value) newParams.set(key, value);
    else newParams.delete(key);
    if (key !== 'page') newParams.set('page', '1');
    if (key === 'limit') localStorage.setItem(LIMIT_STORAGE_KEY, String(value));
    setSearchParams(newParams);
  };

  // La búsqueda espera 400 ms después de dejar de escribir para no pedir
  // a la API con cada tecla.
  useEffect(() => {
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => {
      if (searchInput !== search) updateParams('q', searchInput.trim());
    }, 400);
    return () => clearTimeout(searchTimer.current);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchInput]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    fetchEventos({ page: page - 1, size: limit, categoria, q: search, fecha })
      .then((data) => {
        if (cancelled) return;
        setEventos(data.eventos);
        setTotal(data.total);
        setTotalPages(Math.max(1, data.totalPages));
      })
      .catch((err) => {
        if (!cancelled) setError(apiErrorMessage(err, 'No se pudieron cargar los eventos.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page, limit, search, categoria, fecha]);

  // Si los filtros dejan menos páginas que la actual, se vuelve a la
  // última válida.
  useEffect(() => {
    if (!loading && total > 0 && page > totalPages) {
      updateParams('page', String(totalPages));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, total, totalPages]);

  return (
    <div className="mx-auto max-w-[1440px]">
      {aviso && (
        <div className="mb-6 flex items-center justify-between gap-4 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm font-medium text-emerald-300">
          <span>{aviso}</span>
          <button type="button" onClick={() => setAviso('')} aria-label="Cerrar aviso" className="text-emerald-300 hover:text-white">
            ✕
          </button>
        </div>
      )}
      {/* Filtros: se envían a la API (categoría, búsqueda q, fecha) */}
      <div className="mb-8 flex flex-col gap-4 rounded-2xl border border-white/5 bg-white/5 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex flex-wrap gap-3">
          <label className="flex w-full items-center gap-3 rounded-xl border border-white/10 bg-black/50 px-4 py-2.5 transition-colors focus-within:border-fuchsia-500 sm:max-w-[300px]">
            <Icon name="search" size={18} className="text-white" />
            <input
              type="text"
              placeholder="Buscar evento..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="w-full bg-transparent text-sm text-white outline-none placeholder:text-slate-500"
            />
          </label>
          <label className="relative flex items-center">
            <select
              value={categoria}
              onChange={(e) => updateParams('categoria', e.target.value)}
              className="appearance-none rounded-xl border border-white/10 bg-black/50 py-2.5 pl-4 pr-10 text-sm font-medium text-slate-300 outline-none focus:border-fuchsia-500"
            >
              <option value="">Todas las categorías</option>
              {categorias.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.nombre}
                </option>
              ))}
            </select>
            <Icon name="chevronDown" size={16} className="pointer-events-none absolute right-3 text-white" />
          </label>
          <label className="flex items-center">
            <input
              type="date"
              value={fecha}
              onChange={(e) => updateParams('fecha', e.target.value)}
              aria-label="Filtrar por fecha"
              className="rounded-xl border border-white/10 bg-black/50 px-4 py-2.5 text-sm font-medium text-slate-300 outline-none focus:border-fuchsia-500 [color-scheme:dark]"
            />
          </label>
        </div>
        <div className="text-sm text-slate-400">
          Mostrando {eventos.length} de {total.toLocaleString('es-MX')} eventos
        </div>
      </div>

      {loading ? (
        <Spinner texto="Cargando cartelera..." />
      ) : error ? (
        <div className="rounded-2xl border border-red-500/30 bg-red-500/10 px-4 py-6 text-center">
          <p className="text-sm font-medium text-red-300">{error}</p>
          <button
            onClick={() => updateParams('page', '1')}
            className="mt-4 rounded-xl bg-white/10 px-5 py-2.5 text-sm font-bold text-white transition hover:bg-white/20"
          >
            Intentar de nuevo
          </button>
        </div>
      ) : eventos.length === 0 ? (
        <p className="py-20 text-center text-slate-400">No hay eventos disponibles con estos filtros.</p>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {eventos.map((ev) => (
            <EventCard key={ev.id} event={ev} />
          ))}
        </div>
      )}

      {/* Paginación del servidor */}
      <div className="mt-10 flex flex-col items-center justify-between gap-4 border-t border-white/10 pt-6 sm:flex-row">
        <div className="flex items-center gap-2">
          <span className="text-sm text-slate-400">Mostrar:</span>
          <select
            value={limit}
            onChange={(e) => updateParams('limit', e.target.value)}
            className="rounded-lg border border-white/10 bg-black/50 px-3 py-1.5 text-sm text-white outline-none"
          >
            {LIMIT_OPTIONS.map((n) => (
              <option key={n} value={n}>
                {n} eventos
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <button
            disabled={page === 1}
            onClick={() => updateParams('page', String(page - 1))}
            className="rounded-lg bg-white/5 px-4 py-2 text-sm text-white transition hover:bg-white/10 disabled:opacity-50"
          >
            Anterior
          </button>
          <span className="px-4 text-sm font-medium text-slate-400">
            Página {Math.min(page, totalPages)} de {totalPages}
          </span>
          <button
            disabled={page >= totalPages}
            onClick={() => updateParams('page', String(page + 1))}
            className="rounded-lg bg-white/5 px-4 py-2 text-sm text-white transition hover:bg-white/10 disabled:opacity-50"
          >
            Siguiente
          </button>
        </div>
      </div>
    </div>
  );
}
