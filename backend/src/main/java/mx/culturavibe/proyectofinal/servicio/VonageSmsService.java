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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// SMS con Vonage (antes Nexmo), equivalente a Twilio y permitido por la
// rúbrica. Vonage tiene prueba gratis con un número virtual incluido, y
// no exige upgrade para comprar el número, que es justo lo que Twilio
// trial no permite. Se envía un POST con parámetros de formulario a:
//   https://rest.nexmo.com/sms/json
// con api_key, api_secret, from, to y text. La respuesta JSON trae
// messages[0].status: "0" significa enviado, cualquier otro valor es
// un error (por ejemplo "4" son credenciales inválidas o "15" un
// remitente no válido). Solo se envía si están VONAGE_API_KEY,
// VONAGE_API_SECRET y VONAGE_FROM_NUMBER; si no, se registra un warn
// y se sigue (nunca rompe la reserva). Las credenciales se pasan por
// variables de entorno, nunca van en el código ni en el repositorio.
@Service
public class VonageSmsService {

	private static final Logger log = LoggerFactory.getLogger(VonageSmsService.class);

	private final String apiKey;

	private final String apiSecret;

	private final String fromNumber;

	private final RestClient restClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public VonageSmsService(
			@Value("${app.vonage.api-key:}") String apiKey,
			@Value("${app.vonage.api-secret:}") String apiSecret,
			@Value("${app.vonage.from-number:}") String fromNumber,
			@Value("${app.vonage.api-base-url:https://rest.nexmo.com}") String apiBaseUrl) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.fromNumber = fromNumber;
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
		return apiKey != null && !apiKey.isBlank()
				&& apiSecret != null && !apiSecret.isBlank()
				&& fromNumber != null && !fromNumber.isBlank();
	}

	// Vonage quiere los números solo con dígitos, sin '+' ni espacios.
	private String soloDigitos(String telefono) {
		return telefono.replace(" ", "").replace("-", "").replace("+", "");
	}

	// Construye el formulario exacto que pide la API de Vonage.
	// El destino ya viene solo con dígitos (lo limpia enviarSms).
	MultiValueMap<String, String> formulario(String destino, String texto) {
		MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
		formulario.add("api_key", apiKey);
		formulario.add("api_secret", apiSecret);
		formulario.add("from", soloDigitos(fromNumber));
		formulario.add("to", destino);
		formulario.add("text", texto);
		// El texto de la reserva lleva acentos y '¡'; si no es GSM-7
		// puro, Vonage necesita type=unicode para no romperlo.
		if (texto.chars().anyMatch(c -> c > 127)) {
			formulario.add("type", "unicode");
		}
		return formulario;
	}

	// Lee la respuesta JSON de Vonage: messages[0].status == "0" es
	// éxito; cualquier otro valor (o una respuesta ilegible) es fallo.
	boolean respuestaExitosa(String respuestaJson) {
		try {
			JsonNode mensajes = objectMapper.readTree(respuestaJson).path("messages");
			return mensajes.isArray() && !mensajes.isEmpty()
					&& "0".equals(mensajes.get(0).path("status").asText(""));
		} catch (Exception e) {
			log.warn("No se pudo leer la respuesta de Vonage: {}", e.getMessage());
			return false;
		}
	}

	// Envía el SMS al teléfono del usuario. Devuelve true si Vonage
	// respondió con messages[0].status == "0"; false si no está
	// configurado o si la llamada falló (en ese caso solo queda el
	// warn en el log).
	public boolean enviarSms(String telefono, String texto) {
		if (!configurado()) {
			log.warn("Vonage no está configurado (faltan VONAGE_API_KEY, VONAGE_API_SECRET o VONAGE_FROM_NUMBER); no se envía SMS");
			return false;
		}
		String destino = soloDigitos(telefono);
		try {
			String respuesta = restClient.post()
					.uri("/sms/json")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(formulario(destino, texto))
					.retrieve()
					.body(String.class);
			if (respuestaExitosa(respuesta)) {
				log.info("SMS enviado por Vonage a {}", destino);
				return true;
			}
			log.warn("Vonage no aceptó el SMS a {}", destino);
			return false;
		} catch (Exception e) {
			log.warn("No se pudo enviar el SMS por Vonage a {}: {}", destino, e.getMessage());
			return false;
		}
	}
}
