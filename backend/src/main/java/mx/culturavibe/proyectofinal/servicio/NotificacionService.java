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
// WhatsApp Cloud API (ver WhatsAppCloudService). Cada canal se envía
// solo si está configurado: SMS necesita VONAGE_API_KEY, VONAGE_API_SECRET
// y VONAGE_FROM_NUMBER, o bien TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN y
// TWILIO_PHONE_NUMBER; WhatsApp necesita WHATSAPP_PHONE_NUMBER_ID y
// WHATSAPP_ACCESS_TOKEN. Twilio trial no deja comprar número sin upgrade,
// por eso Vonage va primero. Si un canal no está configurado, se registra
// un warn y se sigue; la reserva se crea igual. Cada intento (enviado o
// fallido) queda guardado en notificaciones.
@Service
public class NotificacionService {

	private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

	private final NotificacionRepository notificaciones;

	private final VonageSmsService vonage;

	private final WhatsAppCloudService whatsapp;

	@Value("${app.twilio.account-sid:}")
	private String accountSid;

	@Value("${app.twilio.auth-token:}")
	private String authToken;

	@Value("${app.twilio.phone-number:}")
	private String numeroTwilio;

	private boolean inicializado = false;

	public NotificacionService(NotificacionRepository notificaciones, VonageSmsService vonage,
			WhatsAppCloudService whatsapp) {
		this.notificaciones = notificaciones;
		this.vonage = vonage;
		this.whatsapp = whatsapp;
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
			enviarSms(usuario, telefono, texto);
			enviado = true;
		} else {
			log.warn("Ni Vonage ni Twilio están configurados; el código de verificación de {} no va por SMS",
					usuario.getEmail());
		}
		// WhatsApp: el mismo código, con la plantilla de código
		// (WHATSAPP_TEMPLATE_CODIGO). Si WhatsApp no está configurado
		// o la plantilla no existe, solo queda el warn y el código
		// sigue llegando por correo y SMS.
		if (whatsapp.configurado()) {
			if (whatsapp.enviarCodigoVerificacion(telefono, codigo)) {
				guardar(usuario, "WHATSAPP", texto, "ENVIADA");
			} else {
				guardar(usuario, "WHATSAPP", texto, "FALLIDA");
			}
			enviado = true;
		} else {
			log.warn("WhatsApp Cloud API no está configurada; el código de verificación de {} no va por WhatsApp",
					usuario.getEmail());
		}
		if (!enviado) {
			log.warn("El código de verificación de {} solo va por correo", usuario.getEmail());
		}
		return enviado;
	}

	// El SMS sale por Vonage si está configurado; si no, por Twilio si
	// está configurado; si ninguno lo está, solo queda el warn y la
	// reserva se crea igual.
	private void enviarSms(Usuario usuario, String telefono, String texto) {
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
			Message.creator(new PhoneNumber(telefono), new PhoneNumber(numeroTwilio), texto).create();
			guardar(usuario, "SMS", texto, "ENVIADA");
			log.info("SMS enviado por Twilio a {}", telefono);
		} catch (Throwable e) {
			// Throwable y no Exception: el SDK de Twilio usa Apache
			// HttpClient 5 y si falta su dependencia httpcore5-h2 lanza
			// NoClassDefFoundError (un Error, no una Exception). Un aviso
			// que falla nunca debe romper la reserva.
			guardar(usuario, "SMS", texto, "FALLIDA");
			log.warn("No se pudo enviar el SMS por Twilio a {}: {}", telefono, e.getMessage());
		}
	}

	private void enviarWhatsapp(Usuario usuario, String telefono, String texto) {
		if (!whatsapp.configurado()) {
			log.warn("WhatsApp Cloud API no está configurada (faltan WHATSAPP_PHONE_NUMBER_ID o WHATSAPP_ACCESS_TOKEN); no se envía WhatsApp a {}",
					usuario.getEmail());
			return;
		}
		if (whatsapp.enviarConfirmacion(telefono, texto)) {
			guardar(usuario, "WHATSAPP", texto, "ENVIADA");
		} else {
			guardar(usuario, "WHATSAPP", texto, "FALLIDA");
		}
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
