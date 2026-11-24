import { useEffect, useState } from 'react';
import { fetchCategorias } from '../services/categorias';
import { extractFieldErrors, apiErrorMessage } from '../services/api';
import { requiredError } from '../utils/validation';
import FormField, { inputClass } from './FormField';
import Icon from './Icon';

// Formulario de evento (crear/editar) en un modal propio. Los artistas
// se escriben separados por comas y se mandan como lista al backend.
export const emptyEvento = () => ({
  titulo: '',
  descripcion: '',
  fecha: '',
  hora: '',
  direccion: '',
  ciudad: '',
  estado: '',
  categoriaId: '',
  precio: '',
  estadoVenta: 'A_LA_VENTA',
  url: '',
  imagenUrl: '',
  artistasTexto: '',
});

export const eventoToForm = (evento) => ({
  titulo: evento.titulo || '',
  descripcion: evento.descripcion || '',
  fecha: evento.fecha || '',
  hora: evento.hora ? String(evento.hora).slice(0, 5) : '',
  direccion: evento.direccion || '',
  ciudad: evento.ciudad || '',
  estado: evento.estado || '',
  categoriaId: evento.categoriaId ? String(evento.categoriaId) : '',
  precio: evento.precio ?? '',
  estadoVenta: evento.estadoVenta || 'A_LA_VENTA',
  url: evento.url || '',
  imagenUrl: evento.imagenUrl || '',
  artistasTexto: Array.isArray(evento.artistas)
    ? evento.artistas.map((a) => (typeof a === 'string' ? a : a?.nombre)).filter(Boolean).join(', ')
    : '',
});

export const formToEvento = (form) => ({
  titulo: form.titulo.trim(),
  descripcion: form.descripcion.trim(),
  fecha: form.fecha,
  hora: form.hora ? `${form.hora}:00` : null,
  direccion: form.direccion.trim(),
  ciudad: form.ciudad.trim(),
  estado: form.estado.trim(),
  categoriaId: form.categoriaId ? Number(form.categoriaId) : null,
  precio: form.precio === '' ? null : Number(form.precio),
  estadoVenta: form.estadoVenta,
  url: form.url.trim() || null,
  imagenUrl: form.imagenUrl.trim() || null,
  artistas: form.artistasTexto
    .split(',')
    .map((a) => a.trim())
    .filter(Boolean),
});

