package mx.culturavibe.proyectofinal.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.culturavibe.proyectofinal.modelo.Notificacion;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

	// Para borrar la cuenta de un usuario: sus notificaciones se
	// borran antes que el usuario (la FK no tiene CASCADE).
	void deleteByUsuarioId(Long usuarioId);
}
