// Campo de formulario con su error DEBAJO del input (nunca con alert).
// Props: label, error, children (el input/select), hint (texto de ayuda).
export default function FormField({ label, error, hint, children }) {
  return (
    <label className="block">
      {label && <span className="mb-2 block text-sm font-semibold text-slate-300">{label}</span>}
      {children}
      {hint && !error && <span className="mt-1.5 block text-xs text-slate-500">{hint}</span>}
      {error && <span className="mt-1.5 block text-xs font-medium text-red-400">{error}</span>}
    </label>
  );
}

export const inputClass = (hasError) =>
  `w-full rounded-xl border bg-black/40 px-4 py-3 text-sm text-white outline-none placeholder:text-slate-600 focus:border-violet-500 ${
    hasError ? 'border-red-500/70' : 'border-white/10'
  }`;