export default function EventoForm({ open, titulo, initial, saving, error, onClose, onSubmit }) {
  const [form, setForm] = useState(initial || emptyEvento());
  const [categorias, setCategorias] = useState([]);
  const [touched, setTouched] = useState({});
  const [fieldErrors, setFieldErrors] = useState({});

  useEffect(() => {
    if (open) {
      setForm(initial || emptyEvento());
      setTouched({});
      setFieldErrors({});
      fetchCategorias().then(setCategorias).catch(() => setCategorias([]));
    }
  }, [open, initial]);

  if (!open) return null;

  const set = (key, value) => setForm((f) => ({ ...f, [key]: value }));

  const errors = {
    titulo: touched.titulo ? requiredError(form.titulo, 'El título') : '',
    fecha: touched.fecha ? requiredError(form.fecha, 'La fecha') : '',
    categoriaId: touched.categoriaId ? requiredError(form.categoriaId, 'La categoría') : '',
    precio:
      touched.precio && form.precio !== '' && (Number.isNaN(Number(form.precio)) || Number(form.precio) < 0)
        ? 'El precio debe ser un número mayor o igual a 0.'
        : '',
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    setTouched({ titulo: true, fecha: true, categoriaId: true, precio: true });
    setFieldErrors({});
    if (errors.titulo || errors.fecha || errors.categoriaId || errors.precio) return;
    onSubmit(formToEvento(form), setFieldErrors);
  };

  return (
    <div className="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-black/70 p-4 backdrop-blur-sm" role="dialog" aria-modal="true">
      <div className="w-full max-w-2xl rounded-3xl border border-white/10 bg-[#0a0a0a] p-6 shadow-2xl sm:p-8">
        <div className="mb-6 flex items-center justify-between">
          <h2 className="text-xl font-bold text-white">{titulo}</h2>
          <button onClick={onClose} aria-label="Cerrar" className="rounded-lg p-1 text-slate-400 hover:text-white">
            <Icon name="x" />
          </button>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <FormField label="Título" error={errors.titulo || fieldErrors.titulo}>
            <input
              type="text"
              value={form.titulo}
              onChange={(e) => set(form.titulo, e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, titulo: true }))}
              className={inputClass(Boolean(errors.titulo || fieldErrors.titulo))}
            />
          </FormField>

          <FormField label="Descripción" error={fieldErrors.descripcion}>
            <textarea
              rows={3}
              value={form.descripcion}
              onChange={(e) => set(form.descripcion, e.target.value)}
              className={`${inputClass(Boolean(fieldErrors.descripcion))} resize-y py-3`}
            />
          </FormField>

          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label="Fecha" error={errors.fecha || fieldErrors.fecha}>
              <input
                type="date"
                value={form.fecha}
                onChange={(e) => set(form.fecha, e.target.value)}
                onBlur={() => setTouched((t) => ({ ...t, fecha: true }))}
                className={`${inputClass(Boolean(errors.fecha || fieldErrors.fecha))} [color-scheme:dark]`}
              />
            </FormField>
            <FormField label="Hora" error={fieldErrors.hora}>
              <input
                type="time"
                value={form.hora}
                onChange={(e) => set(form.hora, e.target.value)}
                className={`${inputClass(Boolean(fieldErrors.hora))} [color-scheme:dark]`}
              />
            </FormField>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label="Categoría" error={errors.categoriaId || fieldErrors.categoriaId}>
              <select
                value={form.categoriaId}
                onChange={(e) => set(form.categoriaId, e.target.value)}
                onBlur={() => setTouched((t) => ({ ...t, categoriaId: true }))}
                className={inputClass(Boolean(errors.categoriaId || fieldErrors.categoriaId))}
              >
                <option value="">Elige una categoría</option>
                {categorias.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.nombre}
                  </option>
                ))}
              </select>
            </FormField>
            <FormField label="Precio (MXN)" error={errors.precio || fieldErrors.precio}>
              <input
                type="number"
                min="0"
                step="0.01"
                placeholder="0 = gratis"
                value={form.precio}
                onChange={(e) => set(form.precio, e.target.value)}
                onBlur={() => setTouched((t) => ({ ...t, precio: true }))}
                className={inputClass(Boolean(errors.precio || fieldErrors.precio))}
              />
            </FormField>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <FormField label="Dirección / sede" error={fieldErrors.direccion}>
              <input type="text" value={form.direccion} onChange={(e) => set(form.direccion, e.target.value)} className={inputClass(Boolean(fieldErrors.direccion))} />
            </FormField>
            <FormField label="Ciudad" error={fieldErrors.ciudad}>
              <input type="text" value={form.ciudad} onChange={(e) => set(form.ciudad, e.target.value)} className={inputClass(Boolean(fieldErrors.ciudad))} />
            </FormField>
            <FormField label="Estado" error={fieldErrors.estado}>
              <input type="text" value={form.estado} placeholder="Ej. Oaxaca" onChange={(e) => set(form.estado, e.target.value)} className={inputClass(Boolean(fieldErrors.estado))} />
            </FormField>
          </div>

          <FormField label="Estado de venta" error={fieldErrors.estadoVenta}>
            <select value={form.estadoVenta} onChange={(e) => set(form.estadoVenta, e.target.value)} className={inputClass(Boolean(fieldErrors.estadoVenta))}>
              <option value="A_LA_VENTA">A la venta</option>
              <option value="AGOTADO">Agotado</option>
              <option value="CANCELADO">Cancelado</option>
              <option value="POSPUESTO">Pospuesto</option>
              <option value="FINALIZADO">Finalizado</option>
            </select>
          </FormField>

          <FormField label="Artistas (separados por comas)" error={fieldErrors.artistas}>
            <input
              type="text"
              placeholder="Ej. Banda MS, Los Tigres del Norte"
              value={form.artistasTexto}
              onChange={(e) => set(form.artistasTexto, e.target.value)}
              className={inputClass(Boolean(fieldErrors.artistas))}
            />
          </FormField>

          <FormField label="Enlace de boletos (opcional)" error={fieldErrors.url}>
            <input type="url" placeholder="https://..." value={form.url} onChange={(e) => set(form.url, e.target.value)} className={inputClass(Boolean(fieldErrors.url))} />
          </FormField>

          <FormField label="URL de la imagen (opcional)" error={fieldErrors.imagenUrl}>
            <input type="url" placeholder="https://..." value={form.imagenUrl} onChange={(e) => set(form.imagenUrl, e.target.value)} className={inputClass(Boolean(fieldErrors.imagenUrl))} />
          </FormField>

          {error && <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">{error}</p>}

          <div className="flex flex-col gap-3 pt-2 sm:flex-row">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-bold text-white transition hover:bg-white/10"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={saving}
              className="flex-1 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-4 py-3 text-sm font-bold text-white transition hover:opacity-90 disabled:opacity-50"
            >
              {saving ? 'Guardando...' : 'Guardar evento'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
