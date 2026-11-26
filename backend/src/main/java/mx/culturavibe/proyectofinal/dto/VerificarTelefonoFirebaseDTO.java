package mx.culturavibe.proyectofinal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerificarTelefonoFirebaseDTO(
		@NotBlank(message = "El token de Firebase es obligatorio")
		String idToken,
		// Correo de la cuenta. Opcional: solo hace falta cuando el
		// token es solo de teléfono (sin correo).
		@Email(message = "El correo no es válido")
		String email
) {
}
