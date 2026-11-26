package mx.culturavibe.proyectofinal.dto;

import jakarta.validation.constraints.NotBlank;

public record FirebaseLoginDTO(
		@NotBlank(message = "El token de Firebase es obligatorio")
		String idToken
) {
}
