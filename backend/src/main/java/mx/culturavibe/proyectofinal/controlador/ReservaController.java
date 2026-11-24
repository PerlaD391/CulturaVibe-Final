package mx.culturavibe.proyectofinal.controlador;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.culturavibe.proyectofinal.dto.ReservaDTO;
import mx.culturavibe.proyectofinal.servicio.ReservaService;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

	private final ReservaService reservas;

	public ReservaController(ReservaService reservas) {
		this.reservas = reservas;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN', 'ORGANIZADOR')")
	public ResponseEntity<ReservaDTO> crear(@Valid @RequestBody ReservaDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(reservas.crear(dto));
	}

	@GetMapping("/mis")
	@PreAuthorize("isAuthenticated()")
	public Page<ReservaDTO> misReservas(@PageableDefault(size = 12, sort = "fechaReserva") Pageable pageable) {
		return reservas.misReservas(pageable);
	}

	// ADMIN ve todas; ORGANIZADOR solo las de sus eventos.
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZADOR')")
	public Page<ReservaDTO> listarTodas(
			@RequestParam(required = false) Long eventoId,
			@RequestParam(required = false) String estado,
			@PageableDefault(size = 12, sort = "fechaReserva") Pageable pageable) {
		return reservas.listarTodas(eventoId, estado, pageable);
	}

	// Cancela la reserva (estado=CANCELADA) y responde 200 con el DTO.
	@DeleteMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ReservaDTO cancelar(@PathVariable Long id) {
		return reservas.cancelar(id);
	}
}
