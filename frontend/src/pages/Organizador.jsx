import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchEventos, createEvento, updateEvento, deleteEvento, estadoVentaLabel } from '../services/eventos';
import { fetchReservas, estadoReservaLabel } from '../services/reservas';
import { apiErrorMessage, extractFieldErrors } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { formatFechaCorta, formatPrecio } from '../utils/format';
import Spinner from '../components/Spinner';
import ConfirmModal from '../components/ConfirmModal';
import EventoForm, { emptyEvento, eventoToForm } from '../components/EventoForm';

// Panel del organizador (solo ORGANIZADOR): sus eventos (los que creó,
// organizadorId = su id) y las reservas de esos eventos.
export default function Organizador() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [eventos, setEventos] = useState([]);
  const [reservas, setReservas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [eventoFormOpen, setEventoFormOpen] = useState(false);
  const [eventoEditing, setEventoEditing] = useState(null);
  const [eventoSaving, setEventoSaving] = useState(false);
  const [eventoFormError, setEventoFormError] = useState('');

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [eventosData, reservasData] = await Promise.all([
        fetchEventos({ page: 0, size: 100 }),
        fetchReservas({ page: 0, size: 100 }),
      ]);
      // Solo los eventos del organizador en sesión (cuando el backend
      // trae organizadorId); si no trae el campo, se muestran todos los
      // que el backend le devolvió.
      const mios = user?.id
        ? eventosData.eventos.filter((ev) => !ev.organizadorId || ev.organizadorId === user.id)
        : eventosData.eventos;
      setEventos(mios);
      const misIds = new Set(mios.map((ev) => ev.id));
      setReservas(reservasData.reservas.filter((r) => misIds.has(r.eventoId)));
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudieron cargar tus eventos.'));
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSaveEvento = async (payload, setFieldErrors) => {
    setEventoSaving(true);
    setEventoFormError('');
    try {
      if (eventoEditing) {
        await updateEvento(eventoEditing.id, payload);
        setNotice('Evento actualizado.');
      } else {
        await createEvento(payload);
        setNotice('Evento creado.');
      }
      setEventoFormOpen(false);
      setEventoEditing(null);
      await load();
    } catch (err) {
      const fields = extractFieldErrors(err);
      if (Object.keys(fields).length > 0) setFieldErrors(fields);
      else setEventoFormError(apiErrorMessage(err, 'No se pudo guardar el evento.'));
    } finally {
      setEventoSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteEvento(deleteTarget.id);
      setNotice('Evento eliminado.');
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo eliminar el evento.'));
      setDeleteTarget(null);
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <Spinner texto="Cargando tus eventos..." />;

  const totalBoletos = reservas.reduce((sum, r) => sum + (r.estado === 'CANCELADA' ? 0 : r.cantidad || 0), 0);

  return (
    <div className="mx-auto max-w-[1440px]">
      <div className="mb-8">
        <p className="mb-1 text-sm font-bold uppercase tracking-widest text-fuchsia-500">Organizador</p>
        <h2 className="text-3xl font-extrabold text-white">Mis eventos</h2>
        <p className="mt-2 text-slate-400">Crea y gestiona tus eventos, y revisa cuántos boletos se han reservado.</p>
      </div>

      {notice && (
        <p className="mb-6 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">{notice}</p>
      )}
      {error && (
        <p className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>
      )}

      <section className="mb-8 grid gap-4 sm:grid-cols-3">
        {[
          { title: 'Mis eventos', value: String(eventos.length), note: 'Publicados por ti' },
          { title: 'Reservas', value: String(reservas.length), note: 'En tus eventos' },
          { title: 'Boletos reservados', value: String(totalBoletos), note: 'Sin contar canceladas' },
        ].map((item) => (
          <article key={item.title} className="rounded-3xl border border-white/5 bg-white/5 p-6 backdrop-blur-md">
            <p className="text-sm font-medium text-slate-400">{item.title}</p>
            <p className="mt-1 text-[36px] font-extrabold tracking-[-.04em] text-white">{item.value}</p>
            <p className="mt-1 text-[12px] font-medium text-slate-500">{item.note}</p>
          </article>
        ))}
      </section>

      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-xl font-bold text-white">Tus eventos</h3>
        <button
          onClick={() => {
            setEventoEditing(null);
            setEventoFormError('');
            setEventoFormOpen(true);
          }}
          className="rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-2.5 text-sm font-bold text-white transition hover:opacity-90"
        >
          + Nuevo evento
        </button>
      </div>

      {eventos.length === 0 ? (
        <div className="rounded-3xl border border-white/5 bg-white/5 p-10 text-center">
          <p className="text-lg font-bold text-white">Aún no tienes eventos</p>
          <p className="mt-2 text-slate-400">Crea tu primer evento para que aparezca en la cartelera.</p>
        </div>
      ) : (
        <div className="mb-10 overflow-x-auto rounded-3xl border border-white/5 bg-white/5">
          <table className="w-full min-w-[720px] text-left">
            <thead>
              <tr className="border-b border-white/5 text-xs uppercase tracking-wider text-slate-400">
                <th className="px-5 py-4">Evento</th>
                <th className="px-5 py-4">Fecha</th>
                <th className="px-5 py-4">Precio</th>
                <th className="px-5 py-4">Venta</th>
                <th className="px-5 py-4 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {eventos.map((ev) => (
                <tr key={ev.id} className="border-b border-white/5 text-sm text-slate-300 hover:bg-white/5">
                  <td className="px-5 py-4">
                    <button onClick={() => navigate(`/eventos/${ev.id}`)} className="font-semibold text-white hover:text-fuchsia-300">
                      {ev.titulo}
                    </button>
                    <p className="text-xs text-slate-500">{ev.ciudad || ''}</p>
                  </td>
                  <td className="px-5 py-4">{formatFechaCorta(ev.fecha)}</td>
                  <td className="px-5 py-4">{formatPrecio(ev.precio)}</td>
                  <td className="px-5 py-4">{estadoVentaLabel(ev.estadoVenta)}</td>
                  <td className="px-5 py-4">
                    <div className="flex justify-end gap-2">
                      <button
                        onClick={() => {
                          setEventoEditing(ev);
                          setEventoFormError('');
                          setEventoFormOpen(true);
                        }}
                        className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-bold text-white transition hover:bg-white/10"
                      >
                        Editar
                      </button>
                      <button
                        onClick={() => setDeleteTarget(ev)}
                        className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-1.5 text-xs font-bold text-red-300 transition hover:bg-red-500/20"
                      >
                        Eliminar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <h3 className="mb-4 text-xl font-bold text-white">Reservas en tus eventos</h3>
      {reservas.length === 0 ? (
        <p className="rounded-3xl border border-white/5 bg-white/5 p-8 text-center text-slate-400">
          Nadie ha reservado en tus eventos todavía.
        </p>
      ) : (
        <div className="overflow-x-auto rounded-3xl border border-white/5 bg-white/5">
          <table className="w-full min-w-[640px] text-left">
            <thead>
              <tr className="border-b border-white/5 text-xs uppercase tracking-wider text-slate-400">
                <th className="px-5 py-4">Evento</th>
                <th className="px-5 py-4">Cantidad</th>
                <th className="px-5 py-4">Fecha reserva</th>
                <th className="px-5 py-4">Estado</th>
              </tr>
            </thead>
            <tbody>
              {reservas.map((r) => (
                <tr key={r.id} className="border-b border-white/5 text-sm text-slate-300 hover:bg-white/5">
                  <td className="px-5 py-4 font-semibold text-white">{r.eventoTitulo}</td>
                  <td className="px-5 py-4">{r.cantidad}</td>
                  <td className="px-5 py-4">{formatFechaCorta(r.fechaReserva)}</td>
                  <td className="px-5 py-4">{estadoReservaLabel(r.estado)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <EventoForm
        open={eventoFormOpen}
        titulo={eventoEditing ? 'Editar evento' : 'Nuevo evento'}
        initial={eventoEditing ? eventoToForm(eventoEditing) : emptyEvento()}
        saving={eventoSaving}
        error={eventoFormError}
        onClose={() => {
          setEventoFormOpen(false);
          setEventoEditing(null);
        }}
        onSubmit={handleSaveEvento}
      />

      <ConfirmModal
        open={Boolean(deleteTarget)}
        titulo="Eliminar evento"
        mensaje={
          deleteTarget ? `¿Seguro que quieres eliminar “${deleteTarget.titulo}”? Esta acción no se puede deshacer.` : ''
        }
        textoConfirmar="Sí, eliminar"
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
