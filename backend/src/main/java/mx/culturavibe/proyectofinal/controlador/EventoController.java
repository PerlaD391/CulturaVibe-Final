package mx.culturavibe.proyectofinal.controlador;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.culturavibe.proyectofinal.dto.EventoDTO;
import mx.culturavibe.proyectofinal.servicio.EventoService;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

	private final EventoService eventos;

	public EventoController(EventoService eventos) {
		this.eventos = eventos;
	}

	// Público: filtros opcionales por categoría, texto (título/descripción) y fecha.
	@GetMapping
	public Page<EventoDTO> listar(
			@RequestParam(required = false) Long categoria,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@PageableDefault(size = 12, sort = "fecha") Pageable pageable) {
		return eventos.listar(categoria, q, fecha, pageable);
	}

	@GetMapping("/{id}")
	public EventoDTO buscarPorId(@PathVariable Long id) {
		return eventos.buscarPorId(id);
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR')")
	public ResponseEntity<EventoDTO> crear(@Valid @RequestBody EventoDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(eventos.crear(dto));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR')")
	public EventoDTO actualizar(@PathVariable Long id, @Valid @RequestBody EventoDTO dto) {
		return eventos.actualizar(id, dto);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR')")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		eventos.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}
