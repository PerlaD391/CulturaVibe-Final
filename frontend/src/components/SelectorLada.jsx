import { LADAS, LADA_OTRA } from '../utils/telefono';

// Select de lada (código de país) junto al teléfono. Props: lada (el
// código elegido, por ejemplo '+52'), codigoOtro (el código escrito
// cuando se elige "Otro"), onLadaChange(codigo), onCodigoOtroChange(codigo).
// Estados Unidos y Canadá comparten el +1: el código que se manda es
// el mismo, solo cambia la etiqueta que se muestra.
export default function SelectorLada({ lada, codigoOtro, onLadaChange, onCodigoOtroChange, disabled = false }) {
  return (
    <span className="flex shrink-0 gap-2">
      <span className="relative flex items-center">
        <select
          value={lada}
          onChange={(e) => onLadaChange(e.target.value)}
          disabled={disabled}
          aria-label="Lada del país"
          className="w-44 appearance-none rounded-xl border border-white/10 bg-black/40 px-3 py-3 pr-8 text-sm font-medium text-white outline-none focus:border-violet-500 disabled:opacity-50"
        >
          {LADAS.map((l) => (
            <option key={`${l.pais}-${l.codigo}`} value={l.codigo}>
              {l.pais} ({l.codigo})
            </option>
          ))}
          <option value={LADA_OTRA}>Otro</option>
        </select>
        <span className="pointer-events-none absolute right-2.5 text-white">▾</span>
      </span>
      {lada === LADA_OTRA && (
        <input
          type="tel"
          value={codigoOtro}
          onChange={(e) => onCodigoOtroChange(e.target.value)}
          disabled={disabled}
          placeholder="+..."
          aria-label="Otra lada"
          className="w-20 rounded-xl border border-white/10 bg-black/40 px-3 py-3 text-sm text-white outline-none placeholder:text-slate-600 focus:border-violet-500 disabled:opacity-50"
        />
      )}
    </span>
  );
}
