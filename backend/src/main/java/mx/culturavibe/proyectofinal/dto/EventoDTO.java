package mx.culturavibe.proyectofinal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mx.culturavibe.proyectofinal.modelo.Categoria;
import mx.culturavibe.proyectofinal.modelo.Evento;
import mx.culturavibe.proyectofinal.modelo.Usuario;

// 14 campos del evento + ids/nombres de categoría y organizador.
// Los artistas van como texto separado por comas ("A, B, C").
public record EventoDTO(
		Long id,
		@NotBlank(message = "El título del evento es obligatorio")
		@Size(max = 200, message = "El título no puede pasar de 200 caracteres")
		String titulo,
		String descripcion,
		@NotNull(message = "La fecha del evento es obligatoria")
		LocalDate fecha,
		LocalTime hora,
		String direccion,
		@NotBlank(message = "La ciudad del evento es obligatoria")
		String ciudad,
		@NotBlank(message = "El estado del evento es obligatorio")
		String estado,
		@NotNull(message = "La categoría del evento es obligatoria")
		Long categoriaId,
		String categoriaNombre,
		Long organizadorId,
		String organizadorNombre,
		@DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
		BigDecimal precio,
		String estadoVenta,
		String url,
		String imagenUrl,
		String artistas,
		LocalDateTime creadoEn
) {
	public static EventoDTO desde(Evento evento) {
		return new EventoDTO(
				evento.getId(),
				evento.getTitulo(),
				evento.getDescripcion(),
				evento.getFecha(),
				evento.getHora(),
				evento.getDireccion(),
				evento.getCiudad(),
				evento.getEstado(),
				evento.getCategoria() == null ? null : evento.getCategoria().getId(),
				evento.getCategoria() == null ? null : evento.getCategoria().getNombre(),
				evento.getOrganizador() == null ? null : evento.getOrganizador().getId(),
				evento.getOrganizador() == null ? null : evento.getOrganizador().getNombre(),
				evento.getPrecio(),
				evento.getEstadoVenta(),
				evento.getUrl(),
				evento.getImagenUrl(),
				evento.getArtistas(),
				evento.getCreadoEn()
		);
	}

	public Evento aEvento(Categoria categoria, Usuario organizador) {
		Evento evento = new Evento();
		evento.setTitulo(titulo);
		evento.setDescripcion(descripcion);
		evento.setFecha(fecha);
		evento.setHora(hora);
		evento.setDireccion(direccion);
		evento.setCiudad(ciudad);
		evento.setEstado(estado);
		evento.setCategoria(categoria);
		evento.setOrganizador(organizador);
		evento.setPrecio(precio);
		evento.setEstadoVenta(estadoVenta == null ? "onsale" : estadoVenta);
		evento.setUrl(url);
		evento.setImagenUrl(imagenUrl);
		evento.setArtistas(artistas);
		return evento;
	}

	public void aplicarA(Evento evento, Categoria categoria) {
		evento.setTitulo(titulo);
		evento.setDescripcion(descripcion);
		evento.setFecha(fecha);
		evento.setHora(hora);
		evento.setDireccion(direccion);
		evento.setCiudad(ciudad);
		evento.setEstado(estado);
		evento.setCategoria(categoria);
		evento.setPrecio(precio);
		evento.setEstadoVenta(estadoVenta);
		evento.setUrl(url);
		evento.setImagenUrl(imagenUrl);
		evento.setArtistas(artistas);
	}
}
