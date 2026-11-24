package mx.culturavibe.proyectofinal.controlador;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.culturavibe.proyectofinal.dto.AuthRespuesta;
import mx.culturavibe.proyectofinal.dto.LoginDTO;
import mx.culturavibe.proyectofinal.dto.OlvidoPasswordDTO;
import mx.culturavibe.proyectofinal.dto.RegistroDTO;
import mx.culturavibe.proyectofinal.dto.RestablecerPasswordDTO;
import mx.culturavibe.proyectofinal.servicio.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<AuthRespuesta> registrar(@Valid @RequestBody RegistroDTO dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(dto));
	}

	@PostMapping("/login")
	public AuthRespuesta login(@Valid @RequestBody LoginDTO dto) {
		return authService.login(dto);
	}

	// Revoca el token Bearer para que ya no sirva.
	@PostMapping("/logout")
	public ResponseEntity<Map<String, String>> logout(
			@RequestHeader(value = "Authorization", required = false) String encabezado) {
		if (encabezado != null && encabezado.startsWith("Bearer ")) {
			authService.logout(encabezado.substring(7));
		}
		return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<Map<String, String>> olvidoPassword(@Valid @RequestBody OlvidoPasswordDTO dto) {
		authService.solicitarRecuperacion(dto);
		// Mensaje genérico a propósito: no revela si el correo existe.
		return ResponseEntity.ok(Map.of("mensaje",
				"Si el correo existe, recibirás un enlace para restablecer tu contraseña"));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Map<String, String>> restablecerPassword(@Valid @RequestBody RestablecerPasswordDTO dto) {
		authService.restablecerPassword(dto);
		return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
	}
}
