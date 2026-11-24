package mx.culturavibe.proyectofinal.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mx.culturavibe.proyectofinal.modelo.Usuario;

// Sin contraseña: la contraseña nunca sale del backend.
public record UsuarioDTO(
		Long id,
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede pasar de 100 caracteres")
		String nombre,
		@NotBlank(message = "El correo es obligatorio")
		@Email(message = "El correo no tiene un formato válido")
		String email,
		@Size(max = 20, message = "El teléfono no puede pasar de 20 caracteres")
		String telefono,
		String rol,
		Boolean activo,
		LocalDateTime creadoEn
) {
	public static UsuarioDTO desde(Usuario usuario) {
		return new UsuarioDTO(
				usuario.getId(),
				usuario.getNombre(),
				usuario.getEmail(),
				usuario.getTelefono(),
				usuario.getRol() == null ? null : usuario.getRol().getNombre(),
				usuario.isActivo(),
				usuario.getCreadoEn()
		);
	}
}
