package mx.culturavibe.proyectofinal.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import mx.culturavibe.proyectofinal.modelo.Evento;
import mx.culturavibe.proyectofinal.modelo.Notificacion;
import mx.culturavibe.proyectofinal.modelo.Usuario;
import mx.culturavibe.proyectofinal.repositorio.NotificacionRepository;

// SMS por Vonage (primero) o Twilio (respaldo) y WhatsApp por Meta
// WhatsApp Cloud API (ver WhatsAppCloudService) o, de respaldo, por el
// sandbox de Twilio (ver TwilioWhatsAppService), que es gratis. Cada
// canal se envía solo si está configurado: SMS necesita VONAGE_API_KEY,
// VONAGE_API_SECRET y VONAGE_FROM_NUMBER, o bien TWILIO_ACCOUNT_SID,
// TWILIO_AUTH_TOKEN y TWILIO_PHONE_NUMBER; WhatsApp necesita
// WHATSAPP_PHONE_NUMBER_ID y WHATSAPP_ACCESS_TOKEN (Meta) o
// TWILIO_ACCOUNT_SID y TWILIO_AUTH_TOKEN (sandbox). Twilio trial no
// deja comprar número sin upgrade, por eso Vonage va primero. La
// cuenta de Twilio es de prueba y solo permite plantillas de
// contenido: el código de verificación se manda con la plantilla
// TWILIO_SMS_CONTENT_SID (SMS) y TWILIO_WHATSAPP_CONTENT_SID
// (WhatsApp), cada una con una variable {{1}} para el código; si no
// hay ContentSid, se manda texto libre como respaldo. Si un
// canal no está configurado, se registra un warn y se sigue; la reserva
// se crea igual. Cada intento (enviado o fallido) queda guardado en
// notificaciones.
@Service
public class NotificacionService {

	private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

	private final NotificacionRepository notificaciones;

	private final VonageSmsService vonage;

	private final WhatsAppCloudService whatsapp;

	private final TwilioWhatsAppService twilioWhatsapp;

	@Value("${app.twilio.account-sid:}")
	private String accountSid;

	@Value("${app.twilio.auth-token:}")
	private String authToken;

	@Value("${app.twilio.phone-number:}")
	private String numeroTwilio;

	// Plantilla de contenido para el SMS con el código de
	// verificación. La cuenta de Twilio es de prueba y solo permite
	// plantillas para SMS (el texto libre da "Invalid template
	// name"). Si está vacío, el código se manda como texto libre.
	@Value("${app.twilio.sms-content-sid:}")
	private String smsContentSid;

	private boolean inicializado = false;

	public NotificacionService(NotificacionRepository notificaciones, VonageSmsService vonage,
			WhatsAppCloudService whatsapp, TwilioWhatsAppService twilioWhatsapp) {
		this.notificaciones = notificaciones;
		this.vonage = vonage;
		this.whatsapp = whatsapp;
		this.twilioWhatsapp = twilioWhatsapp;
	}

	// Se llama al crear una reserva: SMS por Vonage o Twilio y WhatsApp por
	// Cloud API al teléfono del usuario. Cada canal es independiente: si
	// uno falla o no está configurado, el otro se intenta igual.
	public void enviarConfirmacionReserva(Usuario usuario, Evento evento, int cantidad) {
		if (usuario.getTelefono() == null || usuario.getTelefono().isBlank()) {
			log.warn("El usuario {} no tiene teléfono; no se envía notificación", usuario.getEmail());
			return;
		}
		String telefono = usuario.getTelefono().replace(" ", "");
		String texto = "CulturaVibe: tu reserva para '" + evento.getTitulo() + "' ("
				+ cantidad + (cantidad == 1 ? " boleto" : " boletos") + ") está confirmada. ¡Nos vemos ahí!";
		enviarSms(usuario, telefono, texto);
		enviarWhatsapp(usuario, telefono, texto);
	}

	// Código de verificación por SMS y por WhatsApp al registrarse.
	// Devuelve true si se intentó el envío por algún canal (hay
	// teléfono y al menos un canal configurado); si no hay teléfono o
	// no hay ningún canal, solo queda el warn y el registro sigue solo
	// con el correo. Nunca lanza excepciones. El código es el mismo en
	// todos los canales y su expiración no cambia.
	public boolean enviarCodigoVerificacion(Usuario usuario, String codigo) {
		if (usuario.getTelefono() == null || usuario.getTelefono().isBlank()) {
			log.warn("El usuario {} no tiene teléfono; el código de verificación solo va por correo",
					usuario.getEmail());
			return false;
		}
		String telefono = usuario.getTelefono().replace(" ", "");
		String texto = "CulturaVibe: tu código de verificación es " + codigo + ". Válido por 15 minutos.";
		boolean enviado = false;
		if (vonage.configurado() || twilioConfigurado()) {
			enviarSms(usuario, telefono, texto, codigo);
			enviado = true;
		} else {
			log.warn("Ni Vonage ni Twilio están configurados; el código de verificación de {} no va por SMS",
					usuario.getEmail());
		}
		// WhatsApp: el mismo código. Por Meta va con la plantilla de
		// código (WHATSAPP_TEMPLATE_CODIGO); por el sandbox de Twilio
		// va como texto libre. Si ningún WhatsApp está configurado o
		// la plantilla no existe, solo queda el warn y el código sigue
		// llegando por correo y SMS.
		if (enviarCodigoPorWhatsapp(usuario, telefono, codigo, texto)) {
			enviado = true;
		}
		if (!enviado) {
			log.warn("El código de verificación de {} solo va por correo", usuario.getEmail());
		}
		return enviado;
	}

