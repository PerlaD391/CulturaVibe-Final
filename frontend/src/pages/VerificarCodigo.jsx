import { useState } from 'react';
import { apiErrorMessage } from '../services/api';
import { verificarCodigo, reenviarCodigo, verificarEmailFirebase, verificarTelefonoFirebase } from '../services/auth';
import {
  firebaseConfigurado,
  recargarUsuarioFirebase,
  normalizarTelefonoE164,
  enviarCodigoTelefonoFirebase,
  confirmarCodigoTelefonoFirebase,
} from '../services/firebase';
import FormField, { inputClass } from '../components/FormField';
import Icon from '../components/Icon';

// Pantalla del código de verificación después de registrarse. El
// código de 6 dígitos llega por correo y, si el usuario dio
// teléfono y el SMS está configurado, también por SMS. Además, si
// Firebase está configurado, el correo se puede verificar con el
// enlace de Firebase y el teléfono con un SMS de Firebase (Phone
// Auth). La verificación confirma el contacto, no es un candado: se
// puede omitir y entrar al catálogo ("Hacerlo después").
export default function VerificarCodigo({ email, telefono, onVerificado, onOmitir }) {
  const [codigo, setCodigo] = useState('');
  const [touched, setTouched] = useState(false);
  const [mensaje, setMensaje] = useState('');
  const [error, setError] = useState('');
  const [verificando, setVerificando] = useState(false);
  const [reenviando, setReenviando] = useState(false);
  const [verificandoFirebase, setVerificandoFirebase] = useState(false);
  const [telefonoSms, setTelefonoSms] = useState(telefono || '');
  const [codigoSms, setCodigoSms] = useState('');
  const [confirmacionSms, setConfirmacionSms] = useState(null);
  const [enviandoSms, setEnviandoSms] = useState(false);
  const [confirmandoSms, setConfirmandoSms] = useState(false);
  const [mensajeSms, setMensajeSms] = useState('');
  const [errorSms, setErrorSms] = useState('');

  const codigoError = () => {
    if (!codigo) return 'Escribe el código de 6 dígitos.';
    if (!/^\d{6}$/.test(codigo)) return 'El código debe tener 6 dígitos.';
    return '';
  };

  const handleVerificar = async (e) => {
    e.preventDefault();
    setTouched(true);
    setMensaje('');
    setError('');
    if (codigoError()) return;
    setVerificando(true);
    try {
      const msg = await verificarCodigo({ email, codigo });
      setMensaje(msg);
      setTimeout(onVerificado, 1500);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo verificar el código.'));
    } finally {
      setVerificando(false);
    }
  };

  const handleReenviar = async () => {
    setMensaje('');
    setError('');
    setReenviando(true);
    try {
      const msg = await reenviarCodigo({ email });
      setMensaje(msg);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo reenviar el código.'));
    } finally {
      setReenviando(false);
    }
  };

  // Verificación de correo con Firebase: el usuario ya abrió el
  // enlace que Firebase le mandó por correo; se recarga su estado y
  // se sincroniza email_verificado en el backend.
  const handleVerificarCorreoFirebase = async () => {
    setMensaje('');
    setError('');
    setVerificandoFirebase(true);
    try {
      const estado = await recargarUsuarioFirebase();
      if (!estado) {
        setError('No hay una cuenta de Firebase en este navegador. Verifica el correo con el código de 6 dígitos.');
        return;
      }
      if (!estado.emailVerificado) {
        setError('Tu correo aún no está verificado en Firebase. Abre el enlace que te llegó por correo y luego vuelve a intentarlo.');
        return;
      }
      const msg = await verificarEmailFirebase(estado.idToken);
      setMensaje(msg);
      setTimeout(onVerificado, 1500);
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo verificar el correo con Firebase.'));
    } finally {
      setVerificandoFirebase(false);
    }
  };

  // Verificación de teléfono con Firebase Phone Auth: manda el código
  // por SMS al número que escribe el usuario.
  const handleEnviarSmsFirebase = async () => {
    setMensajeSms('');
    setErrorSms('');
    const numero = normalizarTelefonoE164(telefonoSms);
    if (!/^\+\d{10,15}$/.test(numero)) {
      setErrorSms('Escribe un número válido con 10 dígitos (por ejemplo 55 1234 5678).');
      return;
    }
    setEnviandoSms(true);
    try {
      const confirmacion = await enviarCodigoTelefonoFirebase(numero);
      setConfirmacionSms(confirmacion);
      setMensajeSms('Te enviamos un código por SMS. Escríbelo abajo.');
    } catch (err) {
      setErrorSms(apiErrorMessage(err, 'No se pudo enviar el SMS con Firebase.'));
    } finally {
      setEnviandoSms(false);
    }
  };

  // Confirma el código del SMS de Firebase y sincroniza
  // telefono_verificado en el backend.
  const handleConfirmarSmsFirebase = async (e) => {
    e.preventDefault();
    setMensajeSms('');
    setErrorSms('');
    if (!/^\d{6}$/.test(codigoSms)) {
      setErrorSms('El código del SMS debe tener 6 dígitos.');
      return;
    }
    if (!confirmacionSms) {
      setErrorSms('Primero pide el código por SMS.');
      return;
    }
    setConfirmandoSms(true);
    try {
      const idToken = await confirmarCodigoTelefonoFirebase(confirmacionSms, codigoSms);
      const msg = await verificarTelefonoFirebase(idToken, email);
      setMensajeSms(msg);
      setTimeout(onVerificado, 1500);
    } catch (err) {
      setErrorSms(apiErrorMessage(err, 'No se pudo confirmar el código del SMS.'));
    } finally {
      setConfirmandoSms(false);
    }
  };

  return (
    <div>
      <div className="mb-8 text-center">
        <img src="/logo.svg" alt="CulturaVibe" className="mx-auto mb-4 h-14 w-14 rounded-2xl shadow-[0_0_20px_rgba(139,92,246,0.4)]" />
        <h1 className="text-[29px] font-bold tracking-[-.035em] text-white">Verifica tu correo</h1>
        <p className="mt-2 text-sm leading-relaxed text-slate-400">
          Te enviamos un código de 6 dígitos a <span className="font-semibold text-slate-200">{email}</span>
          {telefono ? (
            <> y por SMS al <span className="font-semibold text-slate-200">{telefono}</span>.</>
          ) : (
            '.'
          )}
          {' '}Es válido por 15 minutos.
        </p>
      </div>

      <form onSubmit={handleVerificar} noValidate className="space-y-4">
        <FormField label="Código de verificación" error={touched ? codigoError() : ''}>
          <input
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="000000"
            value={codigo}
            onChange={(e) => setCodigo(e.target.value.replace(/\D/g, '').slice(0, 6))}
            onBlur={() => setTouched(true)}
            className={`${inputClass(Boolean(touched && codigoError()))} text-center text-xl font-bold tracking-[0.5em]`}
          />
        </FormField>

        {mensaje && <p className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-center text-sm font-medium text-emerald-300">{mensaje}</p>}
        {error && <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-center text-sm font-medium text-red-300">{error}</p>}

        <button
          disabled={verificando}
          type="submit"
          className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3.5 text-sm font-bold text-white shadow-[0_0_15px_rgba(139,92,246,0.4)] transition hover:opacity-90 disabled:opacity-50"
        >
          {verificando ? 'Verificando...' : 'Verificar código'} <Icon name="check" size={18} />
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-slate-400">
        ¿No llegó?{' '}
        <button type="button" disabled={reenviando} onClick={handleReenviar} className="font-semibold text-fuchsia-400 hover:text-fuchsia-300 disabled:opacity-50">
          {reenviando ? 'Enviando...' : 'Reenviar código'}
        </button>
      </p>

      {firebaseConfigurado && (
        <p className="mt-4 text-center text-sm text-slate-400">
          ¿Te llegó el correo de verificación de Firebase? Ábrelo y luego toca{' '}
          <button type="button" disabled={verificandoFirebase} onClick={handleVerificarCorreoFirebase} className="font-semibold text-fuchsia-400 hover:text-fuchsia-300 disabled:opacity-50">
            {verificandoFirebase ? 'Revisando...' : 'Ya verifiqué mi correo'}
          </button>
        </p>
      )}

      <div className="mt-8 border-t border-white/10 pt-6">
        <h2 className="text-center text-base font-bold text-white">Verificar mi teléfono por SMS</h2>
        <p className="mt-1 text-center text-sm text-slate-400">
          También puedes verificar tu teléfono con un código por SMS de Firebase.
        </p>
        {firebaseConfigurado ? (
          <form onSubmit={handleConfirmarSmsFirebase} noValidate className="mt-4 space-y-4">
            <FormField label="Teléfono">
              <input
                type="tel"
                placeholder="Ej. 55 1234 5678"
                value={telefonoSms}
                onChange={(e) => setTelefonoSms(e.target.value)}
                className={inputClass(false)}
              />
            </FormField>
            <button
              type="button"
              disabled={enviandoSms}
              onClick={handleEnviarSmsFirebase}
              className="flex w-full items-center justify-center gap-2 rounded-xl border border-violet-500/40 bg-violet-600/20 px-5 py-3 text-sm font-bold text-violet-200 transition hover:bg-violet-600/30 disabled:opacity-50"
            >
              {enviandoSms ? 'Enviando SMS...' : 'Enviar código por SMS'}
            </button>
            {confirmacionSms && (
              <>
                <FormField label="Código del SMS" error={codigoSms && !/^\d{6}$/.test(codigoSms) ? 'El código del SMS debe tener 6 dígitos.' : ''}>
                  <input
                    type="text"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={6}
                    placeholder="000000"
                    value={codigoSms}
                    onChange={(e) => setCodigoSms(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    className={`${inputClass(Boolean(codigoSms && !/^\d{6}$/.test(codigoSms)))} text-center text-xl font-bold tracking-[0.5em]`}
                  />
                </FormField>
                <button
                  disabled={confirmandoSms}
                  type="submit"
                  className="flex w-full items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-fuchsia-600 px-5 py-3.5 text-sm font-bold text-white shadow-[0_0_15px_rgba(139,92,246,0.4)] transition hover:opacity-90 disabled:opacity-50"
                >
                  {confirmandoSms ? 'Confirmando...' : 'Confirmar código del SMS'} <Icon name="check" size={18} />
                </button>
              </>
            )}
            {mensajeSms && <p className="rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-center text-sm font-medium text-emerald-300">{mensajeSms}</p>}
            {errorSms && <p className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-center text-sm font-medium text-red-300">{errorSms}</p>}
          </form>
        ) : (
          <p className="mt-4 rounded-xl border border-white/10 bg-black/30 px-4 py-3 text-center text-sm text-slate-400">
            La verificación por SMS con Firebase no está configurada en esta aplicación. Puedes usar el código de 6 dígitos de arriba.
          </p>
        )}
        {/* Contenedor del reCAPTCHA invisible de Firebase Phone Auth. */}
        <div id="recaptcha-container" />
      </div>

      <p className="mt-4 text-center text-sm text-slate-400">
        <button type="button" onClick={onOmitir} className="font-semibold text-slate-300 hover:text-white">
          Hacerlo después
        </button>
      </p>
    </div>
  );
}
