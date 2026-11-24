// Validaciones del Proyecto Final. Todos los mensajes se muestran
// DEBAJO del campo correspondiente, nunca con alert() ni confirm().
// Política de contraseña (la pide el proyecto): mínimo 8 caracteres,
// una mayúscula, un número y un carácter especial.

export const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

export const isValidEmail = (value) => EMAIL_RE.test(String(value || '').trim());

export const passwordChecks = (password) => {
  const value = password || '';
  return {
    length: value.length >= 8,
    upper: /[A-Z]/.test(value),
    number: /[0-9]/.test(value),
    special: /[^A-Za-z0-9]/.test(value),
  };
};

export const isValidPassword = (password) => {
  const checks = passwordChecks(password);
  return checks.length && checks.upper && checks.number && checks.special;
};

export const passwordError = (password) => {
  const checks = passwordChecks(password);
  if (!password) return 'Escribe una contraseña.';
  if (!checks.length) return 'Mínimo 8 caracteres.';
  if (!checks.upper) return 'Debe tener al menos una letra mayúscula.';
  if (!checks.number) return 'Debe tener al menos un número.';
  if (!checks.special) return 'Debe tener al menos un carácter especial (por ejemplo !, @, #).';
  return '';
};

export const requiredError = (value, label = 'Este campo') =>
  String(value || '').trim() ? '' : `${label} es obligatorio.`;

export const emailError = (value) => {
  if (!String(value || '').trim()) return 'El correo es obligatorio.';
  if (!isValidEmail(value)) return 'Escribe un correo válido, por ejemplo tu@correo.com.';
  return '';
};
