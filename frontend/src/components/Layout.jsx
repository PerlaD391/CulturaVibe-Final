import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../services/auth';
import { iniciales } from '../utils/format';
import Icon from './Icon';

// Barra de navegación visible en toda la parte interna (y en el catálogo
// público). Muestra nombre, correo, avatar por defecto con las iniciales
// del usuario y el botón de cerrar sesión.
export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  const handleLogout = async () => {
    setLoggingOut(true);
    await logout();
    navigate('/login');
  };

  const rol = user?.rol || 'CLIENTE';

  const navItems = [
    { label: 'Panel Principal', icon: 'home', path: '/dashboard', roles: ['ADMIN', 'ORGANIZADOR', 'CLIENTE'] },
    { label: 'Calendario de Eventos', icon: 'calendar', path: '/', roles: ['ADMIN', 'ORGANIZADOR', 'CLIENTE'] },
    { label: 'Mis Reservas', icon: 'ticket', path: '/mis-reservas', roles: ['ADMIN', 'ORGANIZADOR', 'CLIENTE'] },
    { label: 'Artistas', icon: 'users', path: '/artistas', roles: ['ADMIN', 'ORGANIZADOR', 'CLIENTE'] },
    { label: 'Panel Organizador', icon: 'briefcase', path: '/organizador', roles: ['ORGANIZADOR'] },
    { label: 'Administración', icon: 'settings', path: '/admin', roles: ['ADMIN'] },
    { label: 'Configuración', icon: 'settings', path: '/settings', roles: ['ADMIN', 'ORGANIZADOR', 'CLIENTE'] },
  ].filter((item) => item.roles.includes(rol));

  const getTitle = () => {
    if (location.pathname === '/') return 'Calendario de Eventos';
    if (location.pathname === '/mis-reservas') return 'Mis Reservas';
    if (location.pathname === '/artistas') return 'Artistas';
    if (location.pathname === '/settings') return 'Configuración';
    if (location.pathname === '/admin') return 'Administración';
    if (location.pathname === '/organizador') return 'Panel Organizador';
    if (location.pathname.startsWith('/eventos/')) return 'Detalle del Evento';
    return 'Panel Principal';
  };

  return (
    <div className="flex min-h-screen bg-[#050505] font-sans text-slate-200">
      {menuOpen && (
        <button
          aria-label="Cerrar menú"
          className="fixed inset-0 z-30 bg-black/60 backdrop-blur-sm lg:hidden"
          onClick={() => setMenuOpen(false)}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-[260px] flex-col border-r border-white/5 bg-[#0a0a0a]/90 px-5 py-7 backdrop-blur-xl transition-transform duration-300 lg:translate-x-0 ${
          menuOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="mb-10 flex items-center justify-between px-2">
          <Link to="/" className="flex items-center gap-3" onClick={() => setMenuOpen(false)}>
            <img src="/logo.svg" alt="CulturaVibe" className="h-10 w-10 rounded-xl shadow-[0_0_15px_rgba(217,70,239,0.5)]" />
            <span className="text-xl font-bold tracking-tight text-white">CulturaVibe</span>
          </Link>
          <button onClick={() => setMenuOpen(false)} className="rounded-lg p-1 text-slate-400 hover:text-white lg:hidden">
            <Icon name="x" />
          </button>
        </div>

        <nav className="flex-1 space-y-2">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setMenuOpen(false)}
                className={`group flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-white/10 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.1)]'
                    : 'text-slate-400 hover:bg-white/5 hover:text-white'
                }`}
              >
                <span className={isActive ? 'text-fuchsia-400' : 'text-slate-500 transition-colors group-hover:text-fuchsia-400'}>
                  <Icon name={item.icon} size={20} />
                </span>
                {item.label}
              </Link>
            );
          })}
        </nav>

        <p className="px-2 pt-4 text-xs text-slate-600">Proyecto Final · Equipo 13</p>
      </aside>

      <div className="flex min-h-screen min-w-0 flex-1 flex-col lg:pl-[260px]">
        <header className="sticky top-0 z-20 flex h-[80px] items-center justify-between border-b border-white/5 bg-[#050505]/80 px-5 backdrop-blur-xl sm:px-8">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMenuOpen(true)}
              className="rounded-xl border border-white/10 p-2 text-slate-300 transition-colors hover:bg-white/5 lg:hidden"
            >
              <Icon name="menu" />
            </button>
            <h1 className="text-xl font-bold tracking-[-.02em] text-white">{getTitle()}</h1>
          </div>

          {user ? (
            <div className="flex items-center gap-3 sm:gap-4">
              <div className="hidden text-right sm:block">
                <p className="text-sm font-semibold text-white">{user.nombre}</p>
                <p className="text-[11px] text-slate-400">{user.email}</p>
                <p className="flex items-center justify-end gap-1 text-[11px] font-bold uppercase tracking-wider text-fuchsia-400">
                  {rol === 'ADMIN' && <Icon name="star" size={12} />}
                  {roleLabel(rol)}
                </p>
              </div>
              <span
                className="grid h-10 w-10 shrink-0 place-items-center rounded-full border border-fuchsia-500/50 bg-gradient-to-br from-violet-600 to-fuchsia-600 text-sm font-bold text-white shadow-lg"
                title={`${user.nombre} · ${user.email}`}
              >
                {iniciales(user.nombre)}
              </span>
              <button
                onClick={handleLogout}
                disabled={loggingOut}
                title="Cerrar sesión"
                className="rounded-xl p-2 text-slate-400 transition hover:bg-red-500/10 hover:text-red-400 disabled:opacity-50"
              >
                <Icon name="logout" size={20} />
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link
                to="/login"
                className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-bold text-white transition hover:bg-white/10"
              >
                Iniciar sesión
              </Link>
              <Link
                to="/registro"
                className="hidden rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-4 py-2 text-sm font-bold text-white transition hover:opacity-90 sm:block"
              >
                Crear cuenta
              </Link>
            </div>
          )}
        </header>
        <main className="flex-1 overflow-y-auto p-5 sm:p-8 lg:p-10">{children}</main>
      </div>
    </div>
  );
}
