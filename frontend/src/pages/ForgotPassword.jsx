import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../services/auth';
import { emailError } from '../utils/validation';
import { apiErrorMessage } from '../services/api';
import FormField, { inputClass } from '../components/FormField';
import Icon from '../components/Icon';

// POST /api/auth/forgot-password {email} -> 200 {mensaje}
export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [touched, setTouched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const errorText = touched ? emailError(email) : '';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setTouched(true);
    setError('');
    setMessage('');
    if (emailError(email)) return;
    setLoading(true);
    try {
      const msg = await forgotPassword(email.trim());
      setMessage(msg);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo enviar el correo de recuperación.'));
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
          <h1 className="text-2xl font-bold text-white">Recuperar contraseña</h1>
          <p className="mt-2 text-sm text-slate-400">Te enviaremos las instrucciones para crear una contraseña nueva.</p>
        </div>

        {message ? (
          <div className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-4 text-center">
            <p className="text-sm font-medium text-emerald-300">{message}</p>
            <p className="mt-2 text-xs text-slate-400">Revisa tu bandeja de entrada (y la carpeta de spam).</p>
          </div>
        ) : (
          <form onSubmit={handleSubmit} noValidate className="space-y-4">
            <FormField label="Correo electrónico" error={errorText}>
              <span className="flex items-center gap-3 rounded-xl border border-white/10 bg-black/40 px-4 py-3 focus-within:border-violet-500 transition-all">
                <Icon name="mail" size={19} className="text-slate-500" />
                <input
                  type="email"
                  placeholder="tu@correo.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  onBlur={() => setTouched(true)}
                  className="w-full bg-transparent text-sm text-white outline-none placeholder:text-slate-600"
                />
              </span>
            </FormField>
            {error && <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-center text-sm font-medium text-red-300">{error}</p>}
            <button
              disabled={loading}
              type="submit"
              className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3.5 text-sm font-bold text-white transition hover:opacity-90 disabled:opacity-50"
            >
              {loading ? 'Enviando...' : 'Enviar instrucciones'}
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
