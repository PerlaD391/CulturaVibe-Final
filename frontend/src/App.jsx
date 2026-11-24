import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Layout from './components/Layout';
import Spinner from './components/Spinner';
import Login from './pages/Login';

const Register = lazy(() => import('./pages/Register'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'));
const ResetPassword = lazy(() => import('./pages/ResetPassword'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Catalog = lazy(() => import('./pages/Catalog'));
const EventDetail = lazy(() => import('./pages/EventDetail'));
const MisReservas = lazy(() => import('./pages/MisReservas'));
const Artists = lazy(() => import('./pages/Artists'));
const Settings = lazy(() => import('./pages/Settings'));
const Admin = lazy(() => import('./pages/Admin'));
const Organizador = lazy(() => import('./pages/Organizador'));

// Ruta que exige sesión: sin token, redirige a /login guardando a dónde
// se quería entrar.
function RequireAuth({ children }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />;
  return children;
}

// Ruta que exige un rol: sin el rol, redirige al inicio.
function RequireRole({ roles, children }) {
  const { isAuthenticated, rol } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) return <Navigate to="/login" state={{ from: location }} replace />;
  if (!roles.includes(rol)) return <Navigate to="/" replace />;
  return children;
}

// Páginas públicas (catálogo, detalle, artistas) con la barra de
// navegación; las internas, con el menú lateral completo.
function PublicLayout({ children }) {
  return <Layout>{children}</Layout>;
}

function PrivateLayout({ children }) {
  return <Layout>{children}</Layout>;
}

function AppRoutes() {
  return (
    <Suspense fallback={<Spinner texto="Cargando CulturaVibe..." className="min-h-screen bg-[#050505]" />}>
      <Routes>
        {/* Públicas */}
        <Route path="/" element={<PublicLayout><Catalog /></PublicLayout>} />
        <Route path="/eventos/:id" element={<PublicLayout><EventDetail /></PublicLayout>} />
        <Route path="/artistas" element={<PublicLayout><Artists /></PublicLayout>} />
        <Route path="/login" element={<Login />} />
        <Route path="/registro" element={<Register />} />
        <Route path="/recuperar" element={<ForgotPassword />} />
        <Route path="/restablecer" element={<ResetPassword />} />

        {/* Internas (con sesión) */}
        <Route path="/dashboard" element={<RequireAuth><PrivateLayout><Dashboard /></PrivateLayout></RequireAuth>} />
        <Route path="/mis-reservas" element={<RequireAuth><PrivateLayout><MisReservas /></PrivateLayout></RequireAuth>} />
        <Route path="/settings" element={<RequireAuth><PrivateLayout><Settings /></PrivateLayout></RequireAuth>} />

        {/* Por rol */}
        <Route path="/admin" element={<RequireRole roles={['ADMIN']}><PrivateLayout><Admin /></PrivateLayout></RequireRole>} />
        <Route path="/organizador" element={<RequireRole roles={['ORGANIZADOR']}><PrivateLayout><Organizador /></PrivateLayout></RequireRole>} />

        {/* Rutas viejas: redirigen a las nuevas */}
        <Route path="/catalog" element={<Navigate to="/" replace />} />
        <Route path="/event/:id" element={<Navigate to="/eventos/:id" replace />} />
        <Route path="/mis-eventos" element={<Navigate to="/mis-reservas" replace />} />
        <Route path="/artists" element={<Navigate to="/artistas" replace />} />
        <Route path="/verify" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  );
}
