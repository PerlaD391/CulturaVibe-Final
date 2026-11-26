// Firebase Auth (login con Google por Firebase). Se inicializa solo
// si las 6 variables VITE_FIREBASE_* están en el .env (proyecto
// culturavibe-60142, el mismo de la app de la Actividad 8). Sin
// apiKey, Firebase no se inicializa y el botón avisa con un mensaje
// claro en vez de romperse.
import { initializeApp, getApps } from 'firebase/app';
import { getAuth, GoogleAuthProvider, signInWithPopup } from 'firebase/auth';

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
