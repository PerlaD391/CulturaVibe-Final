package mx.culturavibe.proyectofinal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mx.culturavibe.proyectofinal.seguridad.PoliticaPassword;

public record RegistroDTO(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede pasar de 100 caracteres")
		String nombre,
		@NotBlank(message = "El correo es obligatorio")
		@Email(message = "El correo no tiene un formato válido")
		String email,
		@NotBlank(message = "La contraseña es obligatoria")
		@Pattern(regexp = PoliticaPassword.REGEXP, message = PoliticaPassword.MENSAJE)
		String password,
		@Size(max = 20, message = "El teléfono no puede pasar de 20 caracteres")
		String telefono,
		// Rol elegido al registrarse: CLIENTE (por defecto) u ORGANIZADOR
		// (empresa). Opcional; ADMIN no se puede elegir por aquí.
		String rol
) {
}
