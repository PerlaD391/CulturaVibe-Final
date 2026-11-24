import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchMisReservas, cancelReserva, estadoReservaLabel } from '../services/reservas';
import { apiErrorMessage } from '../services/api';
import { formatFechaCorta } from '../utils/format';
import Icon from '../components/Icon';
import Spinner from '../components/Spinner';
import ConfirmModal from '../components/ConfirmModal';

// "Mis eventos" (agenda en localStorage) ahora es "Mis reservas":
// consume GET /api/reservas/mis del backend. Cancelar pide confirmación
// en un modal propio y llama a DELETE /api/reservas/{id}.
export default function MisReservas() {
  const navigate = useNavigate();
  const [reservas, setReservas] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelling, setCancelling] = useState(false);
  const [notice, setNotice] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchMisReservas({ page: 0, size: 50 });
      setReservas(data.reservas);
      setTotal(data.total);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudieron cargar tus reservas.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleCancel = async () => {
    if (!cancelTarget) return;
    setCancelling(true);
    try {
      await cancelReserva(cancelTarget.id);
      setNotice(`Reserva de “${cancelTarget.eventoTitulo}” cancelada.`);
      setCancelTarget(null);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo cancelar la reserva.'));
      setCancelTarget(null);
    } finally {
      setCancelling(false);
    }
  };

  const activas = reservas.filter((r) => r.estado !== 'CANCELADA');
  const canceladas = reservas.filter((r) => r.estado === 'CANCELADA');

  const renderCard = (reserva) => {
    const cancelada = reserva.estado === 'CANCELADA';
    return (
      <article key={reserva.id} className="rounded-3xl border border-white/5 bg-white/5 p-6 backdrop-blur-md">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h3 className="text-lg font-bold leading-snug text-white">{reserva.eventoTitulo}</h3>
            <p className="mt-1 text-sm text-slate-400">
              {formatFechaCorta(reserva.fechaEvento || reserva.eventoFecha)}
              {reserva.cantidad ? ` · ${reserva.cantidad} ${reserva.cantidad === 1 ? 'boleto' : 'boletos'}` : ''}
            </p>
            {reserva.fechaReserva && (
              <p className="mt-1 text-xs text-slate-500">Reservado el {formatFechaCorta(reserva.fechaReserva)}</p>
            )}
          </div>
          <span
            className={`shrink-0 rounded-full px-3 py-1 text-xs font-bold ${
              cancelada ? 'bg-red-500/20 text-red-300' : 'bg-emerald-500/20 text-emerald-300'
            }`}
          >
            {estadoReservaLabel(reserva.estado)}
          </span>
        </div>
        <div className="mt-5 flex flex-col gap-3 sm:flex-row">
          <button
            onClick={() => navigate(`/eventos/${reserva.eventoId}`)}
            className="flex-1 rounded-xl border border-white/10 bg-white/10 px-4 py-2.5 text-sm font-bold text-white transition hover:bg-white/20"
          >
            Ver evento
          </button>
          {!cancelada && (
            <button
              onClick={() => setCancelTarget(reserva)}
              className="flex items-center justify-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-2.5 text-sm font-bold text-red-300 transition hover:bg-red-500/20"
            >
              <Icon name="trash" size={16} /> Cancelar reserva
            </button>
          )}
        </div>
      </article>
    );
  };

  return (
    <div className="mx-auto max-w-[1440px]">
      <div className="mb-8">
        <p className="mb-1 text-sm font-bold uppercase tracking-widest text-fuchsia-500">Tus boletos</p>
        <h2 className="text-3xl font-extrabold text-white">Mis reservas</h2>
        <p className="mt-2 text-slate-400">
          Las reservas que hiciste desde CulturaVibe. Puedes cancelar las que ya no necesites.
        </p>
      </div>

      {notice && (
        <p className="mb-6 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">{notice}</p>
      )}
      {error && (
        <p className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>
      )}

      {loading ? (
        <Spinner texto="Cargando tus reservas..." />
      ) : reservas.length === 0 ? (
        <div className="rounded-3xl border border-white/5 bg-white/5 p-10 text-center">
          <p className="text-lg font-bold text-white">Aún no tienes reservas</p>
          <p className="mt-2 text-slate-400">Explora la cartelera y reserva boletos en el evento que te guste.</p>
          <button
            onClick={() => navigate('/')}
            className="mt-6 rounded-xl bg-gradient-to-r from-fuchsia-600 to-violet-600 px-6 py-3 text-sm font-bold text-white transition hover:opacity-90"
          >
            Ver cartelera
          </button>
        </div>
      ) : (
        <>
          <p className="mb-4 text-sm text-slate-400">
            {activas.length} {activas.length === 1 ? 'reserva activa' : 'reservas activas'} de {total} en total
          </p>
          <div className="grid gap-6 md:grid-cols-2">{activas.map(renderCard)}</div>
          {canceladas.length > 0 && (
            <>
              <h3 className="mb-4 mt-12 text-xl font-bold text-white">Canceladas</h3>
              <div className="grid gap-6 opacity-70 md:grid-cols-2">{canceladas.map(renderCard)}</div>
            </>
          )}
        </>
      )}

      <ConfirmModal
        open={Boolean(cancelTarget)}
        titulo="Cancelar reserva"
        mensaje={
          cancelTarget
            ? `¿Seguro que quieres cancelar tu reserva de “${cancelTarget.eventoTitulo}”? Esta acción no se puede deshacer.`
            : ''
        }
        textoConfirmar="Sí, cancelar reserva"
        loading={cancelling}
        onConfirm={handleCancel}
        onCancel={() => setCancelTarget(null)}
      />
    </div>
  );
}
