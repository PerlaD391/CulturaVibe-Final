import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { getStoredAuth, clearStoredAuth } from '../services/api';
import * as authService from '../services/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(() => getStoredAuth());
  const [loading, setLoading] = useState(false);

  // Si otra pestaña cierra la sesión, esta también se entera.
  useEffect(() => {
    const onStorage = (e) => {
      if (e.key === 'culturavibe-auth') setAuth(getStoredAuth());
    };
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, []);

  const login = useCallback(async ({ email, password }) => {
    setLoading(true);
    try {
      const next = await authService.login({ email, password });
      setAuth(next);
      return next;
    } finally {
      setLoading(false);
    }
  }, []);

  // Sesión que llega con el JWT en la URL (login con Google).
  const loginConToken = useCallback(async (token) => {
    setLoading(true);
    try {
      const next = await authService.loginConToken(token);
      setAuth(next);
      return next;
    } finally {
      setLoading(false);
    }
  }, []);

  const register = useCallback(async ({ nombre, email, password, telefono, rol }) => {
    setLoading(true);
    try {
      const next = await authService.register({ nombre, email, password, telefono, rol });
      setAuth(next);
      return next;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    setAuth(null);
  }, []);

  const updateProfile = useCallback((patch) => {
    setAuth((prev) => {
      if (!prev) return prev;
      const next = { ...prev, ...patch };
      try {
        localStorage.setItem('culturavibe-auth', JSON.stringify(next));
      } catch {
        // Si no se puede guardar, la sesión en memoria sigue válida.
      }
      return next;
    });
  }, []);

  const value = {
    user: auth,
    token: auth?.token || null,
    rol: auth?.rol || null,
    isAuthenticated: Boolean(auth?.token),
    loading,
    login,
    loginConToken,
    register,
    logout,
    updateProfile,
    clearLocal: () => {
      clearStoredAuth();
      setAuth(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>.');
  return ctx;
}
