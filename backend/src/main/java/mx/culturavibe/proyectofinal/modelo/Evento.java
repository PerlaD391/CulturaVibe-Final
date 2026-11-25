package mx.culturavibe.proyectofinal.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

// Evento cultural. Los artistas se guardan como texto separado por comas
// (columna artistas TEXT), igual que los recibe el frontend.
@Entity
@Table(name = "eventos")
public class Evento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String titulo;

	@Column(columnDefinition = "TEXT")
	private String descripcion;

	private LocalDate fecha;

	private LocalTime hora;

	@Column(length = 255)
	private String direccion;

	@Column(length = 100)
	private String ciudad;

	@Column(length = 100)
	private String estado;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "categoria_id")
	private Categoria categoria;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "organizador_id")
	private Usuario organizador;

	@Column(precision = 10, scale = 2)
	private BigDecimal precio;

	@Column(name = "estado_venta", length = 30)
	private String estadoVenta;

	@Column(length = 500)
	private String url;

	@Column(name = "imagen_url", length = 500)
	private String imagenUrl;

	@Column(columnDefinition = "TEXT")
	private String artistas;

	// Origen del evento: "LOCAL" (semilla o creado en la app) o
	// "TICKETMASTER" (importado por la sincronización). La columna
	// tiene DEFAULT 'LOCAL' en la migración V2.
	@Column(length = 20, nullable = false)
	private String fuente = "LOCAL";

	// Id del evento en Ticketmaster (solo para fuente TICKETMASTER).
	// Único en la base de datos: evita importar el mismo evento dos veces.
	@Column(name = "external_id", length = 100)
	private String externalId;

	@CreationTimestamp
	@Column(name = "creado_en")
	private LocalDateTime creadoEn;

	public Evento() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public void setFecha(LocalDate fecha) {
		this.fecha = fecha;
	}

	public LocalTime getHora() {
		return hora;
	}

	public void setHora(LocalTime hora) {
		this.hora = hora;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public String getCiudad() {
		return ciudad;
	}

	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}

	public Usuario getOrganizador() {
		return organizador;
	}

	public void setOrganizador(Usuario organizador) {
		this.organizador = organizador;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public void setPrecio(BigDecimal precio) {
		this.precio = precio;
	}

	public String getEstadoVenta() {
		return estadoVenta;
	}

	public void setEstadoVenta(String estadoVenta) {
		this.estadoVenta = estadoVenta;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getImagenUrl() {
		return imagenUrl;
	}

	public void setImagenUrl(String imagenUrl) {
		this.imagenUrl = imagenUrl;
	}

	public String getArtistas() {
		return artistas;
	}

	public void setArtistas(String artistas) {
		this.artistas = artistas;
	}

	public String getFuente() {
		return fuente;
	}

	public void setFuente(String fuente) {
		this.fuente = fuente;
	}

	public String getExternalId() {
		return externalId;
	}

	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}

	public LocalDateTime getCreadoEn() {
		return creadoEn;
	}

	public void setCreadoEn(LocalDateTime creadoEn) {
		this.creadoEn = creadoEn;
	}
}
