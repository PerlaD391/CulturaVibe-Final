// Ladas (códigos de país) para el teléfono. El número se guarda en
// el backend en formato E.164 (lada + número, por ejemplo
// +529515928701). Antes el teléfono se guardaba sin lada y el backend
// asumía +52, lo que rompía el envío de WhatsApp y SMS a números que
// no son de México.

export const LADAS = [
  { codigo: '+52', pais: 'México' },
  { codigo: '+1', pais: 'Estados Unidos' },
  { codigo: '+1', pais: 'Canadá' },
  { codigo: '+34', pais: 'España' },
  { codigo: '+57', pais: 'Colombia' },
  { codigo: '+54', pais: 'Argentina' },
  { codigo: '+55', pais: 'Brasil' },
  { codigo: '+56', pais: 'Chile' },
  { codigo: '+51', pais: 'Perú' },
  { codigo: '+58', pais: 'Venezuela' },
  { codigo: '+593', pais: 'Ecuador' },
  { codigo: '+502', pais: 'Guatemala' },
];

// Valor del select cuando el usuario escribe otra lada.
export const LADA_OTRA = 'OTRA';

export const LADA_POR_DEFECTO = '+52';

// Une la lada elegida con el número escrito, en E.164. Si el número
// ya trae la lada al inicio, no se duplica.
export const unirLada = (lada, numero) => {
  const digitos = String(numero || '').replace(/\D/g, '');
  if (!digitos) return '';
  const codigo = String(lada || LADA_POR_DEFECTO).replace(/\D/g, '');
  if (!codigo) return `+${digitos}`;
  const sinLada =
    digitos.length > 10 && digitos.startsWith(codigo) ? digitos.slice(codigo.length) : digitos;
  return `+${codigo}${sinLada}`;
};

// Separa un teléfono guardado en E.164 en { lada, numero, codigoOtro }
// para editarlo. Si no trae '+', se asume la lada de México (así se
// guardaban los teléfonos antes de este cambio). Si la lada no está en
// la lista, se devuelve como "Otra" con su código.
export const separarLada = (telefono) => {
  const limpio = String(telefono || '').replace(/[\s\-().]/g, '');
  if (!limpio) return { lada: LADA_POR_DEFECTO, numero: '', codigoOtro: '' };
  if (!limpio.startsWith('+')) {
    return { lada: LADA_POR_DEFECTO, numero: limpio.replace(/\D/g, ''), codigoOtro: '' };
  }
  const digitos = limpio.slice(1);
  const codigos = [...new Set(LADAS.map((l) => l.codigo.slice(1)))].sort((a, b) => b.length - a.length);
  for (const codigo of codigos) {
    if (digitos.startsWith(codigo) && digitos.length - codigo.length >= 7) {
      return { lada: `+${codigo}`, numero: digitos.slice(codigo.length), codigoOtro: '' };
    }
  }
  return { lada: LADA_OTRA, numero: digitos.slice(3), codigoOtro: `+${digitos.slice(0, 3)}` };
};
