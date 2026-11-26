package mx.culturavibe.proyectofinal.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.culturavibe.proyectofinal.modelo.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	// Para no dejar la app sin administradores cuando un ADMIN se
	// borra su propia cuenta.
	long countByRolNombreIgnoreCase(String nombre);
}
