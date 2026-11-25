package mx.culturavibe.proyectofinal.servicio;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// Cliente de la API pública de DummyJSON (la misma que usaba la app
// de la Actividad 8 para el login). No necesita llave. Trae usuarios
// con nombre, correo y contraseña en texto plano, para poblar la base
// de usuarios del Proyecto Final. Sigue el mismo patrón que
// TicketmasterClient: RestClient con el cliente HTTP del JDK y
// parseo con Jackson JsonNode.
@Service
public class DummyJsonClient {

	private static final Logger log = LoggerFactory.getLogger(DummyJsonClient.class);

	private static final String BASE_URL = "https://dummyjson.com";

	private final RestClient restClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public DummyJsonClient() {
		this.restClient = RestClient.builder()
				.requestFactory(new JdkClientHttpRequestFactory())
				.baseUrl(BASE_URL)
				.build();
	}

	public record UsuarioDummy(String nombre, String email, String password) {
	}

	// Trae hasta `limit` usuarios de DummyJSON (hay 208 en total).
	// Solo se piden los campos necesarios: id, nombre, apellidos,
	// correo y contraseña en texto plano.
	public List<UsuarioDummy> buscarUsuarios(int limit) {
		int limite = limit < 1 ? 30 : limit;
		JsonNode raiz = pedirUsuarios(limite);
		JsonNode usuarios = raiz.path("users");
		List<UsuarioDummy> lista = new ArrayList<>();
		if (!usuarios.isArray()) {
			return lista;
		}
		for (JsonNode nodo : usuarios) {
			String nombre = nodo.path("firstName").asText("");
			String apellido = nodo.path("lastName").asText("");
			String nombreCompleto = (nombre + " " + apellido).trim();
			String email = nodo.path("email").asText(null);
			String password = nodo.path("password").asText(null);
			if (nombreCompleto.isBlank() || email == null || email.isBlank()
					|| password == null || password.isBlank()) {
				log.warn("Usuario de DummyJSON {} sin nombre, correo o contraseña, se omite",
						nodo.path("id").asText("?"));
				continue;
			}
			lista.add(new UsuarioDummy(nombreCompleto, email, password));
		}
		log.info("DummyJSON: {} usuarios traídos (límite {})", lista.size(), limite);
		return lista;
	}

	private JsonNode pedirUsuarios(int limite) {
		try {
			String cuerpo = restClient.get()
					.uri(uriBuilder -> uriBuilder.path("/users")
							.queryParam("limit", limite)
							.queryParam("select", "id,firstName,lastName,email,password")
							.build())
					.retrieve()
					.body(String.class);
			return objectMapper.readTree(cuerpo);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("No se pudo leer la respuesta de DummyJSON: " + e.getMessage(), e);
		} catch (RuntimeException e) {
			throw new IllegalStateException("Error al consultar DummyJSON: " + e.getMessage(), e);
		}
	}
}
