package mx.culturavibe.proyectofinal.controlador;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.culturavibe.proyectofinal.servicio.TicketmasterSyncService;

// Sincronización manual de eventos de Ticketmaster a MySQL (solo ADMIN).
// No se ejecuta al arrancar: el ADMIN la dispara cuando quiere con
// POST /api/admin/ticketmaster/sync. Sin TICKETMASTER_API_KEY responde
// 503 con un mensaje claro (no 500).
@RestController
@RequestMapping("/api/admin/ticketmaster")
public class TicketmasterAdminController {

	private final TicketmasterSyncService sync;

	public TicketmasterAdminController(TicketmasterSyncService sync) {
		this.sync = sync;
	}

	@PostMapping("/sync")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Map<String, Object>> sincronizar() {
		try {
			TicketmasterSyncService.ResultadoSync resultado = sync.sincronizar();
			return ResponseEntity.ok(Map.of(
					"importados", resultado.nuevos(),
					"yaExistian", resultado.yaExistian(),
					"total", resultado.totalTicketmaster()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(Map.of("message", e.getMessage()));
		}
	}
}
