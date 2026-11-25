package mx.culturavibe.proyectofinal.seguridad;

// Política de contraseña, la misma en backend y frontend:
// mínimo 8 caracteres, una mayúscula, un número y un carácter especial.
public final class PoliticaPassword {

	public static final String REGEXP = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";

	public static final String MENSAJE = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un carácter especial";

	private PoliticaPassword() {
	}
}
