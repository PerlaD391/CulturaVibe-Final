package mx.culturavibe.proyectofinal.config;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

// Todos los errores de la API salen con el mismo cuerpo JSON:
// {timestamp, status, error, message, path} y, en validación,
// además "errores" con un mensaje por campo.
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e,
			HttpServletRequest request) {
		Map<String, String> campos = new HashMap<>();
		for (FieldError campo : e.getBindingResult().getFieldErrors()) {
			campos.put(campo.getField(), campo.getDefaultMessage());
		}
		Map<String, Object> cuerpo = cuerpo(HttpStatus.BAD_REQUEST, "Validación fallida", request);
		cuerpo.put("errores", campos);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, Object>> violacionRestriccion(ConstraintViolationException e,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(cuerpo(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), request));
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<Map<String, Object>> noEncontrado(NoSuchElementException e, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(cuerpo(HttpStatus.NOT_FOUND, e.getMessage(), request));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> solicitudIncorrecta(IllegalArgumentException e,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(cuerpo(HttpStatus.BAD_REQUEST, e.getMessage(), request));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, Object>> conflicto(IllegalStateException e, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(cuerpo(HttpStatus.CONFLICT, e.getMessage(), request));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException e,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(cuerpo(HttpStatus.CONFLICT, "El registro ya existe o está en uso", request));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, Object>> credencialesInvalidas(BadCredentialsException e,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(cuerpo(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos", request));
	}

	// Cuenta suspendida (por ejemplo tras 5 intentos fallidos): el
	// proveedor de autenticación lanza DisabledException cuando la
	// cuenta tiene activo=false. Se responde 403 con el mensaje de
	// recuperación en vez de un 500.
	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<Map<String, Object>> cuentaSuspendida(DisabledException e,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(cuerpo(HttpStatus.FORBIDDEN,
						"Tu cuenta está suspendida por varios intentos fallidos. Revisa tu correo para recuperarla.",
						request));
	}

	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<Map<String, Object>> usuarioNoEncontrado(UsernameNotFoundException e,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(cuerpo(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos", request));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, Object>> accesoDenegado(AccessDeniedException e, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(cuerpo(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción", request));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> estadoRespuesta(ResponseStatusException e, HttpServletRequest request) {
		HttpStatus estado = HttpStatus.resolve(e.getStatusCode().value());
		if (estado == null) {
			estado = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return ResponseEntity.status(estado)
				.body(cuerpo(estado, e.getReason(), request));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> errorGeneral(Exception e, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request));
	}

	private Map<String, Object> cuerpo(HttpStatus estado, String mensaje, HttpServletRequest request) {
		Map<String, Object> cuerpo = new HashMap<>();
		cuerpo.put("timestamp", LocalDateTime.now().toString());
		cuerpo.put("status", estado.value());
		cuerpo.put("error", estado.getReasonPhrase());
		cuerpo.put("message", mensaje);
		cuerpo.put("path", request.getRequestURI());
		return cuerpo;
	}
}
