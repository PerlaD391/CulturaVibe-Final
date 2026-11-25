package mx.culturavibe.proyectofinal.modelo;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Código de verificación de 6 dígitos para confirmar el correo y, si
// el usuario tiene teléfono, también el teléfono. Válido 15 minutos y
// de un solo uso. El login NO se bloquea por no estar verificado: la
// verificación confirma el contacto, no es un candado.
@Entity
@Table(name = "codigos_verificacion")
public class CodigoVerificacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@Column(nullable = false, length = 10)
	private String codigo;

	@Column(nullable = false)
	private LocalDateTime expiracion;

	@Column(nullable = false)
	private boolean usado = false;

	@CreationTimestamp
	@Column(name = "creado_en")
	private LocalDateTime creadoEn;

	protected CodigoVerificacion() {
	}

	public CodigoVerificacion(Usuario usuario, String codigo, LocalDateTime expiracion) {
		this.usuario = usuario;
		this.codigo = codigo;
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

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
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

	public LocalDateTime getCreadoEn() {
		return creadoEn;
	}

	public void setCreadoEn(LocalDateTime creadoEn) {
		this.creadoEn = creadoEn;
	}
}
