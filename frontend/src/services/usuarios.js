// Usuarios del Proyecto Final (solo ADMIN).
import api from './api';

export const fetchUsuarios = async ({ page = 0, size = 12 } = {}) => {
  const { data } = await api.get('/api/usuarios', { params: { page, size } });
  return {
    usuarios: data.content || [],
    total: data.totalElements ?? 0,
    totalPages: data.totalPages ?? 1,
    page: data.number ?? page,
  };
};

export const fetchUsuarioById = async (id) => {
  const { data } = await api.get(`/api/usuarios/${id}`);
  return data;
};

export const updateUsuario = async (id, usuario) => {
  const { data } = await api.put(`/api/usuarios/${id}`, usuario);
  return data;
};

export const deleteUsuario = async (id) => {
  await api.delete(`/api/usuarios/${id}`);
};
