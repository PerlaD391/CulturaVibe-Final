package mx.culturavibe.proyectofinal.repositorio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import mx.culturavibe.proyectofinal.modelo.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

	@EntityGraph(attributePaths = { "usuario", "evento" })
	Page<Reserva> findByUsuarioId(Long usuarioId, Pageable pageable);

	@EntityGraph(attributePaths = { "usuario", "evento" })
	Page<Reserva> findByEventoId(Long eventoId, Pageable pageable);

	@EntityGraph(attributePaths = { "usuario", "evento" })
	Page<Reserva> findByEventoIdAndEstado(Long eventoId, String estado, Pageable pageable);

	@EntityGraph(attributePaths = { "usuario", "evento" })
	Page<Reserva> findByEstado(String estado, Pageable pageable);

	// Para borrar la cuenta de un usuario o los eventos de un
	// organizador: se borran las reservas dependientes antes que el
	// usuario o el evento (las FK no tienen CASCADE).
	void deleteByUsuarioId(Long usuarioId);

	void deleteByEventoId(Long eventoId);

	// Reservas de los eventos de un organizador (para el listado de ORGANIZADOR).
	@EntityGraph(attributePaths = { "usuario", "evento" })
	Page<Reserva> findByEventoOrganizadorId(Long organizadorId, Pageable pageable);
}
