// Firebase Auth (login con Google por Firebase y verificaciones).
// Se inicializa solo si las 6 variables VITE_FIREBASE_* están en el
// .env (proyecto culturavibe-60142, el mismo de la app de la Actividad
// 8). Sin apiKey, Firebase no se inicializa y los botones avisan con
// un mensaje claro en vez de romperse.
import { initializeApp, getApps } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  createUserWithEmailAndPassword,
  sendEmailVerification,
  signInWithPhoneNumber,
  linkWithPhoneNumber,
  RecaptchaVerifier,
} from 'firebase/auth';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

export const firebaseConfigurado = Boolean(firebaseConfig.apiKey);

let app = null;
if (firebaseConfigurado) {
  app = getApps().length > 0 ? getApps()[0] : initializeApp(firebaseConfig);
}

export const authFirebase = app ? getAuth(app) : null;

// Entra con Google por Firebase y devuelve el ID token de Firebase,
// que el backend verifica y cambia por el JWT propio de la app.
export const entrarConGoogleFirebase = async () => {
  if (!firebaseConfigurado || !authFirebase) {
    throw new Error('Firebase no está configurado en esta aplicación');
  }
  const proveedor = new GoogleAuthProvider();
  const resultado = await signInWithPopup(authFirebase, proveedor);
  return resultado.user.getIdToken();
};

// Crea el usuario en Firebase Auth con el mismo correo y contraseña
// del registro y le manda el correo de verificación de Firebase.
// Nunca rompe el registro: si Firebase no está configurado o la
// creación falla (por ejemplo el correo ya existe en Firebase), solo
// queda un aviso en consola y devuelve null.
export const crearUsuarioFirebase = async (email, password) => {
  if (!firebaseConfigurado || !authFirebase) {
    console.warn('Firebase no está configurado; no se creó el usuario en Firebase Auth.');
    return null;
  }
  try {
    const credencial = await createUserWithEmailAndPassword(authFirebase, email, password);
    try {
      await sendEmailVerification(credencial.user);
    } catch (e) {
      console.warn('No se pudo enviar el correo de verificación de Firebase:', e);
    }
    return credencial.user;
  } catch (e) {
    console.warn('No se pudo crear el usuario en Firebase Auth:', e);
    return null;
  }
};

// Recarga el usuario de Firebase y devuelve si su correo ya está
// verificado y un ID token fresco (o null si no hay usuario con
// sesión en Firebase en este navegador).
export const recargarUsuarioFirebase = async () => {
  if (!firebaseConfigurado || !authFirebase?.currentUser) return null;
  const usuario = authFirebase.currentUser;
  await usuario.reload();
  return {
    emailVerificado: usuario.emailVerified,
    idToken: await usuario.getIdToken(true),
  };
};

// Normaliza un teléfono a E.164: quita espacios, guiones y
// paréntesis; un número mexicano de 10 dígitos sin prefijo lleva +52.
export const normalizarTelefonoE164 = (telefono) => {
  const limpio = String(telefono || '').replace(/[\s\-().]/g, '');
  if (!limpio) return '';
  if (limpio.startsWith('+')) return limpio;
  if (limpio.length === 12 && limpio.startsWith('52')) return `+${limpio}`;
  if (limpio.length === 10) return `+52${limpio}`;
  return limpio;
};

// reCAPTCHA invisible para el SMS de Firebase (Phone Auth). Se crea
// al pedir el código y se limpia antes de crear otro o tras confirmar.
let verificadorRecaptcha = null;

const obtenerVerificadorRecaptcha = () => {
  if (verificadorRecaptcha) {
    verificadorRecaptcha.clear();
    verificadorRecaptcha = null;
  }
  verificadorRecaptcha = new RecaptchaVerifier(authFirebase, 'recaptcha-container', {
    size: 'invisible',
  });
  return verificadorRecaptcha;
};

export const limpiarVerificadorRecaptcha = () => {
  if (verificadorRecaptcha) {
    try {
      verificadorRecaptcha.clear();
    } catch {
      // Si ya se limpió, no pasa nada.
    }
    verificadorRecaptcha = null;
  }
};

// Manda el código por SMS con Firebase Phone Auth y devuelve el
// resultado de confirmación. Si hay un usuario de Firebase con sesión
// (el que se creó al registrarse), el teléfono se vincula a esa
// cuenta; si no, se inicia sesión solo con el teléfono.
export const enviarCodigoTelefonoFirebase = async (telefonoE164) => {
  if (!firebaseConfigurado || !authFirebase) {
    throw new Error('Firebase no está configurado en esta aplicación');
  }
  const verificador = obtenerVerificadorRecaptcha();
  if (authFirebase.currentUser) {
    try {
      return await linkWithPhoneNumber(authFirebase.currentUser, telefonoE164, verificador);
    } catch (e) {
      // Si el teléfono ya está vinculado a otra cuenta de Firebase,
      // se inicia sesión solo con el teléfono.
      if (e?.code !== 'auth/credential-already-in-use') throw e;
      limpiarVerificadorRecaptcha();
      const verificadorNuevo = obtenerVerificadorRecaptcha();
      return await signInWithPhoneNumber(authFirebase, telefonoE164, verificadorNuevo);
    }
  }
  return await signInWithPhoneNumber(authFirebase, telefonoE164, verificador);
};

// Confirma el código del SMS y devuelve un ID token fresco de
// Firebase (con el correo y el teléfono ya verificados).
export const confirmarCodigoTelefonoFirebase = async (confirmacion, codigo) => {
  const credencial = await confirmacion.confirm(codigo);
  limpiarVerificadorRecaptcha();
  return credencial.user.getIdToken(true);
};
