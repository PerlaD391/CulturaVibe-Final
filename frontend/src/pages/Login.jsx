import { useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { emailError, requiredError } from '../utils/validation';
import { apiErrorMessage, extractFieldErrors } from '../services/api';
import FormField, { inputClass } from '../components/FormField';
import Icon from '../components/Icon';

// Login real contra POST /api/auth/login (JWT). La sesión queda en
// localStorage y el rol que devuelve el backend decide a dónde se entra.
export default function Login() {
  const { login, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || null;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [touched, setTouched] = useState({});
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');

  const errors = {
    email: touched.email ? emailError(email) : '',
    password: touched.password ? requiredError(password, 'La contraseña') : '',
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setTouched({ email: true, password: true });
    setFieldErrors({});
    setFormError('');
    const nextErrors = { email: emailError(email), password: requiredError(password, 'La contraseña') };
    if (nextErrors.email || nextErrors.password) return;
    try {
      const auth = await login({ email: email.trim(), password });
      if (from) {
        navigate(from, { replace: true });
      } else if (auth.rol === 'ADMIN') {
        navigate('/admin', { replace: true });
      } else if (auth.rol === 'ORGANIZADOR') {
        navigate('/organizador', { replace: true });
      } else {
        navigate('/', { replace: true });
      }
    } catch (err) {
      const backendFields = extractFieldErrors(err);
      if (Object.keys(backendFields).length > 0) {
        setFieldErrors(backendFields);
      } else {
        setFormError(apiErrorMessage(err));
      }
    }
  };

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#050505] px-5 py-10">
      <div className="absolute left-[-10%] top-[-10%] h-[500px] w-[500px] rounded-full bg-violet-600/20 blur-[120px]" />
      <div className="absolute bottom-[-10%] right-[-10%] h-[500px] w-[500px] rounded-full bg-fuchsia-600/20 blur-[120px]" />

      <section className="relative z-10 w-full max-w-[460px] rounded-3xl border border-white/10 bg-white/5 p-8 shadow-2xl backdrop-blur-xl sm:p-10">
        <div className="mb-8 text-center">
          <img src="/logo.svg" alt="CulturaVibe" className="mx-auto mb-4 h-14 w-14 rounded-2xl shadow-[0_0_20px_rgba(139,92,246,0.4)]" />
          <h1 className="text-[29px] font-bold tracking-[-.035em] text-white">CulturaVibe</h1>
          <p className="mt-2 text-sm text-slate-400">Eventos culturales en México</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <FormField label="Correo electrónico" error={errors.email || fieldErrors.email}>
            <span className="flex items-center gap-3 rounded-xl border border-white/10 bg-black/40 px-4 py-3 focus-within:border-violet-500 transition-all">
              <Icon name="mail" size={19} className="text-slate-500" />
              <input
                type="email"
                placeholder="tu@correo.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onBlur={() => setTouched((t) => ({ ...t, email: true }))}
                className="w-full bg-transparent text-sm text-white outline-none placeholder:text-slate-600"
              />
            </span>
          </FormField>

          <FormField label="Contraseña" error={errors.password || fieldErrors.password}>
            <span className="flex items-center gap-3 rounded-xl border border-white/10 bg-black/40 px-4 py-3 focus-within:border-violet-500 transition-all">
              <Icon name="lock" size={19} className="text-slate-500" />
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onBlur={() => setTouched((t) => ({ ...t, password: true }))}
                className="w-full bg-transparent text-sm text-white outline-none placeholder:text-slate-600"
              />
              <button type="button" aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'} className="text-slate-500 hover:text-violet-400" onClick={() => setShowPassword(!showPassword)}>
                <Icon name="eye" size={19} />
              </button>
            </span>
          </FormField>

          {formError && <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-center text-sm font-medium text-red-300">{formError}</p>}

          <button
            disabled={loading}
            type="submit"
            className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3.5 text-sm font-bold text-white shadow-[0_0_15px_rgba(139,92,246,0.4)] transition hover:opacity-90 disabled:opacity-50"
          >
            {loading ? 'Entrando...' : 'Entrar'} <Icon name="chevronRight" size={18} />
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-400">
          <Link to="/recuperar" className="font-semibold text-fuchsia-400 hover:text-fuchsia-300">¿Olvidaste tu contraseña?</Link>
        </p>
        <p className="mt-4 text-center text-sm text-slate-400">
          ¿No tienes cuenta? <Link to="/registro" className="font-semibold text-fuchsia-400 hover:text-fuchsia-300">Crear cuenta</Link>
        </p>
        <p className="mt-6 rounded-xl border border-white/5 bg-black/30 px-4 py-3 text-center text-xs leading-relaxed text-slate-500">
          Cuentas de prueba: admin@culturavibe.mx · organizador@culturavibe.mx · cliente@culturavibe.mx
        </p>
      </section>
    </main>
  );
}
