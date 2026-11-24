package mx.culturavibe.proyectofinal.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Token de un solo uso para restablecer la contraseña. Expira en 1 hora.
@Entity
@Table(name = "tokens_recuperacion")
public class TokenRecuperacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(nullable = false, unique = true, length = 255)
	private String token;

	@Column(nullable = false)
	private LocalDateTime expiracion;

	@Column(nullable = false)
	private boolean usado = false;

	protected TokenRecuperacion() {
	}

	public TokenRecuperacion(Usuario usuario, String token, LocalDateTime expiracion) {
		this.usuario = usuario;
		this.token = token;
		this.expiracion = expiracion;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
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

	public boolean isUsado() {
		return usado;
	}

	public void setUsado(boolean usado) {
		this.usado = usado;
	}
}
