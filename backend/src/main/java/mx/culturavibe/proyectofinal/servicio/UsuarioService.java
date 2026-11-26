package mx.culturavibe.proyectofinal.servicio;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.culturavibe.proyectofinal.dto.UsuarioDTO;
import mx.culturavibe.proyectofinal.modelo.Evento;
import mx.culturavibe.proyectofinal.modelo.Rol;
import mx.culturavibe.proyectofinal.modelo.Usuario;
import mx.culturavibe.proyectofinal.repositorio.CodigoVerificacionRepository;
import mx.culturavibe.proyectofinal.repositorio.EventoRepository;
import mx.culturavibe.proyectofinal.repositorio.NotificacionRepository;
import mx.culturavibe.proyectofinal.repositorio.ReservaRepository;
import mx.culturavibe.proyectofinal.repositorio.RolRepository;
import mx.culturavibe.proyectofinal.repositorio.TokenRecuperacionRepository;
import mx.culturavibe.proyectofinal.repositorio.UsuarioRepository;

// Gestión de usuarios, solo para ADMIN (salvo la eliminación de la
// propia cuenta, que la hace el usuario autenticado).
@Service
@Transactional
public class UsuarioService {

	private final UsuarioRepository usuarios;

	private final RolRepository roles;

	private final ReservaRepository reservas;

	private final NotificacionRepository notificaciones;

	private final TokenRecuperacionRepository tokensRecuperacion;

	private final CodigoVerificacionRepository codigosVerificacion;

	private final EventoRepository eventos;

	public UsuarioService(UsuarioRepository usuarios, RolRepository roles,
			ReservaRepository reservas, NotificacionRepository notificaciones,
			TokenRecuperacionRepository tokensRecuperacion,
			CodigoVerificacionRepository codigosVerificacion,
			EventoRepository eventos) {
		this.usuarios = usuarios;
		this.roles = roles;
		this.reservas = reservas;
		this.notificaciones = notificaciones;
		this.tokensRecuperacion = tokensRecuperacion;
		this.codigosVerificacion = codigosVerificacion;
		this.eventos = eventos;
	}

	@Transactional(readOnly = true)
	public Page<UsuarioDTO> listar(Pageable pageable) {
		return usuarios.findAll(pageable).map(UsuarioDTO::desde);
	}

	@Transactional(readOnly = true)
	public UsuarioDTO buscarPorId(Long id) {
		return UsuarioDTO.desde(buscarEntidad(id));
	}

	@Transactional(readOnly = true)
	public Usuario buscarEntidad(Long id) {
		return usuarios.findById(id)
				.orElseThrow(() -> new NoSuchElementException("No existe un usuario con id " + id));
	}

	// Actualiza nombre, teléfono, rol y activo. El correo no se cambia
	// aquí porque es el identificador de inicio de sesión.
	public UsuarioDTO actualizar(Long id, UsuarioDTO dto) {
		Usuario usuario = buscarEntidad(id);
		usuario.setNombre(dto.nombre());
		usuario.setTelefono(dto.telefono());
		if (dto.activo() != null) {
			usuario.setActivo(dto.activo());
		}
		if (dto.rol() != null && !dto.rol().isBlank()) {
			Rol rol = roles.findByNombreIgnoreCase(dto.rol())
					.orElseThrow(() -> new NoSuchElementException("No existe el rol " + dto.rol()));
			usuario.setRol(rol);
		}
		return UsuarioDTO.desde(usuarios.save(usuario));
	}

	// Borra el usuario y todos sus dependientes. Antes era un delete
	// simple y las FK sin CASCADE lo bloqueaban con 409 ("El registro
	// ya existe o está en uso") en cuanto el usuario tenía reservas,
	// notificaciones, tokens o eventos organizados.
	public void eliminar(Long id) {
		Usuario usuario = buscarEntidad(id);
		borrarDependientes(usuario);
		usuarios.delete(usuario);
	}

	// El usuario autenticado elimina su propia cuenta. Un ADMIN no
	// puede borrarse si es el único administrador que queda, para no
	// dejar la app sin administradores. La revocación del token la
	// hace el controlador con AuthService.logout (el mismo mecanismo
	// que el cierre de sesión).
	public void eliminarCuentaPropia() {
		Usuario usuario = usuarioActual();
		String rol = usuario.getRol() == null ? null : usuario.getRol().getNombre();
		if ("ADMIN".equalsIgnoreCase(rol) && usuarios.countByRolNombreIgnoreCase("ADMIN") <= 1) {
			throw new IllegalStateException("No puedes eliminar tu cuenta porque eres el único administrador.");
		}
		eliminar(usuario.getId());
	}

	// Borra los dependientes de un usuario en el orden que piden las
	// FK: primero las reservas (las del usuario y las de los eventos
	// que organizó), luego esos eventos, y después notificaciones,
	// tokens de recuperación y códigos de verificación.
	private void borrarDependientes(Usuario usuario) {
		Long id = usuario.getId();
		reservas.deleteByUsuarioId(id);
		List<Evento> propios = eventos.findByOrganizadorId(id);
		for (Evento evento : propios) {
			reservas.deleteByEventoId(evento.getId());
		}
		eventos.deleteAll(propios);
		notificaciones.deleteByUsuarioId(id);
		tokensRecuperacion.deleteByUsuarioId(id);
		codigosVerificacion.deleteByUsuarioId(id);
	}

	private Usuario usuarioActual() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return usuarios.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new NoSuchElementException("No existe el usuario autenticado"));
	}
}
