package mx.culturavibe.proyectofinal.modelo;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

// Usuario del sistema. Implementa UserDetails para que Spring Security
// lo use directamente como principal (así @PreAuthorize puede leer su id).
@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	// La contraseña nunca se expone en las respuestas JSON.
	@JsonIgnore
	@Column(nullable = false, length = 255)
	private String password;

	@Column(length = 20)
	private String telefono;

	// EAGER está bien aquí: el rol se necesita en cada petición autenticada.
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "rol_id", nullable = false)
	private Rol rol;

	@Column(nullable = false)
	private boolean activo = true;

	// Intentos fallidos de login seguidos. Al llegar a 5 la cuenta se
	// suspende (activo=false) y se manda el correo de recuperación.
	// Un login correcto lo pone en 0.
	@Column(name = "intentos_fallidos", nullable = false)
	private int intentosFallidos = 0;

	// Verificación de contacto (no es un candado: el login no se
	// bloquea por no estar verificado).
	@Column(name = "email_verificado", nullable = false)
	private boolean emailVerificado = false;

	@Column(name = "telefono_verificado", nullable = false)
	private boolean telefonoVerificado = false;

	@CreationTimestamp
	@Column(name = "creado_en")
	private LocalDateTime creadoEn;

	protected Usuario() {
	}

	public Usuario(String nombre, String email, String password, String telefono, Rol rol) {
		this.nombre = nombre;
		this.email = email;
		this.password = password;
		this.telefono = telefono;
		this.rol = rol;
	}

	// --- UserDetails: la autoridad es ROLE_<nombre del rol> ---
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + rol.getNombre()));
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return activo;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public int getIntentosFallidos() {
		return intentosFallidos;
	}

	public void setIntentosFallidos(int intentosFallidos) {
		this.intentosFallidos = intentosFallidos;
	}

	public boolean isEmailVerificado() {
		return emailVerificado;
	}

	public void setEmailVerificado(boolean emailVerificado) {
		this.emailVerificado = emailVerificado;
	}

	public boolean isTelefonoVerificado() {
		return telefonoVerificado;
	}

	public void setTelefonoVerificado(boolean telefonoVerificado) {
		this.telefonoVerificado = telefonoVerificado;
	}

	public LocalDateTime getCreadoEn() {
		return creadoEn;
	}

	public void setCreadoEn(LocalDateTime creadoEn) {
		this.creadoEn = creadoEn;
	}
}
