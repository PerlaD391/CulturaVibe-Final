// Spinner de carga (se usa mientras se esperan respuestas del backend).
export default function Spinner({ texto = 'Cargando...', className = '' }) {
  return (
    <div className={`flex flex-col items-center justify-center gap-3 py-16 ${className}`} role="status">
      <span className="h-10 w-10 animate-spin rounded-full border-4 border-white/10 border-t-fuchsia-500" />
      <p className="animate-pulse text-sm font-bold text-fuchsia-400">{texto}</p>
    </div>
  );
}
