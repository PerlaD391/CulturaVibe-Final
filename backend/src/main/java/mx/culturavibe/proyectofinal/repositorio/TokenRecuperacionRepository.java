package mx.culturavibe.proyectofinal.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.culturavibe.proyectofinal.modelo.TokenRecuperacion;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

	Optional<TokenRecuperacion> findByToken(String token);
}
