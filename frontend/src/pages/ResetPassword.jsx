import { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { resetPassword } from '../services/auth';
import { passwordError, requiredError } from '../utils/validation';
import { apiErrorMessage } from '../services/api';
import FormField, { inputClass } from '../components/FormField';
import Icon from '../components/Icon';

// POST /api/auth/reset-password {token, nuevaPassword} -> 200 {mensaje}
// El token llega en la URL (?token=...) desde el enlace del correo.
export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') || '';

  const [nuevaPassword, setNuevaPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [touched, setTouched] = useState({});
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const errors = {
    password: touched.password ? passwordError(nuevaPassword) : '',
    confirmPassword:
      touched.confirmPassword && nuevaPassword && confirmPassword && nuevaPassword !== confirmPassword
        ? 'Las contraseñas no coinciden.'
        : '',
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setTouched({ password: true, confirmPassword: true });
    setError('');
    if (passwordError(nuevaPassword) || nuevaPassword !== confirmPassword) return;
    setLoading(true);
    try {
      const msg = await resetPassword({ token, nuevaPassword });
      setMessage(msg);
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo restablecer la contraseña.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#050505] px-5 py-10">
      <div className="absolute left-[-10%] top-[-10%] h-[500px] w-[500px] rounded-full bg-violet-600/20 blur-[120px]" />
      <div className="absolute bottom-[-10%] right-[-10%] h-[500px] w-[500px] rounded-full bg-fuchsia-600/20 blur-[120px]" />
      <section className="relative z-10 w-full max-w-[460px] rounded-3xl border border-white/10 bg-white/5 p-8 shadow-2xl backdrop-blur-xl sm:p-10">
        <div className="mb-8 text-center">
          <img src="/logo.svg" alt="CulturaVibe" className="mx-auto mb-4 h-14 w-14 rounded-2xl shadow-[0_0_20px_rgba(139,92,246,0.4)]" />
          <h1 className="text-2xl font-bold text-white">Nueva contraseña</h1>
          <p className="mt-2 text-sm text-slate-400">Elige una contraseña nueva para tu cuenta.</p>
        </div>

        {!token ? (
          <p className="rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-center text-sm text-amber-300">
            Este enlace no trae un token válido. Pide uno nuevo desde “¿Olvidaste tu contraseña?”.
          </p>
        ) : message ? (
          <p className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-4 text-center text-sm font-medium text-emerald-300">
            {message} Te llevamos al inicio de sesión...
          </p>
        ) : (
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <FormField
              label="Nueva contraseña"
              error={errors.password}
              hint="Mínimo 8 caracteres, una mayúscula, un número y un carácter especial."
            >
              <input
                type="password"
                placeholder="Mínimo 8 caracteres"
                value={nuevaPassword}
                onChange={(e) => setNuevaPassword(e.target.value)}
                onBlur={() => setTouched((t) => ({ ...t, password: true }))}
                className={inputClass(Boolean(errors.password))}
              />
            </FormField>
            <FormField label="Confirmar contraseña" error={errors.confirmPassword}>
              <input
                type="password"
                placeholder="Repite tu contraseña"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                onBlur={() => setTouched((t) => ({ ...t, confirmPassword: true }))}
                className={inputClass(Boolean(errors.confirmPassword))}
              />
            </FormField>
            {error && <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-center text-sm font-medium text-red-300">{error}</p>}
            <button
              disabled={loading}
              type="submit"
              className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3.5 text-sm font-bold text-white transition hover:opacity-90 disabled:opacity-50"
            >
              {loading ? 'Guardando...' : 'Restablecer contraseña'}
            </button>
          </form>
        )}

        <p className="mt-6 text-center text-sm text-slate-400">
          <Link to="/login" className="font-semibold text-fuchsia-400 hover:text-fuchsia-300">Volver al inicio de sesión</Link>
        </p>
      </section>
    </main>
  );
}
