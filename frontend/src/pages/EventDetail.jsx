import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { fetchEventoById, estadoVentaLabel, estaALaVenta } from '../services/eventos';
import { createReserva } from '../services/reservas';
import { apiErrorMessage } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { formatFecha, formatHora, formatPrecio } from '../utils/format';
import Icon from '../components/Icon';
import Spinner from '../components/Spinner';

// Detalle público del evento (DTO del backend). Si hay sesión, se puede
// reservar: POST /api/reservas {eventoId, cantidad}. Si el evento trae
// url, se mantiene el enlace "Comprar boletos".
export default function EventDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);
  const [cantidad, setCantidad] = useState(1);
  const [reserving, setReserving] = useState(false);
  const [reservaOk, setReservaOk] = useState('');
  const [reservaError, setReservaError] = useState('');

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    fetchEventoById(id)
      .then((data) => {
        if (!cancelled) setEvent(data);
      })
      .catch((err) => {
        if (!cancelled) setError(apiErrorMessage(err, 'No se pudo cargar el evento.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const handleReservar = async () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/eventos/${id}` } } });
      return;
    }
    setReserving(true);
    setReservaOk('');
    setReservaError('');
    try {
      await createReserva({ eventoId: Number(id), cantidad });
      setReservaOk(`Reserva confirmada: ${cantidad} ${cantidad === 1 ? 'boleto' : 'boletos'}. La verás en “Mis Reservas”.`);
    } catch (err) {
      setReservaError(apiErrorMessage(err, 'No se pudo crear la reserva.'));
    } finally {
      setReserving(false);
    }
  };

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
    } catch {
      const input = document.createElement('textarea');
      input.value = window.location.href;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
    }
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  if (loading) return <Spinner texto="Cargando detalles..." />;
  if (error || !event) {
    return (
      <div className="mx-auto max-w-[600px] py-20 text-center">
        <p className="text-red-400">{error || 'Evento no encontrado.'}</p>
        <button
          onClick={() => navigate('/')}
          className="mt-6 rounded-xl bg-white/10 px-6 py-3 text-sm font-bold text-white transition hover:bg-white/20"
        >
          Volver al catálogo
        </button>
      </div>
    );
  }

  const imageUrl =
    event.imagenUrl ||
    'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=1200&q=80';
  const date = formatFecha(event.fecha);
  const time = formatHora(event.hora) || 'Hora por confirmar';
  const placeLine = [event.direccion, event.ciudad, event.estado].filter(Boolean).join(', ');
  const category = event.categoriaNombre || 'Cultura';
  const price = formatPrecio(event.precio);
  const info = event.descripcion || 'No hay descripción disponible para este evento.';
  const artistas = Array.isArray(event.artistas)
    ? event.artistas.map((a) => (typeof a === 'string' ? a : a?.nombre)).filter(Boolean)
    : typeof event.artistas === 'string'
      ? event.artistas.split(',').map((a) => a.trim()).filter(Boolean)
      : [];
  const aLaVenta = estaALaVenta(event.estadoVenta);

  const shareText = `${event.titulo} — ${date} a las ${time}${placeLine ? ` en ${placeLine}` : ''}. ${window.location.href}`;
  const whatsappUrl = `https://wa.me/?text=${encodeURIComponent(shareText)}`;

  return (
    <div className="mx-auto max-w-[1200px]">
      <button onClick={() => navigate('/')} className="mb-6 flex items-center gap-2 text-sm text-slate-400 transition hover:text-white">
        <Icon name="chevronLeft" size={18} /> Volver al catálogo
      </button>

      <div className="flex flex-col gap-8 overflow-hidden rounded-3xl border border-white/5 bg-[#0a0a0a] shadow-2xl lg:flex-row">
        <div className="relative h-[400px] w-full lg:h-auto lg:w-1/2">
          <img src={imageUrl} alt={event.titulo} className="h-full w-full object-cover" />
          <div className="absolute left-4 top-4 rounded-full border border-white/10 bg-black/70 px-4 py-1.5 backdrop-blur-md">
            <span className="text-xs font-bold uppercase tracking-wider text-fuchsia-400">{category}</span>
          </div>
          {event.estado && (
            <div className="absolute right-4 top-4 rounded-full border border-white/10 bg-black/70 px-4 py-1.5 backdrop-blur-md">
              <span className="text-xs font-bold uppercase tracking-wider text-white">{event.estado}</span>
            </div>
          )}
        </div>

        <div className="flex w-full flex-col justify-center p-8 lg:w-1/2 lg:p-12">
          <h1 className="mb-4 text-3xl font-extrabold leading-tight text-white lg:text-4xl">{event.titulo}</h1>
          <p
            className={`mb-4 w-fit rounded-full px-3 py-1 text-xs font-bold ${
              aLaVenta ? 'bg-emerald-500/20 text-emerald-300' : 'bg-amber-500/20 text-amber-300'
            }`}
          >
            {estadoVentaLabel(event.estadoVenta)}
          </p>

          <div className="mb-8 space-y-4">
            <div className="flex items-start gap-4 text-slate-300">
              <div className="rounded-lg bg-white/5 p-2 text-fuchsia-400">
                <Icon name="calendar" size={20} />
              </div>
              <div>
                <p className="font-semibold text-white">Fecha y Hora</p>
                <p>
                  {date} a las {time}
                </p>
              </div>
            </div>

            <div className="flex items-start gap-4 text-slate-300">
              <div className="rounded-lg bg-white/5 p-2 text-fuchsia-400">
                <Icon name="map-pin" size={20} />
              </div>
              <div>
                <p className="font-semibold text-white">Lugar</p>
                <p>{placeLine || 'Lugar por confirmar'}</p>
              </div>
            </div>

            <div className="flex items-start gap-4 text-slate-300">
              <div className="rounded-lg bg-white/5 p-2 text-fuchsia-400">
                <Icon name="briefcase" size={20} />
              </div>
              <div>
                <p className="font-semibold text-white">Precio del boleto</p>
                <p>{price}</p>
              </div>
            </div>
          </div>

          {artistas.length > 0 && (
            <div className="mb-8">
              <h3 className="mb-2 text-lg font-semibold text-white">Artistas</h3>
              <div className="flex flex-wrap gap-2">
                {artistas.map((artist) => (
                  <span key={artist} className="rounded-full border border-white/10 bg-white/5 px-3 py-1 text-sm text-slate-200">
                    {artist}
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="mb-8">
            <h3 className="mb-2 text-lg font-semibold text-white">Acerca del evento</h3>
            <p className="leading-relaxed text-slate-400">{info}</p>
          </div>

          {event.url && aLaVenta && (
            <a
              href={event.url}
              target="_blank"
              rel="noopener noreferrer"
              className="mb-3 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-fuchsia-600 to-violet-600 px-5 py-4 text-lg font-bold text-white shadow-[0_0_20px_rgba(217,70,239,0.4)] transition hover:opacity-90"
            >
              <Icon name="ticket" size={20} /> Comprar boletos <Icon name="external" size={16} />
            </a>
          )}
          {!aLaVenta && (
            <p className="mb-3 rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-sm text-amber-300">
              La venta de boletos para este evento ya está cerrada.
            </p>
          )}

          {/* Reserva en CulturaVibe */}
          <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <label className="flex flex-1 items-center gap-3">
                <span className="text-sm font-semibold text-slate-300">Boletos</span>
                <input
                  type="number"
                  min="1"
                  max="10"
                  value={cantidad}
                  onChange={(e) => setCantidad(Math.max(1, Math.min(10, Number(e.target.value) || 1)))}
                  className="w-20 rounded-xl border border-white/10 bg-black/40 px-3 py-2.5 text-sm text-white outline-none focus:border-violet-500"
                />
              </label>
              <button
                onClick={handleReservar}
                disabled={reserving || !aLaVenta}
                className="flex flex-1 items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3.5 text-sm font-bold text-white transition hover:opacity-90 disabled:opacity-50"
              >
                <Icon name="ticket" size={18} />
                {reserving ? 'Reservando...' : isAuthenticated ? 'Reservar boletos' : 'Inicia sesión para reservar'}
              </button>
            </div>
            {reservaOk && (
              <p className="mt-3 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">
                {reservaOk}{' '}
                <Link to="/mis-reservas" className="font-bold underline underline-offset-2">
                  Ver mis reservas
                </Link>
              </p>
            )}
            {reservaError && (
              <p className="mt-3 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{reservaError}</p>
            )}
            {!isAuthenticated && (
              <p className="mt-3 text-xs text-slate-500">
                Necesitas una cuenta para reservar. <Link to="/login" className="font-semibold text-fuchsia-400">Inicia sesión</Link> o{' '}
                <Link to="/registro" className="font-semibold text-fuchsia-400">crea una cuenta</Link>.
              </p>
            )}
          </div>

          <div className="mt-4 flex flex-col gap-3 sm:flex-row">
            <a
              href={whatsappUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-emerald-500/40 bg-emerald-600/20 px-4 py-3 text-sm font-bold text-emerald-300 transition hover:bg-emerald-600/30"
            >
              <Icon name="share" size={18} /> Compartir por WhatsApp
            </a>
            <button
              onClick={handleCopyLink}
              className="flex flex-1 items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-bold text-white transition hover:bg-white/10"
            >
              <Icon name={copied ? 'check' : 'share'} size={18} /> {copied ? '¡Enlace copiado!' : 'Copiar enlace'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
