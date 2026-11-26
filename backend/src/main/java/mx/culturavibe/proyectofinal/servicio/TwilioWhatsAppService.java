package mx.culturavibe.proyectofinal.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

// WhatsApp con el sandbox de Twilio, gratis. El sandbox ya está activo
// en la cuenta del equipo: el número es +1 737 221 2163 y cada número
// que deba recibir mensajes tiene que unirse antes mandando
// "join twilio-trial" por WhatsApp a ese número. Se envía un POST con
// parámetros de formulario a:
//   https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json
// con autenticación básica (SID como usuario, Auth Token como
// contraseña) y From=whatsapp:+17372212163, To=whatsapp:{teléfono} y
// Body={texto}. La respuesta 201 significa enviado. La cuenta de
// Twilio es de prueba y solo permite plantillas de contenido: el
// código de verificación se manda con la plantilla
// TWILIO_WHATSAPP_CONTENT_SID (ContentSid + ContentVariables con la
// variable {{1}} = el código); si no hay ContentSid, el código se
// manda como texto libre, pero Twilio lo rechaza con 400 código 21654
// "ContentSid Required" cuando la ventana de 24 h del sandbox está
// cerrada. La confirmación de reserva sigue como texto libre.
// Solo se envía si están TWILIO_ACCOUNT_SID y TWILIO_AUTH_TOKEN; si
// no, se registra un warn y se sigue (nunca rompe la reserva). Las
// credenciales se pasan por variables de entorno, nunca van en el
// código ni en el repositorio.
@Service
public class TwilioWhatsAppService {

