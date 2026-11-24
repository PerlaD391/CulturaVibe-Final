import Icon from './Icon';

// Modal de confirmación propio (nunca confirm() nativo).
// Props: open, titulo, mensaje, textoConfirmar, textoCancelar, peligro,
// onConfirm, onCancel, loading.
export default function ConfirmModal({
  open,
  titulo = '¿Estás seguro?',
  mensaje,
  textoConfirmar = 'Eliminar',
  textoCancelar = 'Cancelar',
  peligro = true,
  loading = false,
  onConfirm,
  onCancel,
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-black/70 p-4 backdrop-blur-sm" role="dialog" aria-modal="true">
      <div className="w-full max-w-md rounded-3xl border border-white/10 bg-[#0a0a0a] p-6 shadow-2xl sm:p-8">
        <div className={`mx-auto mb-4 grid h-12 w-12 place-items-center rounded-full ${peligro ? 'bg-red-500/20 text-red-400' : 'bg-fuchsia-500/20 text-fuchsia-400'}`}>
          <Icon name={peligro ? 'trash' : 'check'} size={24} />
        </div>
        <h2 className="text-center text-xl font-bold text-white">{titulo}</h2>
        {mensaje && <p className="mt-3 text-center text-sm leading-relaxed text-slate-400">{mensaje}</p>}
        <div className="mt-6 flex flex-col gap-3 sm:flex-row">
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="flex-1 rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-bold text-white transition hover:bg-white/10 disabled:opacity-50"
          >
            {textoCancelar}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className={`flex-1 rounded-xl px-4 py-3 text-sm font-bold text-white transition disabled:opacity-50 ${
              peligro
                ? 'bg-red-600 hover:bg-red-500'
                : 'bg-gradient-to-r from-violet-600 to-fuchsia-600 hover:opacity-90'
            }`}
          >
            {loading ? 'Procesando...' : textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  );
}
