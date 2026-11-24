package mx.culturavibe.proyectofinal.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.culturavibe.proyectofinal.modelo.TokenRevocado;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, Long> {

	boolean existsByToken(String token);
}
