package mx.culturavibe.proyectofinal.dto;

public record AuthRespuesta(
		String token,
		String tipo,
		String email,
		String nombre,
		String rol
) {
	public static AuthRespuesta conToken(String token, String email, String nombre, String rol) {
		return new AuthRespuesta(token, "Bearer", email, nombre, rol);
	}
}
