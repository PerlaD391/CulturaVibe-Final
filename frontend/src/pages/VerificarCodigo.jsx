import { useState } from 'react';
import { apiErrorMessage } from '../services/api';
import { verificarCodigo, reenviarCodigo } from '../services/auth';
import FormField, { inputClass } from '../components/FormField';
import Icon from '../components/Icon';

// Pantalla del código de verificación después de registrarse. El
// código de 6 dígitos llega por correo y, si el usuario dio
// teléfono y el SMS está configurado, también por SMS. La
// verificación confirma el contacto, no es un candado: se puede
// omitir y entrar al catálogo ("Hacerlo después").
export default function VerificarCodigo({ email, telefono, onVerificado, onOmitir }) {
  const [codigo, setCodigo] = useState('');
  const [touched, setTouched] = useState(false);
  const [mensaje, setMensaje] = useState('');
  const [error, setError] = useState('');
  const [verificando, setVerificando] = useState(false);
  const [reenviando, setReenviando] = useState(false);

  const codigoError = () => {
    if (!codigo) return 'Escribe el código de 6 dígitos.';
    if (!/^\d{6}$/.test(codigo)) return 'El código debe tener 6 dígitos.';
    return '';
  };

  const handleVerificar = async (e) => {
    e.preventDefault();
    setTouched(true);
    setMensaje('');
    setError('');
    if (codigoError()) return;
    setVerificando(true);
    try {
      const msg = await verificarCodigo({ email, codigo });
      setMensaje(msg);
      setTimeout(onVerificado, 1500);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo verificar el código.'));
    } finally {
      setVerificando(false);
    }
  };

  const handleReenviar = async () => {
    setMensaje('');
    setError('');
    setReenviando(true);
    try {
      const msg = await reenviarCodigo({ email });
      setMensaje(msg);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo reenviar el código.'));
    } finally {
      setReenviando(false);
    }
  };

  return (
    <div>
      <div className="mb-8 text-center">
        <img src="/logo.svg" alt="CulturaVibe" className="mx-auto mb-4 h-14 w-14 rounded-2xl shadow-[0_0_20px_rgba(139,92,246,0.4)]" />
        <h1 className="text-[29px] font-bold tracking-[-.035em] text-white">Verifica tu correo</h1>
        <p className="mt-2 text-sm leading-relaxed text-slate-400">
          Te enviamos un código de 6 dígitos a <span className="font-semibold text-slate-200">{email}</span>
          {telefono ? (
            <> y por SMS al <span className="font-semibold text-slate-200">{telefono}</span>.</>
          ) : (
            '.'
          )}
          {' '}Es válido por 15 minutos.
        </p>
      </div>

      <form onSubmit={handleVerificar} noValidate className="space-y-4">
        <FormField label="Código de verificación" error={touched ? codigoError() : ''}>
          <input
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="000000"
            value={codigo}
            onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
            onBlur={() => setTouched(true)}
            className={`${inputClass(Boolean(touched && codigoError()))} text-center text-xl font-bold tracking-[0.5em]`}
          />
        </FormField>

        {mensaje && <p className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-center text-sm font-medium text-emerald-300">{mensaje}</p>}
        {error && <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-center text-sm font-medium text-red-300">{error}</p>}

        <button
          disabled={verificando}
          type="submit"
          className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3.5 text-sm font-bold text-white shadow-[0_0_15px_rgba(139,92,246,0.4)] transition hover:opacity-90 disabled:opacity-50"
        >
          {verificando ? 'Verificando...' : 'Verificar código'} <Icon name="check" size={18} />
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-slate-400">
        ¿No llegó?{' '}
        <button type="button" disabled={reenviando} onClick={handleReenviar} className="font-semibold text-fuchsia-400 hover:text-fuchsia-300 disabled:opacity-50">
          {reenviando ? 'Enviando...' : 'Reenviar código'}
        </button>
      </p>
      <p className="mt-4 text-center text-sm text-slate-400">
        <button type="button" onClick={onOmitir} className="font-semibold text-slate-300 hover:text-white">
          Hacerlo después
        </button>
      </p>
    </div>
  );
}
