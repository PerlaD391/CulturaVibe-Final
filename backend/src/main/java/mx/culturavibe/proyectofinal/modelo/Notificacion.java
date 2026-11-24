package mx.culturavibe.proyectofinal.modelo;

import java.time.LocalDateTime;

import jakarta.persistence.Column;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Registro de cada notificación enviada (SMS o WhatsApp) por Twilio.
// tipo: SMS o WHATSAPP. estado: ENVIADA o FALLIDA.
@Entity
@Table(name = "notificaciones")
public class Notificacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	@Column(length = 20)
	private String tipo;

	@Column(columnDefinition = "TEXT")
	private String mensaje;

	@CreationTimestamp
	@Column(name = "fecha_envio")
	private LocalDateTime fechaEnvio;

	@Column(length = 20)
	private String estado;

	public Notificacion() {
	}

	public Notificacion(Usuario usuario, String tipo, String mensaje, String estado) {
		this.usuario = usuario;
		this.tipo = tipo;
		this.mensaje = mensaje;
		this.estado = estado;
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

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	public LocalDateTime getFechaEnvio() {
		return fechaEnvio;
	}

	public void setFechaEnvio(LocalDateTime fechaEnvio) {
		this.fechaEnvio = fechaEnvio;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}
}
