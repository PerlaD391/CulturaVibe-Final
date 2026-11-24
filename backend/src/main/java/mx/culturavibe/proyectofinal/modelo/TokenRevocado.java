package mx.culturavibe.proyectofinal.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Token JWT revocado al cerrar sesión. El filtro lo consulta en cada
// petición para que un token revocado ya no sirva aunque no haya expirado.
@Entity
@Table(name = "tokens_revocados")
public class TokenRevocado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 500)
	private String token;

	private LocalDateTime expiracion;

	@CreationTimestamp
	@Column(name = "revocado_en")
	private LocalDateTime revocadoEn;

	protected TokenRevocado() {
	}

	public TokenRevocado(String token, LocalDateTime expiracion) {
		this.token = token;
		this.expiracion = expiracion;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public LocalDateTime getExpiracion() {
		return expiracion;
	}

	public void setExpiracion(LocalDateTime expiracion) {
		this.expiracion = expiracion;
	}

	public LocalDateTime getRevocadoEn() {
		return revocadoEn;
	}

	public void setRevocadoEn(LocalDateTime revocadoEn) {
		this.revocadoEn = revocadoEn;
	}
}
