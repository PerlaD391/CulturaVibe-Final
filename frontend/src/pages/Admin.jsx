import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchEventos, createEvento, updateEvento, deleteEvento, estadoVentaLabel } from '../services/eventos';
import { fetchCategorias, createCategoria, updateCategoria, deleteCategoria } from '../services/categorias';
import { fetchUsuarios, updateUsuario, deleteUsuario } from '../services/usuarios';
import { fetchReservas, estadoReservaLabel } from '../services/reservas';
import { apiErrorMessage, extractFieldErrors } from '../services/api';
import { roleLabel } from '../services/auth';
import { formatFechaCorta, formatPrecio } from '../utils/format';
import { requiredError, emailError } from '../utils/validation';
import Icon from '../components/Icon';
import Spinner from '../components/Spinner';
import ConfirmModal from '../components/ConfirmModal';
import EventoForm, { emptyEvento, eventoToForm } from '../components/EventoForm';
import FormField, { inputClass } from '../components/FormField';

// Panel de administración (solo ADMIN): gestión de usuarios, eventos,
// categorías y vista de reservas. Eliminar y cancelar siempre piden
// confirmación en un modal propio.
const TABS = [
  { id: 'eventos', label: 'Eventos' },
  { id: 'categorias', label: 'Categorías' },
  { id: 'usuarios', label: 'Usuarios' },
  { id: 'reservas', label: 'Reservas' },
];

