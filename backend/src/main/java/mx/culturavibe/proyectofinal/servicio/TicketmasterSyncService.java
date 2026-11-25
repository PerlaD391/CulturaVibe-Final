package mx.culturavibe.proyectofinal.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import mx.culturavibe.proyectofinal.modelo.Categoria;
import mx.culturavibe.proyectofinal.modelo.Evento;
import mx.culturavibe.proyectofinal.repositorio.CategoriaRepository;
import mx.culturavibe.proyectofinal.repositorio.EventoRepository;

// Importa los eventos de Ticketmaster a la tabla eventos, para que el
// catálogo muestre todos (no solo los 12 de la semilla) y las reservas
// sigan funcionando igual (Reserva referencia Evento por FK). MySQL
// sigue siendo la única fuente del catálogo: la sincronización solo
// se ejecuta cuando un ADMIN llama a POST /api/admin/ticketmaster/sync.
// Upsert por external_id: si el evento ya existe se salta, nunca se
// duplica. Cada guardado es su propia transacción: si un evento falla,
// se registra un warn y se sigue con los demás.
@Service
public class TicketmasterSyncService {

	private static final Logger log = LoggerFactory.getLogger(TicketmasterSyncService.class);

	private final TicketmasterClient cliente;

	private final EventoRepository eventos;

	private final CategoriaRepository categorias;

	public TicketmasterSyncService(TicketmasterClient cliente, EventoRepository eventos,
			CategoriaRepository categorias) {
		this.cliente = cliente;
		this.eventos = eventos;
		this.categorias = categorias;
	}

	public record ResultadoSync(int nuevos, int yaExistian, int totalTicketmaster) {
	}

	public ResultadoSync sincronizar() {
		List<JsonNode> nodos = cliente.buscarEventosMexico();
		Map<String, Categoria> porNombre = cargarCategorias();
		int nuevos = 0;
		int yaExistian = 0;
		int errores = 0;
		for (JsonNode nodo : nodos) {
			String externalId = nodo.path("id").asText(null);
			if (externalId == null || externalId.isBlank()) {
				errores++;
				log.warn("Evento de Ticketmaster sin id, se omite");
				continue;
			}
			if (eventos.existsByExternalId(externalId)) {
				yaExistian++;
				continue;
			}
			try {
				Evento evento = mapear(nodo, porNombre);
				if (evento == null) {
					// Sin título o sin fecha válida: no se puede importar.
					errores++;
					continue;
				}
				eventos.save(evento);
				nuevos++;
			} catch (DataIntegrityViolationException e) {
				// Otro proceso lo insertó entre la consulta y el guardado
				// (el índice único de external_id lo impidió duplicar).
				yaExistian++;
			} catch (Exception e) {
				errores++;
				log.warn("No se pudo importar el evento de Ticketmaster {}: {}", externalId, e.getMessage());
			}
		}
		log.info("Sincronización Ticketmaster: {} nuevos, {} ya existían, {} errores, {} en total",
				nuevos, yaExistian, errores, nodos.size());
		return new ResultadoSync(nuevos, yaExistian, nodos.size());
	}

	// Las 5 categorías del catálogo, cargadas una vez. Si falta alguna,
	// la sincronización no arranca (la semilla las crea todas).
	private Map<String, Categoria> cargarCategorias() {
		Map<String, Categoria> mapa = new HashMap<>();
		for (String nombre : List.of("Música", "Teatro", "Danza", "Arte", "Festivales")) {
			Categoria categoria = categorias.findByNombreIgnoreCase(nombre)
					.orElseThrow(() -> new IllegalStateException(
							"Falta la categoría semilla '" + nombre + "' en la base de datos"));
			mapa.put(nombre, categoria);
		}
		return mapa;
	}

