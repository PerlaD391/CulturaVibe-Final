import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { emailError, passwordError, passwordChecks, requiredError } from '../utils/validation';
import { apiErrorMessage, extractFieldErrors } from '../services/api';
import FormField, { inputClass } from '../components/FormField';
import Icon from '../components/Icon';

// Registro real contra POST /api/auth/register (JWT). El rol lo elige
// el usuario: CLIENTE (por defecto) u ORGANIZADOR (empresa); la política
// de contraseña se valida en tiempo real bajo el campo.
export default function Register() {
  const { register, loading } = useAuth();
  const navigate = useNavigate();

  const [nombre, setNombre] = useState('');
  const [email, setEmail] = useState('');
  const [telefono, setTelefono] = useState('');
  const [rol, setRol] = useState('CLIENTE');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [touched, setTouched] = useState({});
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState('');

  const checks = passwordChecks(password);
  const errors = {
    nombre: touched.nombre ? requiredError(nombre, 'El nombre') : '',
    email: touched.email ? emailError(email) : '',
    password: touched.password ? passwordError(password) : '',
    confirmPassword:
      touched.confirmPassword && password && confirmPassword && password !== confirmPassword
        ? 'Las contraseñas no coinciden.'
        : '',
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setTouched({ nombre: true, email: true, password: true, confirmPassword: true });
    setFieldErrors({});
    setFormError('');
    const nextErrors = {
      nombre: requiredError(nombre, 'El nombre'),
      email: emailError(email),
      password: passwordError(password),
      confirmPassword: password !== confirmPassword ? 'Las contraseñas no coinciden.' : '',
    };
    if (Object.values(nextErrors).some(Boolean)) return;
    try {
      await register({
        nombre: nombre.trim(),
        email: email.trim(),
        password,
        telefono: telefono.trim() || undefined,
        rol,
      });
      navigate('/', { replace: true });
    } catch (err) {
      const backendFields = extractFieldErrors(err);
      if (Object.keys(backendFields).length > 0) {
        setFieldErrors(backendFields);
      } else {
        setFormError(apiErrorMessage(err, 'No se pudo crear la cuenta.'));
      }
    }
  };

  const checkItem = (ok, label) => (
    <li className={`flex items-center gap-1.5 ${ok ? 'text-emerald-400' : 'text-slate-500'}`}>
      <Icon name="check" size={13} /> {label}
    </li>
  );

  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#050505] px-5 py-10">
      <div className="absolute left-[-10%] top-[-10%] h-[500px] w-[500px] rounded-full bg-violet-600/20 blur-[120px]" />
      <div className="absolute bottom-[-10%] right-[-10%] h-[500px] w-[500px] rounded-full bg-fuchsia-600/20 blur-[120px]" />
      <section className="relative z-10 w-full max-w-[520px] rounded-3xl border border-white/10 bg-white/5 p-8 shadow-2xl backdrop-blur-xl sm:p-10">
        <div className="mb-8 text-center">
          <img src="/logo.svg" alt="CulturaVibe" className="mx-auto mb-4 h-14 w-14 rounded-2xl shadow-[0_0_20px_rgba(139,92,246,0.4)]" />
          <h1 className="text-[29px] font-bold tracking-[-.035em] text-white">Crear cuenta</h1>
          <p className="mt-2 text-sm text-slate-400">Únete a CulturaVibe y reserva tus eventos favoritos.</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <FormField label="Nombre completo" error={errors.nombre || fieldErrors.nombre}>
            <input
              type="text"
              placeholder="Ej. Perla Gallardo"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, nombre: true }))}
              className={inputClass(Boolean(errors.nombre || fieldErrors.nombre))}
            />
          </FormField>

          <FormField label="Correo electrónico" error={errors.email || fieldErrors.email} hint="Puedes usar Gmail, Outlook o cualquier correo válido.">
            <input
              type="email"
              placeholder="tu@correo.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, email: true }))}
              className={inputClass(Boolean(errors.email || fieldErrors.email))}
            />
          </FormField>

          <FormField label="Teléfono (opcional)" error={fieldErrors.telefono}>
            <input
              type="tel"
              placeholder="Ej. 55 1234 5678"
              value={telefono}
              onChange={(e) => setTelefono(e.target.value)}
              className={inputClass(Boolean(fieldErrors.telefono))}
            />
          </FormField>

          <FormField
            label="Tipo de cuenta"
            error={fieldErrors.rol}
            hint={
              rol === 'ORGANIZADOR'
                ? 'Las empresas organizadoras pueden crear y gestionar sus eventos.'
                : 'Reserva eventos y gestiona tus reservas.'
            }
          >
            <span className="relative flex items-center">
              <select
                value={rol}
                onChange={(e) => setRol(e.target.value)}
                className="w-full appearance-none rounded-xl border border-white/10 bg-black/40 px-4 py-3 pr-10 text-sm font-medium text-white outline-none focus:border-violet-500"
              >
                <option value="CLIENTE">Cliente</option>
                <option value="ORGANIZADOR">Empresa organizadora</option>
              </select>
              <Icon name="chevronDown" size={16} className="pointer-events-none absolute right-3 text-white" />
            </span>
          </FormField>

          <FormField
            label="Contraseña"
            error={errors.password || fieldErrors.password}
            hint="Mínimo 8 caracteres, una mayúscula, un número y un carácter especial."
          >
            <span className="flex items-center gap-3 rounded-xl border border-white/10 bg-black/40 px-4 py-3 focus-within:border-violet-500 transition-all">
              <Icon name="lock" size={19} className="text-slate-500" />
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="Mínimo 8 caracteres"
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

          {password && (
            <ul className="grid grid-cols-2 gap-x-4 gap-y-1 rounded-xl border border-white/5 bg-black/30 px-4 py-3 text-xs">
              {checkItem(checks.length, 'Mínimo 8 caracteres')}
              {checkItem(checks.upper, 'Una mayúscula')}
              {checkItem(checks.number, 'Un número')}
              {checkItem(checks.special, 'Un carácter especial')}
            </ul>
          )}

          <FormField label="Confirmar contraseña" error={errors.confirmPassword || fieldErrors.password}>
            <input
              type="password"
              placeholder="Repite tu contraseña"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, confirmPassword: true }))}
              className={inputClass(Boolean(errors.confirmPassword))}
            />
          </FormField>

          {formError && <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-center text-sm font-medium text-red-300">{formError}</p>}

          <button
            disabled={loading}
            type="submit"
            className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3.5 text-sm font-bold text-white shadow-[0_0_15px_rgba(139,92,246,0.4)] transition hover:opacity-90 disabled:opacity-50"
          >
            {loading ? 'Creando tu cuenta...' : 'Crear cuenta'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-400">
          ¿Ya tienes cuenta? <Link to="/login" className="font-semibold text-fuchsia-400 hover:text-fuchsia-300">Inicia sesión</Link>
        </p>
      </section>
    </main>
  );
}
