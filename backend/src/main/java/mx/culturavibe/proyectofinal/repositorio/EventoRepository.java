package mx.culturavibe.proyectofinal.repositorio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.culturavibe.proyectofinal.modelo.Evento;

// Los listados cargan categoría y organizador en la misma consulta
// (@EntityGraph / JOIN FETCH) para no hacer una consulta extra por evento (N+1).
public interface EventoRepository extends JpaRepository<Evento, Long> {

	@Override
	@EntityGraph(attributePaths = { "categoria", "organizador" })
	Page<Evento> findAll(Pageable pageable);

	@EntityGraph(attributePaths = { "categoria", "organizador" })
	Page<Evento> findByCategoriaId(Long categoriaId, Pageable pageable);

	@EntityGraph(attributePaths = { "categoria", "organizador" })
	Page<Evento> findByFecha(LocalDate fecha, Pageable pageable);

	@EntityGraph(attributePaths = { "categoria", "organizador" })
	Page<Evento> findByCategoriaIdAndFecha(Long categoriaId, LocalDate fecha, Pageable pageable);

	@Query(value = "SELECT e FROM Evento e JOIN FETCH e.categoria LEFT JOIN FETCH e.organizador "
			+ "WHERE LOWER(e.titulo) LIKE LOWER(CONCAT('%', :q, '%')) "
			+ "OR LOWER(e.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))",
			countQuery = "SELECT COUNT(e) FROM Evento e "
					+ "WHERE LOWER(e.titulo) LIKE LOWER(CONCAT('%', :q, '%')) "
					+ "OR LOWER(e.descripcion) LIKE LOWER(CONCAT('%', :q, '%'))")
	Page<Evento> buscarPorTexto(@Param("q") String q, Pageable pageable);

	@EntityGraph(attributePaths = { "categoria", "organizador" })
	Page<Evento> findByOrganizadorId(Long organizadorId, Pageable pageable);

	// Para borrar la cuenta de un organizador: sus eventos sin
	// paginar, para borrar antes las reservas que tengan.
	List<Evento> findByOrganizadorId(Long organizadorId);

	// Para la sincronización con Ticketmaster: un evento importado ya
	// existe si hay otro con el mismo id de Ticketmaster (external_id).
	boolean existsByExternalId(String externalId);
}