	// Mapea un evento de Ticketmaster a Evento. Devuelve null si no se
	// puede importar (sin título o sin fecha válida). Todos los campos
	// opcionales se leen con path(), que no falla si faltan (devuelve
	// un nodo vacío), así que un evento incompleto no rompe el sync.
	Evento mapear(JsonNode nodo, Map<String, Categoria> porNombre) {
		String titulo = nodo.path("name").asText(null);
		if (titulo == null || titulo.isBlank()) {
			log.warn("Evento de Ticketmaster {} sin título, se omite", nodo.path("id").asText("?"));
			return null;
		}
		String fechaTexto = nodo.path("dates").path("start").path("localDate").asText(null);
		if (fechaTexto == null || fechaTexto.isBlank()) {
			log.warn("Evento de Ticketmaster {} sin fecha, se omite", nodo.path("id").asText("?"));
			return null;
		}
		LocalDate fecha;
		try {
			fecha = LocalDate.parse(fechaTexto);
		} catch (Exception e) {
			log.warn("Evento de Ticketmaster {} con fecha inválida '{}', se omite",
					nodo.path("id").asText("?"), fechaTexto);
			return null;
		}

		LocalTime hora = null;
		String horaTexto = nodo.path("dates").path("start").path("localTime").asText(null);
		if (horaTexto != null && !horaTexto.isBlank()) {
			try {
				hora = LocalTime.parse(horaTexto);
			} catch (Exception e) {
				log.warn("Evento de Ticketmaster {} con hora inválida '{}', se guarda sin hora",
						nodo.path("id").asText("?"), horaTexto);
			}
		}

		// Venue: el nombre (y la calle, si viene) van a dirección; la
		// ciudad y el estado, a sus columnas. Sin venue quedan en null.
		JsonNode venue = nodo.path("_embedded").path("venues").path(0);
		String direccion = venue.path("name").asText(null);
		String linea1 = venue.path("address").path("line1").asText(null);
		if (direccion != null && linea1 != null && !linea1.isBlank()) {
			direccion = direccion + ", " + linea1;
		}
		String ciudad = venue.path("city").path("name").asText(null);
		String estado = venue.path("state").path("stateCode").asText(null);
		if (estado == null || estado.isBlank()) {
			estado = venue.path("state").path("name").asText(null);
		}

		BigDecimal precio = BigDecimal.ZERO;
		JsonNode rangos = nodo.path("priceRanges");
		if (rangos.isArray() && !rangos.isEmpty()) {
			JsonNode min = rangos.get(0).path("min");
			if (min.isNumber()) {
				precio = min.decimalValue();
			}
		}

		List<String> nombresArtistas = new ArrayList<>();
		JsonNode attractions = nodo.path("_embedded").path("attractions");
		if (attractions.isArray()) {
			for (JsonNode attraction : attractions) {
				String nombre = attraction.path("name").asText(null);
				if (nombre != null && !nombre.isBlank()) {
					nombresArtistas.add(nombre);
				}
			}
		}
		String artistas = nombresArtistas.isEmpty() ? null : String.join(", ", nombresArtistas);

		String descripcion = nodo.path("info").asText(null);
		if (descripcion == null || descripcion.isBlank()) {
			descripcion = nodo.path("pleaseNote").asText(null);
		}
		if (descripcion == null) {
			descripcion = "";
		}

		// La primera imagen del evento (si trae). El catálogo hoy muestra
		// la misma imagen genérica en todas las tarjetas; con esto cada
		// evento importado trae su propia imagen de Ticketmaster.
		String imagenUrl = null;
		JsonNode imagenes = nodo.path("images");
		if (imagenes.isArray() && !imagenes.isEmpty()) {
			imagenUrl = imagenes.get(0).path("url").asText(null);
		}

		Evento evento = new Evento();
		evento.setTitulo(titulo);
		evento.setDescripcion(descripcion);
		evento.setFecha(fecha);
		evento.setHora(hora);
		evento.setDireccion(direccion);
		evento.setCiudad(ciudad);
		evento.setEstado(estado);
		evento.setCategoria(mapearCategoria(nodo, porNombre));
		// Sin organizador: los eventos importados no pertenecen a ningún
		// usuario (la columna organizador_id admite null).
		evento.setOrganizador(null);
		evento.setPrecio(precio);
		evento.setEstadoVenta(mapearEstadoVenta(nodo));
		evento.setUrl(nodo.path("url").asText(null));
		evento.setImagenUrl(imagenUrl);
		evento.setArtistas(artistas);
		evento.setFuente("TICKETMASTER");
		evento.setExternalId(nodo.path("id").asText());
		return evento;
	}

	private String mapearEstadoVenta(JsonNode nodo) {
		String codigo = nodo.path("dates").path("status").path("code").asText("onsale");
		if ("offsale".equals(codigo)) {
			return "Agotado";
		}
		if ("cancelled".equals(codigo)) {
			return "Cancelado";
		}
		// onsale, postponed y cualquier otro código: en venta.
		return "En venta";
	}

	// Segmento de Ticketmaster → categoría del catálogo (solo hay 5):
	//   Music → Música
	//   Arts & Theatre → Teatro, salvo que el género sea danza
	//     (Dance, Danza, Ballet → Danza) o arte (Art, Arte,
	//     Exhibition → Arte)
	//   Film, Sports, Miscellaneous y cualquier otro segmento → Festivales
	//   (categoría genérica: no hay Cine ni Deportes en el catálogo).
	// Sin classifications → Festivales.
	private Categoria mapearCategoria(JsonNode nodo, Map<String, Categoria> porNombre) {
		JsonNode clasificacion = nodo.path("classifications").path(0);
		String segmento = clasificacion.path("segment").path("name").asText(null);
		if ("Music".equalsIgnoreCase(segmento)) {
			return porNombre.get("Música");
		}
		if ("Arts & Theatre".equalsIgnoreCase(segmento)) {
			String genero = clasificacion.path("genre").path("name").asText("");
			String generoMinusculas = genero.toLowerCase();
			if (generoMinusculas.contains("dance") || generoMinusculas.contains("danza")
					|| generoMinusculas.contains("ballet")) {
				return porNombre.get("Danza");
			}
			if (generoMinusculas.contains("art") || generoMinusculas.contains("arte")
					|| generoMinusculas.contains("exhibition") || generoMinusculas.contains("exposición")) {
				return porNombre.get("Arte");
			}
			return porNombre.get("Teatro");
		}
		return porNombre.get("Festivales");
	}
}
