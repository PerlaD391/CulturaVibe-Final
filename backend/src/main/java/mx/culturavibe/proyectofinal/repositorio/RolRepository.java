package mx.culturavibe.proyectofinal.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.culturavibe.proyectofinal.modelo.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {

	Optional<Rol> findByNombreIgnoreCase(String nombre);
}