	// El SMS sale por Vonage si está configurado; si no, por Twilio si
	// está configurado; si ninguno lo está, solo queda el warn y la
	// reserva se crea igual. El código (null para la confirmación de
	// reserva) es el código de 6 dígitos: si viene y hay plantilla de
	// contenido configurada, el SMS por Twilio se manda con la
	// plantilla en vez de texto libre.
	private void enviarSms(Usuario usuario, String telefono, String texto) {
		enviarSms(usuario, telefono, texto, null);
	}

	private void enviarSms(Usuario usuario, String telefono, String texto, String codigo) {
		if (vonage.configurado()) {
			if (vonage.enviarSms(telefono, texto)) {
				guardar(usuario, "SMS", texto, "ENVIADA");
			} else {
				guardar(usuario, "SMS", texto, "FALLIDA");
			}
			return;
		}
		if (!twilioConfigurado()) {
			log.warn("Ni Vonage ni Twilio están configurados (faltan VONAGE_* o TWILIO_*); no se envía SMS a {}",
					usuario.getEmail());
			return;
		}
		inicializar();
		try {
			if (codigo != null && smsContentSid != null && !smsContentSid.isBlank()) {
				// Plantilla de contenido: la cuenta de prueba solo
				// permite plantillas para SMS (el texto libre da
				// "Invalid template name"). La plantilla lleva una
				// variable {{1}} con el código. El Body se ignora
				// cuando hay ContentSid.
				Message.creator(new PhoneNumber(telefono), new PhoneNumber(numeroTwilio), texto)
						.setContentSid(smsContentSid)
						.setContentVariables("{\"1\":\"" + codigo + "\"}")
						.create();
				log.info("SMS enviado por Twilio a {} (plantilla {})", telefono, smsContentSid);
			} else {
				Message.creator(new PhoneNumber(telefono), new PhoneNumber(numeroTwilio), texto).create();
				log.info("SMS enviado por Twilio a {}", telefono);
			}
			guardar(usuario, "SMS", texto, "ENVIADA");
		} catch (Throwable e) {
			// Throwable y no Exception: el SDK de Twilio usa Apache
			// HttpClient 5 y si falta su dependencia httpcore5-h2 lanza
			// NoClassDefFoundError (un Error, no una Exception). Un aviso
			// que falla nunca debe romper la reserva.
			guardar(usuario, "SMS", texto, "FALLIDA");
			log.warn("No se pudo enviar el SMS por Twilio a {}: {}", telefono, e.getMessage());
		}
	}

	// WhatsApp por Meta si está configurado; si no, por el sandbox
	// de Twilio (gratis); si ninguno lo está, solo queda el warn.
	private void enviarWhatsapp(Usuario usuario, String telefono, String texto) {
		if (whatsapp.configurado()) {
			if (whatsapp.enviarConfirmacion(telefono, texto)) {
				guardar(usuario, "WHATSAPP", texto, "ENVIADA");
			} else {
				guardar(usuario, "WHATSAPP", texto, "FALLIDA");
			}
			return;
		}
		if (twilioWhatsapp.configurado()) {
			if (twilioWhatsapp.enviarMensaje(telefono, texto)) {
				guardar(usuario, "WHATSAPP", texto, "ENVIADA");
			} else {
				guardar(usuario, "WHATSAPP", texto, "FALLIDA");
			}
			return;
		}
		log.warn("WhatsApp no está configurado (ni Meta Cloud API ni el sandbox de Twilio); no se envía WhatsApp a {}",
				usuario.getEmail());
	}

	// El código de verificación por WhatsApp: Meta si está
	// configurado, si no el sandbox de Twilio. Devuelve true si se
	// intentó el envío por algún canal.
	private boolean enviarCodigoPorWhatsapp(Usuario usuario, String telefono, String codigo, String texto) {
		if (whatsapp.configurado()) {
			if (whatsapp.enviarCodigoVerificacion(telefono, codigo)) {
				guardar(usuario, "WHATSAPP", texto, "ENVIADA");
			} else {
				guardar(usuario, "WHATSAPP", texto, "FALLIDA");
			}
			return true;
		}
		if (twilioWhatsapp.configurado()) {
			if (twilioWhatsapp.enviarCodigoVerificacion(telefono, codigo)) {
				guardar(usuario, "WHATSAPP", texto, "ENVIADA");
			} else {
				guardar(usuario, "WHATSAPP", texto, "FALLIDA");
			}
			return true;
		}
		log.warn("WhatsApp no está configurado (ni Meta Cloud API ni el sandbox de Twilio); el código de verificación de {} no va por WhatsApp",
				usuario.getEmail());
		return false;
	}

	private boolean twilioConfigurado() {
		return accountSid != null && !accountSid.isBlank()
				&& authToken != null && !authToken.isBlank()
				&& numeroTwilio != null && !numeroTwilio.isBlank();
	}

	private synchronized void inicializar() {
		if (!inicializado) {
			Twilio.init(accountSid, authToken);
			inicializado = true;
		}
	}

	private void guardar(Usuario usuario, String tipo, String mensaje, String estado) {
		try {
			notificaciones.save(new Notificacion(usuario, tipo, mensaje, estado));
		} catch (Exception e) {
			log.warn("No se pudo guardar la notificación {} para {}: {}", tipo, usuario.getEmail(), e.getMessage());
		}
	}
}
