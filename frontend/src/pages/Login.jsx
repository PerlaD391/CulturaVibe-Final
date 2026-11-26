import { useEffect, useRef, useState } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { emailError, requiredError } from '../utils/validation';
import { apiErrorMessage, extractFieldErrors, API_URL } from '../services/api';
import { firebaseConfigurado, entrarConGoogleFirebase } from '../services/firebase';
import FormField, { inputClass } from '../components/FormField';
import Icon from '../components/Icon';

// Login real contra POST /api/auth/login (JWT). La sesión queda en
// localStorage y el rol que devuelve el backend decide a dónde se entra.
// También acepta el login con Google y Facebook por OAuth2: el backend
// redirige aquí con ?token=<JWT> y esta página guarda la sesión con ese
// token. Y el login con Google por Firebase: el botón manda el ID token
// de Firebase a POST /api/auth/firebase y entra con el JWT propio.
export default function Login() {
  const { login, loginConToken, loginConFirebase, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || null;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [touched, setTouched] = useState({});
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [firebaseLoading, setFirebaseLoading] = useState(false);
  const procesadoGoogle = useRef(false);

  // Vuelta del login con Google o Facebook (?token= o ?error=google
  // o ?error=facebook). Se procesa una sola vez al montar; la URL se
  // limpia para que un recargo no repita el proceso.
  useEffect(() => {
    if (procesadoGoogle.current) return;
    procesadoGoogle.current = true;
    const params = new URLSearchParams(location.search);
    const token = params.get('token');
    const error = params.get('error');
    if (!token && error !== 'google' && error !== 'facebook') return;
    navigate(location.pathname, { replace: true });
    if (error === 'google') {
      setFormError('No se pudo iniciar sesión con Google. Inténtalo de nuevo.');
      return;
    }
    if (error === 'facebook') {
      setFormError('No se pudo iniciar sesión con Facebook. Inténtalo de nuevo.');
      return;
    }
    loginConToken(token)
      .then(navegarSegunRol)
      .catch(() => setFormError('No se pudo iniciar sesión con Google. Inténtalo de nuevo.'));
    // Solo al montar la página.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const navegarSegunRol = (auth) => {
    if (from) {
      navigate(from, { replace: true });
    } else if (auth.rol === 'ADMIN') {
      navigate('/admin', { replace: true });
    } else if (auth.rol === 'ORGANIZADOR') {
      navigate('/organizador', { replace: true });
    } else {
      navigate('/', { replace: true });
    }
  };

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
      navegarSegunRol(auth);
    } catch (err) {
      const backendFields = extractFieldErrors(err);
      if (Object.keys(backendFields).length > 0) {
        setFieldErrors(backendFields);
      } else {
        setFormError(apiErrorMessage(err));
      }
    }
  };

  // Login con Google por Firebase: el popup de Firebase devuelve el
  // ID token, se manda al backend y se entra con el JWT propio. Si
  // Firebase no está configurado, solo avisa y no se rompe nada.
  const handleFirebase = async () => {
    setFormError('');
    if (!firebaseConfigurado) {
      setFormError('El login con Firebase no está configurado en esta aplicación.');
      return;
    }
    setFirebaseLoading(true);
    try {
      const idToken = await entrarConGoogleFirebase();
      const auth = await loginConFirebase(idToken);
      navegarSegunRol(auth);
    } catch (err) {
      setFormError(apiErrorMessage(err, 'No se pudo iniciar sesión con Google por Firebase. Inténtalo de nuevo.'));
    } finally {
      setFirebaseLoading(false);
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

        <div className="mt-6 flex items-center gap-3">
          <span className="h-px flex-1 bg-white/10" />
          <span className="text-xs text-slate-500">o continúa con</span>
          <span className="h-px flex-1 bg-white/10" />
        </div>

        <a
          href={`${API_URL}/oauth2/authorization/google`}
          className="mt-4 flex w-full items-center justify-center gap-3 rounded-xl bg-white px-5 py-3 text-sm font-bold text-slate-800 shadow transition hover:bg-slate-100"
        >
          <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true">
            <path fill="#FFC107" d="M43.611,20.083H42V20H24v8h11.303C33.654,32.657,29.223,36,24,36c-6.627,0-12-5.373-12-12s5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24s8.955,20,20,20s20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z" />
            <path fill="#FF3D00" d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z" />
            <path fill="#4CAF50" d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z" />
            <path fill="#1976D2" d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z" />
          </svg>
          Continuar con Google
        </a>

        <button
          type="button"
          disabled={firebaseLoading}
          onClick={handleFirebase}
          className="mt-3 flex w-full items-center justify-center gap-3 rounded-xl bg-white px-5 py-3 text-sm font-bold text-slate-800 shadow transition hover:bg-slate-100 disabled:opacity-50"
        >
          <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true">
            <path fill="#FFC107" d="M43.611,20.083H42V20H24v8h11.303C33.654,32.657,29.223,36,24,36c-6.627,0-12-5.373-12-12s5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24s8.955,20,20,20s20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z" />
            <path fill="#FF3D00" d="M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z" />
            <path fill="#4CAF50" d="M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z" />
            <path fill="#1976D2" d="M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z" />
          </svg>
          {firebaseLoading ? 'Entrando con Firebase...' : 'Continuar con Google (Firebase)'}
        </button>

        <a
          href={`${API_URL}/oauth2/authorization/facebook`}
          className="mt-3 flex w-full items-center justify-center gap-3 rounded-xl bg-[#1877F2] px-5 py-3 text-sm font-bold text-white shadow transition hover:bg-[#166FE5]"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
          </svg>
          Continuar con Facebook
        </a>

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
