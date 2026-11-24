package mx.culturavibe.proyectofinal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import mx.culturavibe.proyectofinal.modelo.Categoria;

public record CategoriaDTO(
		Long id,
		@NotBlank(message = "El nombre de la categoría es obligatorio")
		@Size(max = 100, message = "El nombre no puede pasar de 100 caracteres")
		String nombre,
		@Size(max = 255, message = "La descripción no puede pasar de 255 caracteres")
		String descripcion
) {
	public static CategoriaDTO desde(Categoria categoria) {
		return new CategoriaDTO(
				categoria.getId(),
				categoria.getNombre(),
				categoria.getDescripcion()
		);
	}
}
