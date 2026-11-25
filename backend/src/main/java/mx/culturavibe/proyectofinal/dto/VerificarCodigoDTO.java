package mx.culturavibe.proyectofinal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificarCodigoDTO(
		@NotBlank(message = "El correo es obligatorio")
		@Email(message = "El correo no tiene un formato válido")
		String email,
		@NotBlank(message = "El código es obligatorio")
		@Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos")
		String codigo
) {
}
