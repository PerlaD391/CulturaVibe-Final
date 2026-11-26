package mx.culturavibe.proyectofinal.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.culturavibe.proyectofinal.modelo.CodigoVerificacion;
import mx.culturavibe.proyectofinal.modelo.Usuario;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

	List<CodigoVerificacion> findByUsuarioAndUsadoFalse(Usuario usuario);

	Optional<CodigoVerificacion> findFirstByUsuarioAndUsadoFalseOrderByExpiracionDesc(Usuario usuario);

	// Para borrar la cuenta de un usuario: sus códigos de
	// verificación se borran antes que el usuario.
	void deleteByUsuarioId(Long usuarioId);
}
