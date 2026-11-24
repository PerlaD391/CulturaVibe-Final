import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { updateUsuario } from '../services/usuarios';
import { apiErrorMessage } from '../services/api';
import { roleLabel } from '../services/auth';
import { iniciales } from '../utils/format';
import { requiredError } from '../utils/validation';
import FormField, { inputClass } from '../components/FormField';
import Icon from '../components/Icon';

// Perfil del usuario: los datos vienen del backend (JWT). El nombre y el
// teléfono se actualizan con PUT /api/usuarios/{id} cuando el token trae
// el id del usuario. Las preferencias de notificaciones se guardan solo
// en este navegador. Para cambiar la contraseña se usa "Olvidé mi
// contraseña" (el backend no expone cambio de contraseña en sesión).
const PREFS_KEY = 'culturavibe-prefs';

const defaultPrefs = () => ({
  channels: { email: true, sms: false, app: true },
  reminders: { dayBefore: true, dayOf: true },
});

const loadPrefs = () => {
  try {
    const stored = JSON.parse(localStorage.getItem(PREFS_KEY) || 'null');
    if (stored) return { ...defaultPrefs(), ...stored };
  } catch {
    // Preferencias guardadas corruptas: se usan las de por defecto.
  }
  return defaultPrefs();
};

export default function Settings() {
  const { user, updateProfile } = useAuth();
  const [preferences, setPreferences] = useState(loadPrefs());
  const [savedPrefs, setSavedPrefs] = useState(false);

  const [editName, setEditName] = useState(user?.nombre || '');
  const [editPhone, setEditPhone] = useState(user?.telefono || '');
  const [touchedName, setTouchedName] = useState(false);
  const [profileMsg, setProfileMsg] = useState('');
  const [profileError, setProfileError] = useState('');
  const [savingProfile, setSavingProfile] = useState(false);

  useEffect(() => {
    setEditName(user?.nombre || '');
    setEditPhone(user?.telefono || '');
  }, [user?.nombre, user?.telefono]);

  const nameError = touchedName ? requiredError(editName, 'El nombre') : '';

  const handleSavePrefs = () => {
    localStorage.setItem(PREFS_KEY, JSON.stringify(preferences));
    setSavedPrefs(true);
    setTimeout(() => setSavedPrefs(false), 3000);
  };

  const handleSaveProfile = async () => {
    setProfileError('');
    setProfileMsg('');
    setTouchedName(true);
    if (requiredError(editName, 'El nombre')) return;
    if (!user?.id) {
      // El token no trae el id: se actualiza solo la vista local.
      updateProfile({ nombre: editName.trim(), telefono: editPhone.trim() });
      setProfileMsg('Nombre actualizado en este navegador. (El servidor no devolvió el id para guardarlo en tu perfil.)');
      return;
    }
    setSavingProfile(true);
    try {
      await updateUsuario(user.id, { nombre: editName.trim(), telefono: editPhone.trim() });
      updateProfile({ nombre: editName.trim(), telefono: editPhone.trim() });
      setProfileMsg('Perfil actualizado.');
    } catch (err) {
      setProfileError(apiErrorMessage(err, 'No se pudo guardar el perfil.'));
    } finally {
      setSavingProfile(false);
    }
  };

  return (
    <div className="mx-auto max-w-[1440px]">
      <div className="mb-8">
        <p className="mb-1 text-sm font-bold uppercase tracking-widest text-fuchsia-500">Sistema</p>
        <h2 className="text-3xl font-extrabold text-white">Configuración de la Cuenta</h2>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-3xl border border-white/5 bg-white/5 p-8 backdrop-blur-md">
          <div className="mb-6 flex items-center gap-4">
            <span className="grid h-16 w-16 place-items-center rounded-full border-2 border-fuchsia-500 bg-gradient-to-br from-violet-600 to-fuchsia-600 text-2xl font-bold text-white">
              {iniciales(user?.nombre)}
            </span>
            <div>
              <h3 className="text-xl font-bold text-white">{user?.nombre}</h3>
              <p className="text-sm font-semibold text-fuchsia-400">{roleLabel(user?.rol)}</p>
              <p className="text-xs text-slate-400">{user?.email}</p>
            </div>
          </div>

          <div className="space-y-4">
            <FormField label="Nombre completo" error={nameError}>
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                onBlur={() => setTouchedName(true)}
                className={inputClass(Boolean(nameError))}
              />
            </FormField>
            <FormField label="Teléfono (para avisos por SMS)">
              <input
                type="tel"
                value={editPhone}
                onChange={(e) => setEditPhone(e.target.value)}
                placeholder="Ej. 55 1234 5678"
                className={inputClass(false)}
              />
            </FormField>
            <div className="border-b border-white/5 pb-2">
              <p className="text-xs uppercase text-slate-500">Correo Electrónico</p>
              <p className="text-white">{user?.email || '—'}</p>
            </div>
            <div className="border-b border-white/5 pb-2">
              <p className="text-xs uppercase text-slate-500">Rol</p>
              <p className="text-white">{roleLabel(user?.rol)}</p>
            </div>
            {profileError && <p className="text-sm font-medium text-red-400">{profileError}</p>}
            {profileMsg && <p className="text-sm font-medium text-emerald-400">{profileMsg}</p>}
            <button
              onClick={handleSaveProfile}
              disabled={savingProfile}
              className="w-full rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3 text-sm font-bold text-white transition hover:opacity-90 disabled:opacity-50"
            >
              {savingProfile ? 'Guardando...' : 'Guardar perfil'}
            </button>
          </div>
        </section>

        <section className="rounded-3xl border border-white/5 bg-white/5 p-8 backdrop-blur-md">
          <div className="mb-6 flex items-center gap-3">
            <Icon name="lock" size={28} className="text-fuchsia-400" />
            <div>
              <h3 className="text-xl font-bold text-white">Cambiar contraseña</h3>
              <p className="text-sm text-slate-400">Te enviamos un enlace a tu correo para elegir una nueva.</p>
            </div>
          </div>
          <p className="text-sm leading-relaxed text-slate-400">
            Por seguridad, el cambio de contraseña se hace con un enlace que llega a tu correo. Te tomará menos de un
            minuto.
          </p>
          <Link
            to="/recuperar"
            className="mt-6 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3 text-sm font-bold text-white transition hover:opacity-90"
          >
            Recuperar contraseña por correo
          </Link>
        </section>

        <section className="rounded-3xl border border-white/5 bg-white/5 p-8 backdrop-blur-md lg:col-span-2">
          <div className="mb-6 flex items-center gap-3">
            <Icon name="settings" size={28} className="text-fuchsia-400" />
            <div>
              <h3 className="text-xl font-bold text-white">Notificaciones y recordatorios</h3>
              <p className="text-sm text-slate-400">Elige de qué quieres enterarte y cómo. Se guardan en este navegador.</p>
            </div>
          </div>

          <div className="space-y-6">
            <div>
              <p className="mb-2 text-sm font-semibold text-slate-300">¿Cómo te avisamos?</p>
              <div className="space-y-2 text-sm text-slate-300">
                <label className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    checked={preferences.channels.email}
                    onChange={(e) => setPreferences((p) => ({ ...p, channels: { ...p.channels, email: e.target.checked } }))}
                    className="h-4 w-4 accent-fuchsia-500"
                  />
                  Por correo electrónico
                </label>
                <label className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    checked={preferences.channels.sms}
                    onChange={(e) => setPreferences((p) => ({ ...p, channels: { ...p.channels, sms: e.target.checked } }))}
                    className="h-4 w-4 accent-fuchsia-500"
                  />
                  Por SMS (requiere teléfono)
                </label>
                <label className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    checked={preferences.channels.app}
                    onChange={(e) => setPreferences((p) => ({ ...p, channels: { ...p.channels, app: e.target.checked } }))}
                    className="h-4 w-4 accent-fuchsia-500"
                  />
                  Solo dentro de la app
                </label>
              </div>
            </div>

            <div>
              <p className="mb-2 text-sm font-semibold text-slate-300">Recordatorios de tus reservas</p>
              <div className="space-y-2 text-sm text-slate-300">
                <label className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    checked={preferences.reminders.dayBefore}
                    onChange={(e) => setPreferences((p) => ({ ...p, reminders: { ...p.reminders, dayBefore: e.target.checked } }))}
                    className="h-4 w-4 accent-fuchsia-500"
                  />
                  Un día antes
                </label>
                <label className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    checked={preferences.reminders.dayOf}
                    onChange={(e) => setPreferences((p) => ({ ...p, reminders: { ...p.reminders, dayOf: e.target.checked } }))}
                    className="h-4 w-4 accent-fuchsia-500"
                  />
                  El mismo día del evento
                </label>
              </div>
            </div>

            <button
              onClick={handleSavePrefs}
              className="w-full rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3 text-sm font-bold text-white transition hover:opacity-90"
            >
              Guardar preferencias
            </button>
            {savedPrefs && <p className="text-center text-sm font-medium text-emerald-400">Preferencias guardadas.</p>}
          </div>
        </section>
      </div>
    </div>
  );
}
