// Categorías del Proyecto Final.
import api from './api';

export const fetchCategorias = async () => {
  const { data } = await api.get('/api/categorias');
  return Array.isArray(data) ? data : data.content || [];
};

export const createCategoria = async (categoria) => {
  const { data } = await api.post('/api/categorias', categoria);
  return data;
};

export const updateCategoria = async (id, categoria) => {
  const { data } = await api.put(`/api/categorias/${id}`, categoria);
  return data;
};

export const deleteCategoria = async (id) => {
  await api.delete(`/api/categorias/${id}`);
};
