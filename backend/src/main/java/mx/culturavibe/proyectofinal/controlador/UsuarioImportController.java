package mx.culturavibe.proyectofinal.controlador;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.culturavibe.proyectofinal.servicio.UsuarioImportService;

// Importación manual de usuarios de DummyJSON a MySQL (solo ADMIN).
// No se ejecuta al arrancar: el ADMIN la dispara cuando quiere con
// POST /api/admin/usuarios/importar-dummyjson. Si DummyJSON no
// responde, devuelve 503 con un mensaje claro (no 500).
@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioImportController {

	private final UsuarioImportService importService;

	public UsuarioImportController(UsuarioImportService importService) {
		this.importService = importService;
	}

	@PostMapping("/importar-dummyjson")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Map<String, Object>> importar(
			@RequestParam(defaultValue = "30") int limit) {
		try {
			UsuarioImportService.ResultadoImportacion resultado = importService.importar(limit);
			return ResponseEntity.ok(Map.of(
					"importados", resultado.importados(),
					"yaExistian", resultado.yaExistian(),
					"total", resultado.total()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(Map.of("message", e.getMessage()));
		}
	}
}
