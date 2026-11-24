package mx.culturavibe.proyectofinal.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import mx.culturavibe.proyectofinal.modelo.Reserva;

public record ReservaDTO(
		Long id,
		Long usuarioId,
		String usuarioNombre,
		@NotNull(message = "El evento es obligatorio")
		Long eventoId,
		String eventoTitulo,
		LocalDateTime fechaReserva,
		String estado,
		@Min(value = 1, message = "La cantidad debe ser al menos 1")
		int cantidad
) {
	public static ReservaDTO desde(Reserva reserva) {
		return new ReservaDTO(
				reserva.getId(),
				reserva.getUsuario() == null ? null : reserva.getUsuario().getId(),
				reserva.getUsuario() == null ? null : reserva.getUsuario().getNombre(),
				reserva.getEvento() == null ? null : reserva.getEvento().getId(),
				reserva.getEvento() == null ? null : reserva.getEvento().getTitulo(),
				reserva.getFechaReserva(),
				reserva.getEstado(),
				reserva.getCantidad()
		);
	}
}
