package mx.culturavibe.proyectofinal.servicio;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// Cliente de la API Discovery de Ticketmaster (la misma que usa la
// app de la Actividad 8). Trae los eventos de México en ventanas
// mensuales: Ticketmaster rechaza las consultas con page * size >= 1000,
// así que cada ventana se pagina por separado (máximo 5 páginas de 200).
// La llave se pasa con la variable de entorno TICKETMASTER_API_KEY;
// sin ella el cliente no hace ninguna llamada (el controlador responde
// 503). Sigue el mismo patrón que VonageSmsService: RestClient con el
// cliente HTTP del JDK, para no depender de Apache HttpClient 5.
@Service
public class TicketmasterClient {

	private static final Logger log = LoggerFactory.getLogger(TicketmasterClient.class);

	private static final String BASE_URL = "https://app.ticketmaster.com";

	private static final int PAGE_SIZE = 200;

	// Ticketmaster rechaza page * size >= 1000.
	private static final int MAX_PAGE_POR_SIZE = 1000;

	// Cuántos meses hacia adelante se sincronizan, contando desde hoy.
	private static final int MESES_ADELANTE = 12;

	private final String apiKey;

	private final RestClient restClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public TicketmasterClient(@Value("${TICKETMASTER_API_KEY:}") String apiKey) {
		this.apiKey = apiKey;
		// Cliente HTTP del JDK (java.net.http): el RestClient por defecto usa
		// Apache HttpClient 5 si lo encuentra en el classpath, y en este
		// proyecto Twilio lo trae sin su dependencia httpcore5-h2, lo que
		// rompía las llamadas con NoClassDefFoundError. Con el cliente del
		// JDK la llamada no depende de ninguna librería externa.
		this.restClient = RestClient.builder()
				.requestFactory(new JdkClientHttpRequestFactory())
				.baseUrl(BASE_URL)
				.build();
	}

	public boolean configurado() {
		return apiKey != null && !apiKey.isBlank();
	}

	// Todos los eventos de México (countryCode=MX), ordenados por fecha,
	// en ventanas mensuales desde hoy hasta 12 meses adelante. Devuelve
	// los nodos JSON crudos de _embedded.events[]; el mapeo a Evento
	// lo hace TicketmasterSyncService.
	public List<JsonNode> buscarEventosMexico() {
		if (!configurado()) {
			throw new IllegalStateException("TICKETMASTER_API_KEY no configurada");
		}
		List<JsonNode> todos = new ArrayList<>();
		LocalDate hoy = LocalDate.now();
		LocalDate fin = hoy.plusMonths(MESES_ADELANTE);
		LocalDate inicioVentana = hoy;
		int ventanas = 0;
		while (!inicioVentana.isAfter(fin)) {
			LocalDate finVentana = inicioVentana.withDayOfMonth(inicioVentana.lengthOfMonth());
			if (finVentana.isAfter(fin)) {
				finVentana = fin;
			}
			int enVentana = buscarVentana(inicioVentana, finVentana, todos);
			log.info("Ticketmaster: ventana {} a {}: {} eventos", inicioVentana, finVentana, enVentana);
			ventanas++;
			inicioVentana = finVentana.plusDays(1);
		}
		log.info("Ticketmaster: {} eventos en total en {} ventanas", todos.size(), ventanas);
		return todos;
	}

	// Una ventana de fechas: pide páginas de 200 hasta agotar
	// totalElements o llegar al tope de 1000 resultados por consulta.
	private int buscarVentana(LocalDate desde, LocalDate hasta, List<JsonNode> acumulador) {
		int pagina = 0;
		int totalElements = Integer.MAX_VALUE;
		int traidos = 0;
		while (pagina * PAGE_SIZE < MAX_PAGE_POR_SIZE && traidos < totalElements) {
			JsonNode raiz = pedirPagina(desde, hasta, pagina);
			totalElements = raiz.path("page").path("totalElements").asInt(0);
			JsonNode eventos = raiz.path("_embedded").path("events");
			if (!eventos.isArray() || eventos.isEmpty()) {
				break;
			}
			for (JsonNode evento : eventos) {
				acumulador.add(evento);
			}
			traidos += eventos.size();
			if (traidos >= totalElements) {
				break;
			}
			pagina++;
		}
		if (totalElements > traidos) {
			log.warn("Ticketmaster: la ventana {} a {} tiene {} eventos, pero el tope de 1000 por consulta "
					+ "solo permitió traer {} (los más próximos en fecha)", desde, hasta, totalElements, traidos);
		}
		return traidos;
	}

	private JsonNode pedirPagina(LocalDate desde, LocalDate hasta, int pagina) {
		String inicio = desde.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
		String fin = hasta.atTime(23, 59, 59).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";
		try {
			String cuerpo = restClient.get()
					.uri(uriBuilder -> uriBuilder.path("/discovery/v2/events.json")
							.queryParam("apikey", apiKey)
							.queryParam("countryCode", "MX")
							.queryParam("size", PAGE_SIZE)
							.queryParam("page", pagina)
							.queryParam("sort", "date,asc")
							.queryParam("startDateTime", inicio)
							.queryParam("endDateTime", fin)
							.build())
					.retrieve()
					.body(String.class);
			return objectMapper.readTree(cuerpo);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("No se pudo leer la respuesta de Ticketmaster: " + e.getMessage(), e);
		} catch (RuntimeException e) {
			throw new IllegalStateException("Error al consultar Ticketmaster: " + e.getMessage(), e);
		}
	}
}
