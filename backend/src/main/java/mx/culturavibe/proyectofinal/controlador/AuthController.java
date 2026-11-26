package mx.culturavibe.proyectofinal.controlador;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import mx.culturavibe.proyectofinal.dto.AuthRespuesta;
import mx.culturavibe.proyectofinal.dto.FirebaseLoginDTO;
import mx.culturavibe.proyectofinal.dto.LoginDTO;
import mx.culturavibe.proyectofinal.dto.OlvidoPasswordDTO;
import mx.culturavibe.proyectofinal.dto.ReenviarCodigoDTO;
import mx.culturavibe.proyectofinal.dto.RegistroDTO;
import mx.culturavibe.proyectofinal.dto.RestablecerPasswordDTO;
import mx.culturavibe.proyectofinal.dto.UsuarioDTO;
import mx.culturavibe.proyectofinal.dto.VerificarCodigoDTO;
import mx.culturavibe.proyectofinal.dto.VerificarTelefonoFirebaseDTO;
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
		try {
			return authService.login(dto);
		} catch (BadCredentialsException e) {
			// El contador se guarda en su propia transacción (si se
			// contara dentro de login(), el rollback lo borraría).
			boolean suspendida = authService.registrarIntentoFallido(dto.email());
			if (suspendida) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN,
						"Tu cuenta está suspendida por varios intentos fallidos. Revisa tu correo para recuperarla.");
			}
			throw e;
		}
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

	// Verifica el código de 6 dígitos enviado al registrarse.
	@PostMapping("/verificar-codigo")
	public ResponseEntity<Map<String, String>> verificarCodigo(@Valid @RequestBody VerificarCodigoDTO dto) {
		return ResponseEntity.ok(Map.of("mensaje", authService.verificarCodigo(dto)));
	}

	// Manda un código nuevo (invalida los anteriores).
	@PostMapping("/reenviar-codigo")
	public ResponseEntity<Map<String, String>> reenviarCodigo(@Valid @RequestBody ReenviarCodigoDTO dto) {
		return ResponseEntity.ok(Map.of("mensaje", authService.reenviarCodigo(dto)));
	}

	// Login con Firebase Auth (Google por Firebase): el frontend manda
	// el ID token de Firebase y el backend responde con el JWT propio.
	// Responde 503 si Firebase no está configurado en el servidor.
	@PostMapping("/firebase")
	public AuthRespuesta loginConFirebase(@Valid @RequestBody FirebaseLoginDTO dto) {
		return authService.loginConFirebase(dto.idToken());
	}

	// Sincroniza la verificación de correo de Firebase (el usuario ya
	// abrió el enlace que Firebase le mandó). Responde 503 si Firebase
	// no está configurado y 400 si el correo aún no está verificado.
	@PostMapping("/verificar-email-firebase")
	public ResponseEntity<Map<String, String>> verificarEmailFirebase(@Valid @RequestBody FirebaseLoginDTO dto) {
		return ResponseEntity.ok(Map.of("mensaje", authService.verificarEmailFirebase(dto.idToken())));
	}

	// Sincroniza la verificación de teléfono de Firebase (Phone Auth
	// por SMS). El token trae el número verificado; el correo solo
	// hace falta cuando el token es solo de teléfono (sin correo).
	@PostMapping("/verificar-telefono-firebase")
	public ResponseEntity<Map<String, String>> verificarTelefonoFirebase(@Valid @RequestBody VerificarTelefonoFirebaseDTO dto) {
		return ResponseEntity.ok(Map.of("mensaje", authService.verificarTelefonoFirebase(dto.idToken(), dto.email())));
	}

	// Perfil del usuario del token Bearer. Lo usa el login con Google,
	// que llega solo con el JWT y necesita nombre y rol para la sesión.
	@GetMapping("/me")
	public UsuarioDTO perfil(@AuthenticationPrincipal UserDetails principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No has iniciado sesión");
		}
		return authService.obtenerPerfil(principal.getUsername());
	}
}
