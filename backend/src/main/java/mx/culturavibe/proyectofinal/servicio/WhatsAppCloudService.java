package mx.culturavibe.proyectofinal.servicio;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

// WhatsApp con Meta WhatsApp Cloud API (Graph API), gratis.
// Se envía un mensaje de plantilla (por defecto hello_world, que no
// lleva parámetros) con POST a:
//   https://graph.facebook.com/v21.0/{phone-number-id}/messages
// Solo se envía si están WHATSAPP_PHONE_NUMBER_ID y WHATSAPP_ACCESS_TOKEN;
// si no, se registra un warn y se sigue (nunca rompe la reserva).
// El token de Meta es temporal (24 h) y se pasa por variable de entorno,
// nunca va en el código ni en el repositorio.
@Service
public class WhatsAppCloudService {

	private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudService.class);

	private final String phoneNumberId;

	private final String accessToken;

	private final String templateName;

	private final String templateCodigo;

	private final RestClient restClient;

	public WhatsAppCloudService(
			@Value("${app.whatsapp.phone-number-id:}") String phoneNumberId,
			@Value("${app.whatsapp.access-token:}") String accessToken,
			@Value("${app.whatsapp.template-name:hello_world}") String templateName,
			@Value("${app.whatsapp.template-codigo:codigo_verificacion}") String templateCodigo,
			@Value("${app.whatsapp.api-base-url:https://graph.facebook.com/v21.0}") String apiBaseUrl) {
		this.phoneNumberId = phoneNumberId;
		this.accessToken = accessToken;
		this.templateName = templateName;
		this.templateCodigo = templateCodigo;
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
		return phoneNumberId != null && !phoneNumberId.isBlank()
				&& accessToken != null && !accessToken.isBlank();
	}

	// El cuerpo JSON que pide la Cloud API: mensaje de plantilla con el
	// nombre y el idioma. La plantilla hello_world no lleva variables,
	// así que el texto de la reserva no viaja en el JSON.
	Map<String, Object> cuerpoMensaje(String destino) {
		Map<String, Object> lenguaje = Map.of("code", "es_MX");
		Map<String, Object> plantilla = new LinkedHashMap<>();
		plantilla.put("name", templateName);
		plantilla.put("language", lenguaje);
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("messaging_product", "whatsapp");
		cuerpo.put("to", destino);
		cuerpo.put("type", "template");
		cuerpo.put("template", plantilla);
		return cuerpo;
	}

	// Envía el mensaje de plantilla al teléfono del usuario.
	// Devuelve true si Meta respondió 2xx; false si no está configurado
	// o si la llamada falló (en ese caso solo queda el warn en el log).
	// El texto de la reserva solo se usa en el log: la plantilla
	// hello_world no lleva variables, así que no viaja en el JSON.
	public boolean enviarConfirmacion(String telefono, String texto) {
		if (!configurado()) {
			log.warn("WhatsApp Cloud API no está configurada (faltan WHATSAPP_PHONE_NUMBER_ID o WHATSAPP_ACCESS_TOKEN); no se envía WhatsApp");
			return false;
		}
		// La Cloud API quiere el número solo con dígitos, sin '+' ni espacios.
		String destino = telefono.replace(" ", "").replace("-", "").replace("+", "");
		try {
			restClient.post()
					.uri("/{phoneNumberId}/messages", phoneNumberId)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.body(cuerpoMensaje(destino))
					.retrieve()
					.toBodilessEntity();
			log.info("WhatsApp enviado a {} (plantilla {})", destino, templateName);
			return true;
		} catch (Exception e) {
			log.warn("No se pudo enviar el WhatsApp a {}: {}", destino, e.getMessage());
			return false;
		}
	}

	// Código de verificación de 6 dígitos por WhatsApp. Usa la
	// plantilla de código (WHATSAPP_TEMPLATE_CODIGO), que debe existir
	// en Meta con una variable {{1}} para el código: la plantilla
	// hello_world no lleva variables, así que el código no se puede
	// mandar con ella. Si la plantilla no existe o no está aprobada,
	// Meta responde 400, solo queda el warn y el código sigue llegando
	// por correo y SMS. Devuelve true si Meta respondió 2xx.
	public boolean enviarCodigoVerificacion(String telefono, String codigo) {
		if (!configurado()) {
			log.warn("WhatsApp Cloud API no está configurada (faltan WHATSAPP_PHONE_NUMBER_ID o WHATSAPP_ACCESS_TOKEN); no se envía el código por WhatsApp");
			return false;
		}
		// La Cloud API quiere el número solo con dígitos, sin '+' ni espacios.
		String destino = telefono.replace(" ", "").replace("-", "").replace("+", "");
		Map<String, Object> parametro = Map.of("type", "text", "text", codigo);
		Map<String, Object> componente = new LinkedHashMap<>();
		componente.put("type", "body");
		componente.put("parameters", java.util.List.of(parametro));
		Map<String, Object> plantilla = new LinkedHashMap<>();
		plantilla.put("name", templateCodigo);
		plantilla.put("language", Map.of("code", "es_MX"));
		plantilla.put("components", java.util.List.of(componente));
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("messaging_product", "whatsapp");
		cuerpo.put("to", destino);
		cuerpo.put("type", "template");
		cuerpo.put("template", plantilla);
		try {
			restClient.post()
					.uri("/{phoneNumberId}/messages", phoneNumberId)
					.header("Authorization", "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)
					.body(cuerpo)
					.retrieve()
					.toBodilessEntity();
			log.info("Código de verificación enviado por WhatsApp a {} (plantilla {})", destino, templateCodigo);
			return true;
		} catch (Exception e) {
			log.warn("No se pudo enviar el código por WhatsApp a {}: {}", destino, e.getMessage());
			return false;
		}
	}
}
