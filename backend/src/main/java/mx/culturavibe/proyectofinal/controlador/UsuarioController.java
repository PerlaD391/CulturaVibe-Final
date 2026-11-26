package mx.culturavibe.proyectofinal.controlador;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import mx.culturavibe.proyectofinal.dto.UsuarioDTO;
import mx.culturavibe.proyectofinal.servicio.AuthService;
import mx.culturavibe.proyectofinal.servicio.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioService usuarios;

	private final AuthService authService;

	public UsuarioController(UsuarioService usuarios, AuthService authService) {
		this.usuarios = usuarios;
		this.authService = authService;
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public Page<UsuarioDTO> listar(@PageableDefault(size = 12) Pageable pageable) {
		return usuarios.listar(pageable);
	}

	// ADMIN o el propio usuario (el principal es la entidad Usuario).
	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #id")
	public UsuarioDTO buscarPorId(@PathVariable Long id) {
		return usuarios.buscarPorId(id);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public UsuarioDTO actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioDTO dto) {
		return usuarios.actualizar(id, dto);
	}

	// El usuario autenticado elimina su propia cuenta (cualquier
	// rol). La ruta literal /me tiene preferencia sobre /{id}. El
	// token actual se revoca para que ya no sirva, con el mismo
	// mecanismo que el logout (AuthService.logout).
	@DeleteMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> eliminarMiCuenta(
			@RequestHeader(value = "Authorization", required = false) String encabezado) {
		usuarios.eliminarCuentaPropia();
		if (encabezado != null && encabezado.startsWith("Bearer ")) {
			authService.logout(encabezado.substring(7));
		}
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		usuarios.eliminar(id);
		return ResponseEntity.noContent().build();
	}
}