	private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppService.class);

	private final String accountSid;

	private final String authToken;

	private final String fromNumber;

	// Plantilla de contenido para el código de verificación. La
	// cuenta de Twilio es de prueba y solo permite plantillas: el
	// texto libre da 400 código 21654 "ContentSid Required" cuando la
	// ventana de 24 h del sandbox está cerrada. Si está vacío, el
	// código se manda como texto libre (Body).
	private final String whatsappContentSid;

	private final RestClient restClient;

	public TwilioWhatsAppService(
			@Value("${app.twilio.account-sid:}") String accountSid,
			@Value("${app.twilio.auth-token:}") String authToken,
			@Value("${app.twilio.whatsapp-from:whatsapp:+17372212163}") String fromNumber,
			@Value("${app.twilio.whatsapp-content-sid:}") String whatsappContentSid,
			@Value("${app.twilio.api-base-url:https://api.twilio.com}") String apiBaseUrl) {
		this.accountSid = accountSid;
		this.authToken = authToken;
		this.fromNumber = fromNumber;
		this.whatsappContentSid = whatsappContentSid;
		// Cliente HTTP del JDK (java.net.http): el RestClient por defecto usa
		// Apache HttpClient 5 si lo encuentra en el classpath, y en este
		// proyecto Twilio lo trae sin su dependencia httpcore5-h2, lo que
		// rompía la llamada con NoClassDefFoundError. Con el cliente del
		// JDK la llamada no depende de ninguna librería externa.
		this.restClient = RestClient.builder()
				.requestFactory(new JdkClientHttpRequestFactory())
				.baseUrl(apiBaseUrl)
				.build();
	}

	public boolean configurado() {
		return accountSid != null && !accountSid.isBlank()
				&& authToken != null && !authToken.isBlank()
				&& fromNumber != null && !fromNumber.isBlank();
	}

	// El destino va como whatsapp:+52... (E.164 con el prefijo
	// "whatsapp:"); el remitente igual. Si el valor configurado no
	// trae el prefijo, se agrega. El teléfono que el usuario escribe
	// al registrarse muchas veces viene sin código de país (10 dígitos);
	// sin el +52 Twilio no lo reconoce como el número verificado del
	// sandbox y responde 572002, así que aquí se normaliza a E.164.
	private String comoWhatsapp(String telefono) {
		String limpio = telefono.replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
		if (limpio.startsWith("whatsapp:")) {
			return limpio;
		}
		return "whatsapp:" + aE164(limpio);
	}

	// Normaliza a E.164: número mexicano de 10 dígitos → +52...;
	// 12 dígitos que empiezan con 52 → +...; si ya trae +, se queda.
	private static String aE164(String limpio) {
		if (limpio.startsWith("+")) {
			return limpio;
		}
		if (limpio.length() == 10) {
			return "+52" + limpio;
		}
		if (limpio.length() == 12 && limpio.startsWith("52")) {
			return "+" + limpio;
		}
		return "+" + limpio;
	}

	// Construye el formulario exacto que pide la API de Twilio.
	MultiValueMap<String, String> formulario(String destino, String texto) {
		MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
		formulario.add("From", comoWhatsapp(fromNumber));
		formulario.add("To", comoWhatsapp(destino));
		formulario.add("Body", texto);
		return formulario;
	}

	// Formulario para el código de verificación: con plantilla de
	// contenido (ContentSid + ContentVariables con la variable {{1}}
	// = el código) si TWILIO_WHATSAPP_CONTENT_SID está configurado;
	// si no, con Body (respaldo, mismo comportamiento de antes).
	MultiValueMap<String, String> formularioCodigo(String destino, String codigo) {
		MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
		formulario.add("From", comoWhatsapp(fromNumber));
		formulario.add("To", comoWhatsapp(destino));
		if (whatsappContentSid != null && !whatsappContentSid.isBlank()) {
			formulario.add("ContentSid", whatsappContentSid);
			formulario.add("ContentVariables", "{\"1\":\"" + codigo + "\"}");
		} else {
			formulario.add("Body", "CulturaVibe: tu código de verificación es " + codigo + ". Válido por 15 minutos.");
		}
		return formulario;
	}

	// Envía el mensaje por el sandbox de Twilio. Devuelve true si
	// Twilio respondió 2xx (201 = enviado); false si no está
	// configurado o si la llamada falló (en ese caso solo queda el
	// warn en el log).
	public boolean enviarMensaje(String telefono, String texto) {
		if (!configurado()) {
			log.warn("WhatsApp por Twilio no está configurado (faltan TWILIO_ACCOUNT_SID o TWILIO_AUTH_TOKEN); no se envía WhatsApp");
			return false;
		}
		return enviarFormulario(formulario(telefono, texto), telefono);
	}

	// Código de verificación por WhatsApp con plantilla de contenido.
	// La cuenta de prueba solo permite plantillas: el texto libre da
	// 400 código 21654 "ContentSid Required" cuando la ventana de 24 h
	// del sandbox está cerrada. Si TWILIO_WHATSAPP_CONTENT_SID está
	// configurado se manda la plantilla; si no, Body (respaldo).
	public boolean enviarCodigoVerificacion(String telefono, String codigo) {
		if (!configurado()) {
			log.warn("WhatsApp por Twilio no está configurado (faltan TWILIO_ACCOUNT_SID o TWILIO_AUTH_TOKEN); no se envía el código por WhatsApp");
			return false;
		}
		return enviarFormulario(formularioCodigo(telefono, codigo), telefono);
	}

	// POST común a Messages.json. Devuelve true si Twilio respondió
	// 2xx (201 = enviado); false si la llamada falló (solo queda el
	// warn en el log).
	private boolean enviarFormulario(MultiValueMap<String, String> formulario, String telefono) {
		String destino = telefono.replace(" ", "").replace("-", "");
		try {
			var respuesta = restClient.post()
					.uri("/2010-04-01/Accounts/{accountSid}/Messages.json", accountSid)
					.headers(headers -> headers.setBasicAuth(accountSid, authToken))
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(formulario)
					.retrieve()
					.toBodilessEntity();
			if (respuesta.getStatusCode().is2xxSuccessful()) {
				log.info("WhatsApp enviado por el sandbox de Twilio a {}", destino);
				return true;
			}
			log.warn("Twilio no aceptó el WhatsApp a {} (estado {})", destino, respuesta.getStatusCode());
			return false;
		} catch (Exception e) {
			// El error más común del sandbox es que el destinatario no
			// se unió: para recibir mensajes hay que mandar
			// "join twilio-trial" por WhatsApp al +1 737 221 2163.
			log.warn("No se pudo enviar el WhatsApp por el sandbox de Twilio a {}: {}. Si el número no se unió al sandbox, debe mandar \"join twilio-trial\" por WhatsApp al +1 737 221 2163",
					destino, e.getMessage());
			return false;
		}
	}
}