export default function Admin() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('eventos');

  const [eventos, setEventos] = useState([]);
  const [totalEventos, setTotalEventos] = useState(0);
  const [categorias, setCategorias] = useState([]);
  const [usuarios, setUsuarios] = useState([]);
  const [totalUsuarios, setTotalUsuarios] = useState(0);
  const [reservas, setReservas] = useState([]);
  const [totalReservas, setTotalReservas] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const [eventoFormOpen, setEventoFormOpen] = useState(false);
  const [eventoEditing, setEventoEditing] = useState(null);
  const [eventoSaving, setEventoSaving] = useState(false);
  const [eventoFormError, setEventoFormError] = useState('');

  const [deleteTarget, setDeleteTarget] = useState(null); // {tipo, id, nombre}
  const [deleting, setDeleting] = useState(false);

  const [categoriaForm, setCategoriaForm] = useState({ nombre: '', descripcion: '' });
  const [categoriaEditing, setCategoriaEditing] = useState(null);
  const [categoriaSaving, setCategoriaSaving] = useState(false);
  const [categoriaError, setCategoriaError] = useState('');

  const [usuarioForm, setUsuarioForm] = useState(null); // {id, nombre, email, telefono, rol}
  const [usuarioSaving, setUsuarioSaving] = useState(false);
  const [usuarioError, setUsuarioError] = useState('');
  const [usuarioFieldErrors, setUsuarioFieldErrors] = useState({});

  const loadEventos = useCallback(async () => {
    const data = await fetchEventos({ page: 0, size: 100 });
    setEventos(data.eventos);
    setTotalEventos(data.total);
  }, []);
  const loadCategorias = useCallback(async () => {
    setCategorias(await fetchCategorias());
  }, []);
  const loadUsuarios = useCallback(async () => {
    const data = await fetchUsuarios({ page: 0, size: 100 });
    setUsuarios(data.usuarios);
    setTotalUsuarios(data.total);
  }, []);
  const loadReservas = useCallback(async () => {
    const data = await fetchReservas({ page: 0, size: 100 });
    setReservas(data.reservas);
    setTotalReservas(data.total);
  }, []);

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      await Promise.all([loadEventos(), loadCategorias(), loadUsuarios(), loadReservas()]);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudieron cargar los datos de administración.'));
    } finally {
      setLoading(false);
    }
  }, [loadEventos, loadCategorias, loadUsuarios, loadReservas]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

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
      await loadEventos();
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
      if (deleteTarget.tipo === 'evento') {
        await deleteEvento(deleteTarget.id);
        setNotice('Evento eliminado.');
        await loadEventos();
      } else if (deleteTarget.tipo === 'categoria') {
        await deleteCategoria(deleteTarget.id);
        setNotice('Categoría eliminada.');
        await loadCategorias();
      } else if (deleteTarget.tipo === 'usuario') {
        await deleteUsuario(deleteTarget.id);
        setNotice('Usuario eliminado.');
        await loadUsuarios();
      }
      setDeleteTarget(null);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo eliminar.'));
      setDeleteTarget(null);
    } finally {
      setDeleting(false);
    }
  };

  const handleSaveCategoria = async (e) => {
    e.preventDefault();
    setCategoriaError('');
    if (requiredError(categoriaForm.nombre, 'El nombre')) {
      setCategoriaError('El nombre de la categoría es obligatorio.');
      return;
    }
    setCategoriaSaving(true);
    try {
      if (categoriaEditing) {
        await updateCategoria(categoriaEditing.id, categoriaForm);
        setNotice('Categoría actualizada.');
      } else {
        await createCategoria(categoriaForm);
        setNotice('Categoría creada.');
      }
      setCategoriaForm({ nombre: '', descripcion: '' });
      setCategoriaEditing(null);
      await loadCategorias();
    } catch (err) {
      setCategoriaError(apiErrorMessage(err, 'No se pudo guardar la categoría.'));
    } finally {
      setCategoriaSaving(false);
    }
  };

  const handleSaveUsuario = async (e) => {
    e.preventDefault();
    setUsuarioError('');
    setUsuarioFieldErrors({});
    const nextErrors = {
      nombre: requiredError(usuarioForm.nombre, 'El nombre'),
      email: emailError(usuarioForm.email),
    };
    if (nextErrors.nombre || nextErrors.email) {
      setUsuarioFieldErrors(nextErrors);
      return;
    }
    setUsuarioSaving(true);
    try {
      await updateUsuario(usuarioForm.id, {
        nombre: usuarioForm.nombre.trim(),
        email: usuarioForm.email.trim(),
        telefono: usuarioForm.telefono?.trim() || '',
        rol: usuarioForm.rol,
      });
      setNotice('Usuario actualizado.');
      setUsuarioForm(null);
      await loadUsuarios();
    } catch (err) {
      const fields = extractFieldErrors(err);
      if (Object.keys(fields).length > 0) setUsuarioFieldErrors(fields);
      else setUsuarioError(apiErrorMessage(err, 'No se pudo guardar el usuario.'));
    } finally {
      setUsuarioSaving(false);
    }
  };

  if (loading) return <Spinner texto="Cargando administración..." />;

  return (
    <div className="mx-auto max-w-[1440px]">
      <div className="mb-8">
        <p className="mb-1 text-sm font-bold uppercase tracking-widest text-fuchsia-500">Solo administradores</p>
        <h2 className="text-3xl font-extrabold text-white">Administración</h2>
        <p className="mt-2 text-slate-400">Gestiona usuarios, eventos, categorías y revisa las reservas.</p>
      </div>

      {notice && (
        <p className="mb-6 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-300">{notice}</p>
      )}
      {error && (
        <p className="mb-6 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>
      )}

      <div className="mb-6 flex flex-wrap gap-2">
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`rounded-full px-5 py-2.5 text-sm font-bold transition ${
              tab === t.id ? 'bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white' : 'border border-white/10 bg-white/5 text-slate-300 hover:bg-white/10'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'eventos' && (
        <section>
          <div className="mb-4 flex items-center justify-between">
            <p className="text-sm text-slate-400">{totalEventos} eventos</p>
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
          <div className="overflow-x-auto rounded-3xl border border-white/5 bg-white/5">
            <table className="w-full min-w-[720px] text-left">
              <thead>
                <tr className="border-b border-white/5 text-xs uppercase tracking-wider text-slate-400">
                  <th className="px-5 py-4">Evento</th>
                  <th className="px-5 py-4">Fecha</th>
                  <th className="px-5 py-4">Categoría</th>
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
                    <td className="px-5 py-4">{ev.categoriaNombre || '—'}</td>
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
                          onClick={() => setDeleteTarget({ tipo: 'evento', id: ev.id, nombre: ev.titulo })}
                          className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-1.5 text-xs font-bold text-red-300 transition hover:bg-red-500/20"
                        >
                          Eliminar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {eventos.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-5 py-10 text-center text-slate-400">
                      No hay eventos todavía.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {tab === 'categorias' && (
        <section className="grid gap-6 lg:grid-cols-[1fr_1.6fr]">
          <form onSubmit={handleSaveCategoria} noValidate className="h-fit rounded-3xl border border-white/5 bg-white/5 p-6">
            <h3 className="mb-4 text-lg font-bold text-white">{categoriaEditing ? 'Editar categoría' : 'Nueva categoría'}</h3>
            <div className="space-y-4">
              <FormField label="Nombre" error="">
                <input
                  type="text"
                  value={categoriaForm.nombre}
                  onChange={(e) => setCategoriaForm((f) => ({ ...f, nombre: e.target.value }))}
                  placeholder="Ej. Música"
                  className={inputClass(false)}
                />
              </FormField>
              <FormField label="Descripción">
                <textarea
                  rows={3}
                  value={categoriaForm.descripcion}
                  onChange={(e) => setCategoriaForm((f) => ({ ...f, descripcion: e.target.value }))}
                  className={`${inputClass(false)} resize-y py-3`}
                />
              </FormField>
              {categoriaError && <p className="text-sm font-medium text-red-400">{categoriaError}</p>}
              <div className="flex gap-3">
                <button
                  type="submit"
                  disabled={categoriaSaving}
                  className="flex-1 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-4 py-3 text-sm font-bold text-white transition hover:opacity-90 disabled:opacity-50"
                >
                  {categoriaSaving ? 'Guardando...' : categoriaEditing ? 'Guardar cambios' : 'Crear categoría'}
                </button>
                {categoriaEditing && (
                  <button
                    type="button"
                    onClick={() => {
                      setCategoriaEditing(null);
                      setCategoriaForm({ nombre: '', descripcion: '' });
                    }}
                    className="rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-bold text-white transition hover:bg-white/10"
                  >
                    Cancelar
                  </button>
                )}
              </div>
            </div>
          </form>

          <div className="overflow-x-auto rounded-3xl border border-white/5 bg-white/5">
            <table className="w-full min-w-[480px] text-left">
              <thead>
                <tr className="border-b border-white/5 text-xs uppercase tracking-wider text-slate-400">
                  <th className="px-5 py-4">Categoría</th>
                  <th className="px-5 py-4">Descripción</th>
                  <th className="px-5 py-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {categorias.map((cat) => (
                  <tr key={cat.id} className="border-b border-white/5 text-sm text-slate-300 hover:bg-white/5">
                    <td className="px-5 py-4 font-semibold text-white">{cat.nombre}</td>
                    <td className="px-5 py-4">{cat.descripcion || '—'}</td>
                    <td className="px-5 py-4">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => {
                            setCategoriaEditing(cat);
                            setCategoriaForm({ nombre: cat.nombre || '', descripcion: cat.descripcion || '' });
                          }}
                          className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-bold text-white transition hover:bg-white/10"
                        >
                          Editar
                        </button>
                        <button
                          onClick={() => setDeleteTarget({ tipo: 'categoria', id: cat.id, nombre: cat.nombre })}
                          className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-1.5 text-xs font-bold text-red-300 transition hover:bg-red-500/20"
                        >
                          Eliminar
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {categorias.length === 0 && (
                  <tr>
                    <td colSpan={3} className="px-5 py-10 text-center text-slate-400">
                      No hay categorías todavía.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {tab === 'usuarios' && (
        <section className="overflow-x-auto rounded-3xl border border-white/5 bg-white/5">
          <table className="w-full min-w-[720px] text-left">
            <thead>
              <tr className="border-b border-white/5 text-xs uppercase tracking-wider text-slate-400">
                <th className="px-5 py-4">Usuario</th>
                <th className="px-5 py-4">Correo</th>
                <th className="px-5 py-4">Rol</th>
                <th className="px-5 py-4 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map((u) => (
                <tr key={u.id} className="border-b border-white/5 text-sm text-slate-300 hover:bg-white/5">
                  <td className="px-5 py-4 font-semibold text-white">{u.nombre || '—'}</td>
                  <td className="px-5 py-4">{u.email}</td>
                  <td className="px-5 py-4">{roleLabel(u.rol)}</td>
                  <td className="px-5 py-4">
                    <div className="flex justify-end gap-2">
                      <button
                        onClick={() => setUsuarioForm({ id: u.id, nombre: u.nombre || '', email: u.email || '', telefono: u.telefono || '', rol: u.rol || 'CLIENTE' })}
                        className="rounded-lg border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-bold text-white transition hover:bg-white/10"
                      >
                        Editar
                      </button>
                      <button
                        onClick={() => setDeleteTarget({ tipo: 'usuario', id: u.id, nombre: u.nombre || u.email })}
                        className="rounded-lg border border-red-500/30 bg-red-500/10 px-3 py-1.5 text-xs font-bold text-red-300 transition hover:bg-red-500/20"
                      >
                        Eliminar
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {usuarios.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-5 py-10 text-center text-slate-400">
                    No hay usuarios.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </section>
      )}

      {tab === 'reservas' && (
        <section className="overflow-x-auto rounded-3xl border border-white/5 bg-white/5">
          <table className="w-full min-w-[720px] text-left">
            <thead>
              <tr className="border-b border-white/5 text-xs uppercase tracking-wider text-slate-400">
                <th className="px-5 py-4">Evento</th>
                <th className="px-5 py-4">Usuario</th>
                <th className="px-5 py-4">Cantidad</th>
                <th className="px-5 py-4">Fecha reserva</th>
                <th className="px-5 py-4">Estado</th>
              </tr>
            </thead>
            <tbody>
              {reservas.map((r) => (
                <tr key={r.id} className="border-b border-white/5 text-sm text-slate-300 hover:bg-white/5">
                  <td className="px-5 py-4 font-semibold text-white">{r.eventoTitulo}</td>
                  <td className="px-5 py-4">{r.usuarioId ? `Usuario #${r.usuarioId}` : '—'}</td>
                  <td className="px-5 py-4">{r.cantidad}</td>
                  <td className="px-5 py-4">{formatFechaCorta(r.fechaReserva)}</td>
                  <td className="px-5 py-4">{estadoReservaLabel(r.estado)}</td>
                </tr>
              ))}
              {reservas.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-5 py-10 text-center text-slate-400">
                    No hay reservas todavía.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </section>
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
        titulo="Eliminar"
        mensaje={
          deleteTarget
            ? `¿Seguro que quieres eliminar “${deleteTarget.nombre}”? Esta acción no se puede deshacer.`
            : ''
        }
        textoConfirmar="Sí, eliminar"
        loading={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      {usuarioForm && (
        <div className="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-black/70 p-4 backdrop-blur-sm" role="dialog" aria-modal="true">
          <div className="w-full max-w-md rounded-3xl border border-white/10 bg-[#0a0a0a] p-6 shadow-2xl sm:p-8">
            <div className="mb-6 flex items-center justify-between">
              <h2 className="text-xl font-bold text-white">Editar usuario</h2>
              <button onClick={() => setUsuarioForm(null)} aria-label="Cerrar" className="rounded-lg p-1 text-slate-400 hover:text-white">
                <Icon name="x" />
              </button>
            </div>
            <form onSubmit={handleSaveUsuario} noValidate className="space-y-4">
              <FormField label="Nombre" error={usuarioFieldErrors.nombre}>
                <input
                  type="text"
                  value={usuarioForm.nombre}
                  onChange={(e) => setUsuarioForm((f) => ({ ...f, nombre: e.target.value }))}
                  className={inputClass(Boolean(usuarioFieldErrors.nombre))}
                />
              </FormField>
              <FormField label="Correo electrónico" error={usuarioFieldErrors.email}>
                <input
                  type="email"
                  value={usuarioForm.email}
                  onChange={(e) => setUsuarioForm((f) => ({ ...f, email: e.target.value }))}
                  className={inputClass(Boolean(usuarioFieldErrors.email))}
                />
              </FormField>
              <FormField label="Teléfono" error={usuarioFieldErrors.telefono}>
                <input
                  type="tel"
                  value={usuarioForm.telefono}
                  onChange={(e) => setUsuarioForm((f) => ({ ...f, telefono: e.target.value }))}
                  className={inputClass(Boolean(usuarioFieldErrors.telefono))}
                />
              </FormField>
              <FormField label="Rol" error={usuarioFieldErrors.rol}>
                <select
                  value={usuarioForm.rol}
                  onChange={(e) => setUsuarioForm((f) => ({ ...f, rol: e.target.value }))}
                  className={inputClass(Boolean(usuarioFieldErrors.rol))}
                >
                  <option value="CLIENTE">Cliente</option>
                  <option value="ORGANIZADOR">Organizador</option>
                  <option value="ADMIN">Administrador</option>
                </select>
              </FormField>
              {usuarioError && <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{usuarioError}</p>}
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setUsuarioForm(null)}
                  className="flex-1 rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-bold text-white transition hover:bg-white/10"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={usuarioSaving}
                  className="flex-1 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-4 py-3 text-sm font-bold text-white transition hover:opacity-90 disabled:opacity-50"
                >
                  {usuarioSaving ? 'Guardando...' : 'Guardar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
