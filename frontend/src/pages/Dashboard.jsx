import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchEventos } from '../services/eventos';
import { fetchCategorias } from '../services/categorias';
import { fetchMisReservas } from '../services/reservas';
import { useAuth } from '../context/AuthContext';
import { diasPara, formatFechaCorta } from '../utils/format';
import Icon from '../components/Icon';
import Spinner from '../components/Spinner';

// Panel principal: próximos eventos del backend, total de eventos y
// categorías reales, y recordatorios de las reservas del usuario para
// los próximos 7 días.
export default function Dashboard() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [previewEvents, setPreviewEvents] = useState([]);
  const [totalEvents, setTotalEvents] = useState(null);
  const [categoriasCount, setCategoriasCount] = useState(null);
  const [loadingPreview, setLoadingPreview] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [reminders, setReminders] = useState([]);

  useEffect(() => {
    fetchEventos({ page: 0, size: 4 })
      .then((data) => {
        setPreviewEvents(data.eventos || []);
        setTotalEvents(data.total);
      })
      .catch(() => setLoadError('No se pudo conectar con el servidor.'))
      .finally(() => setLoadingPreview(false));
    fetchCategorias()
      .then((cats) => setCategoriasCount(cats.length))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!isAuthenticated) return;
    fetchMisReservas({ page: 0, size: 50 })
      .then(({ reservas }) => {
        const upcoming = (reservas || [])
          .filter((r) => r.estado !== 'CANCELADA')
          .map((r) => ({ ...r, daysLeft: diasPara(r.fechaEvento || r.eventoFecha) }))
          .filter((r) => r.daysLeft !== null && r.daysLeft >= 0 && r.daysLeft <= 7)
          .sort((a, b) => a.daysLeft - b.daysLeft);
        setReminders(upcoming);
      })
      .catch(() => {});
  }, [isAuthenticated]);

  const nextEvent = previewEvents[0] || null;
  const nextEventDays = nextEvent ? diasPara(nextEvent.fecha) : null;
  const nextEventName = nextEvent?.titulo || '';
  const shortNextName = nextEventName.length > 32 ? `${nextEventName.slice(0, 32)}…` : nextEventName;
  const nextEventNote = !nextEvent
    ? 'Sin eventos próximos'
    : nextEventDays === 0
      ? `Hoy: ${shortNextName}`
      : `días para ${shortNextName}`;
  const nextEventValue = nextEventDays === null ? '—' : nextEventDays === 0 ? 'Hoy' : String(nextEventDays);

  return (
    <div className="mx-auto max-w-[1440px]">
      <div
        className="relative mb-8 overflow-hidden rounded-3xl border border-white/10 bg-slate-900 shadow-[0_0_50px_rgba(217,70,239,0.15)]"
        style={{
          backgroundImage:
            'url("https://images.unsplash.com/photo-1518659526054-190340b32735?q=80&w=2070&auto=format&fit=crop")',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
        }}
      >
        <div className="absolute inset-0 bg-gradient-to-t from-[#050505] via-[#050505]/70 to-transparent" />
        <div className="absolute inset-0 bg-fuchsia-900/20 mix-blend-overlay" />
        <div className="relative z-10 flex h-[400px] flex-col justify-end p-8 sm:p-12">
          <p className="mb-2 flex items-center gap-2 text-sm font-bold uppercase tracking-widest text-[#f0abfc] drop-shadow-md">
            <Icon name="map-pin" size={16} /> Eventos en México
          </p>
          <h2 className="max-w-3xl text-4xl font-extrabold leading-tight tracking-[-.02em] text-white drop-shadow-xl sm:text-5xl lg:text-6xl">
            Vive la cultura en todo el país
          </h2>
          <p className="mt-4 max-w-2xl text-lg text-slate-300">
            Descubre conciertos, obras de teatro, danza y presentaciones de libros en las principales ciudades de la República.
          </p>
          <button
            onClick={() => navigate('/')}
            className="mt-8 flex w-fit items-center gap-2 rounded-xl bg-gradient-to-r from-fuchsia-600 to-violet-600 px-6 py-3.5 text-sm font-bold text-white shadow-[0_0_20px_rgba(217,70,239,0.5)] transition hover:opacity-90"
          >
            Ver Cartelera Nacional <Icon name="chevronRight" size={18} />
          </button>
        </div>
      </div>

      <section className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          { title: 'Eventos Activos', value: totalEvents !== null ? totalEvents.toLocaleString('es-MX') : '…', note: 'En todo México', icon: 'calendar', tone: 'bg-fuchsia-500/20 text-fuchsia-400', target: '/' },
          { title: 'Categorías', value: categoriasCount !== null ? String(categoriasCount) : '…', note: 'De la cartelera', icon: 'palette', tone: 'bg-violet-500/20 text-violet-400', target: '/' },
          { title: 'Mis Reservas', value: reminders.length ? String(reminders.length) : '0', note: 'Próximos 7 días', icon: 'ticket', tone: 'bg-pink-500/20 text-pink-400', target: '/mis-reservas' },
          { title: 'Próximo Evento', value: nextEventValue, note: nextEventNote, icon: 'star', tone: 'bg-emerald-500/20 text-emerald-400', target: '/' },
        ].map((item, i) => (
          <article
            key={i}
            onClick={() => navigate(item.target)}
            className="group cursor-pointer rounded-3xl border border-white/5 bg-white/5 p-6 backdrop-blur-md transition-all hover:-translate-y-1 hover:border-white/20 hover:bg-white/10"
          >
            <div className={`mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl ${item.tone}`}>
              <Icon name={item.icon} size={24} />
            </div>
            <div>
              <p className="text-sm font-medium text-slate-400">{item.title}</p>
              <p className="mt-1 text-[36px] font-extrabold tracking-[-.04em] text-white">{item.value}</p>
              <p className="mt-1 text-[12px] font-medium text-slate-500 group-hover:text-slate-400">{item.note}</p>
            </div>
          </article>
        ))}
      </section>

      {loadError && (
        <p className="mb-8 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{loadError}</p>
      )}

      {reminders.length > 0 && (
        <section className="mb-8 rounded-3xl border border-fuchsia-500/20 bg-fuchsia-500/10 p-6 backdrop-blur-md">
          <h3 className="text-lg font-bold text-white">Tus recordatorios</h3>
          <p className="text-sm text-slate-400">Reservas para los próximos 7 días.</p>
          <div className="mt-4 space-y-2">
            {reminders.map((reserva) => (
              <div
                key={reserva.id}
                onClick={() => navigate(`/eventos/${reserva.eventoId}`)}
                className="flex cursor-pointer items-center justify-between gap-4 rounded-xl bg-black/30 px-4 py-3 transition hover:bg-black/50"
              >
                <div className="min-w-0 flex-1">
                  <span className="block truncate font-semibold text-white">{reserva.eventoTitulo}</span>
                  <span className="block truncate text-xs text-slate-400">
                    {reserva.cantidad} {reserva.cantidad === 1 ? 'boleto' : 'boletos'} · {formatFechaCorta(reserva.fechaEvento || reserva.eventoFecha)}
                  </span>
                </div>
                <span className="shrink-0 rounded bg-fuchsia-500/20 px-2 py-1 text-xs text-fuchsia-300">
                  {reserva.daysLeft === 0 ? '¡Es hoy!' : reserva.daysLeft === 1 ? 'Mañana' : `En ${reserva.daysLeft} días`}
                </span>
              </div>
            ))}
          </div>
        </section>
      )}

      <div className="grid gap-6 xl:grid-cols-[1.6fr_1fr]">
        <section className="flex flex-col rounded-3xl border border-white/5 bg-white/5 backdrop-blur-md">
          <div className="flex items-center justify-between border-b border-white/5 p-6">
            <div>
              <h3 className="text-lg font-bold text-white">Próximos Eventos</h3>
              <p className="text-sm text-slate-400">Cartelera destacada</p>
            </div>
            <button onClick={() => navigate('/')} className="text-sm font-semibold text-fuchsia-400 hover:text-fuchsia-300">
              Ver todos
            </button>
          </div>
          <div className="flex-1 divide-y divide-white/5">
            {loadingPreview ? (
              <Spinner texto="Cargando eventos..." />
            ) : (
              previewEvents.map((ev) => (
                <div
                  key={ev.id}
                  onClick={() => navigate(`/eventos/${ev.id}`)}
                  className="group flex cursor-pointer items-center gap-4 px-6 py-4 transition hover:bg-white/5"
                >
                  {ev.imagenUrl ? (
                    <img src={ev.imagenUrl} alt={ev.titulo} className="h-12 w-12 rounded-xl bg-black/50 object-cover" />
                  ) : (
                    <span className="grid h-12 w-12 place-items-center rounded-xl bg-fuchsia-500/20 text-fuchsia-400">
                      <Icon name="calendar" size={20} />
                    </span>
                  )}
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-semibold text-white transition-colors group-hover:text-fuchsia-300">{ev.titulo}</p>
                    <p className="text-sm text-slate-400">
                      {formatFechaCorta(ev.fecha)}
                      {ev.ciudad ? ` · ${ev.ciudad}` : ''}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className="rounded bg-fuchsia-500/20 px-2 py-1 text-xs text-fuchsia-300">{ev.categoriaNombre || 'Cultura'}</span>
                  </div>
                </div>
              ))
            )}
          </div>
          <div className="rounded-b-3xl border-t border-white/5 bg-white/[0.02] p-4">
            <button
              onClick={() => navigate('/')}
              className="w-full rounded-xl border border-white/10 bg-white/5 py-3 font-semibold text-white transition hover:bg-white/10"
            >
              Ir a la cartelera
            </button>
          </div>
        </section>

        <section className="rounded-3xl border border-white/5 bg-white/5 p-6 backdrop-blur-md">
          <div className="mb-6 border-b border-white/5 pb-4">
            <h3 className="text-lg font-bold text-white">Cultura en México</h3>
            <p className="text-sm text-slate-400">Tradiciones y arte</p>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="group relative cursor-pointer overflow-hidden rounded-xl" onClick={() => navigate('/')}>
              <img
                src="https://images.unsplash.com/photo-1599940824399-b87987ceb72a?fit=crop&w=400&q=80"
                alt="Alebrijes"
                className="h-32 w-full object-cover transition-transform duration-500 group-hover:scale-110"
              />
              <div className="absolute inset-0 flex items-end bg-gradient-to-t from-black/80 to-transparent p-3">
                <span className="text-sm font-bold text-white">Artesanías</span>
              </div>
            </div>
            <div className="group relative cursor-pointer overflow-hidden rounded-xl" onClick={() => navigate('/')}>
              <img
                src="https://images.unsplash.com/photo-1518638150340-f706e86654de?fit=crop&w=400&q=80"
                alt="Guelaguetza"
                className="h-32 w-full object-cover transition-transform duration-500 group-hover:scale-110"
              />
              <div className="absolute inset-0 flex items-end bg-gradient-to-t from-black/80 to-transparent p-3">
                <span className="text-sm font-bold text-white">Danzas</span>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
