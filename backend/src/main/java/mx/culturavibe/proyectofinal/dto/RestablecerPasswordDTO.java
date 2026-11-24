package mx.culturavibe.proyectofinal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import mx.culturavibe.proyectofinal.seguridad.PoliticaPassword;

public record RestablecerPasswordDTO(
		@NotBlank(message = "El token es obligatorio")
		String token,
		@NotBlank(message = "La nueva contraseña es obligatoria")
		@Pattern(regexp = PoliticaPassword.REGEXP, message = PoliticaPassword.MENSAJE)
		String nuevaPassword
) {
}
