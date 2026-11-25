package mx.culturavibe.proyectofinal.servicio;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.culturavibe.proyectofinal.dto.EventoDTO;
import mx.culturavibe.proyectofinal.modelo.Categoria;
import mx.culturavibe.proyectofinal.modelo.Evento;
import mx.culturavibe.proyectofinal.modelo.Usuario;
import mx.culturavibe.proyectofinal.repositorio.EventoRepository;
import mx.culturavibe.proyectofinal.repositorio.UsuarioRepository;

@Service
@Transactional
public class EventoService {

	private final EventoRepository eventos;

	private final CategoriaService categorias;

	private final UsuarioRepository usuarios;

	public EventoService(EventoRepository eventos, CategoriaService categorias, UsuarioRepository usuarios) {
		this.eventos = eventos;
		this.categorias = categorias;
		this.usuarios = usuarios;
	}

	@Transactional(readOnly = true)
	public Page<EventoDTO> listar(Long categoriaId, String q, LocalDate fecha, Pageable pageable) {
		if (q != null && !q.isBlank()) {
			return eventos.buscarPorTexto(q.trim(), pageable).map(EventoDTO::desde);
		}
		if (categoriaId != null && fecha != null) {
			categorias.buscarEntidad(categoriaId);
			return eventos.findByCategoriaIdAndFecha(categoriaId, fecha, pageable).map(EventoDTO::desde);
		}
		if (categoriaId != null) {
			categorias.buscarEntidad(categoriaId);
			return eventos.findByCategoriaId(categoriaId, pageable).map(EventoDTO::desde);
		}
		if (fecha != null) {
			return eventos.findByFecha(fecha, pageable).map(EventoDTO::desde);
		}
		return eventos.findAll(pageable).map(EventoDTO::desde);
	}

	@Transactional(readOnly = true)
	public EventoDTO buscarPorId(Long id) {
		return EventoDTO.desde(buscarEntidad(id));
	}

	// Mis eventos (ORGANIZADOR): solo los del usuario en sesión.
	@Transactional(readOnly = true)
	public Page<EventoDTO> misEventos(Pageable pageable) {
		return eventos.findByOrganizadorId(usuarioActual().getId(), pageable).map(EventoDTO::desde);
	}

	@Transactional(readOnly = true)
	public Evento buscarEntidad(Long id) {
		return eventos.findById(id)
				.orElseThrow(() -> new NoSuchElementException("No existe un evento con id " + id));
	}

	// Crear: ADMIN o ORGANIZADOR. El organizador del evento es el usuario
	// autenticado, salvo que un ADMIN indique otro organizadorId.
	public EventoDTO crear(EventoDTO dto) {
		Usuario actual = usuarioActual();
		Categoria categoria = categorias.buscarEntidad(dto.categoriaId());
		Usuario organizador = actual;
		if (dto.organizadorId() != null && esAdmin(actual)) {
			organizador = usuarios.findById(dto.organizadorId())
					.orElseThrow(() -> new NoSuchElementException(
							"No existe un usuario con id " + dto.organizadorId()));
		}
		Evento evento = dto.aEvento(categoria, organizador);
		return EventoDTO.desde(eventos.save(evento));
	}

	// Actualizar y eliminar: ADMIN, o el ORGANIZADOR dueño del evento.
	public EventoDTO actualizar(Long id, EventoDTO dto) {
		Evento evento = buscarEntidad(id);
		verificarPermisoSobre(evento);
		Categoria categoria = categorias.buscarEntidad(dto.categoriaId());
		dto.aplicarA(evento, categoria);
		return EventoDTO.desde(eventos.save(evento));
	}

	public void eliminar(Long id) {
		Evento evento = buscarEntidad(id);
		verificarPermisoSobre(evento);
		eventos.delete(evento);
	}

	private void verificarPermisoSobre(Evento evento) {
		Usuario actual = usuarioActual();
		boolean esDuenio = evento.getOrganizador() != null
				&& evento.getOrganizador().getId().equals(actual.getId());
		if (!esAdmin(actual) && !esDuenio) {
			throw new AccessDeniedException("Solo el organizador del evento o un administrador puede modificarlo");
		}
	}

	private boolean esAdmin(Usuario usuario) {
		return usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
	}

	private Usuario usuarioActual() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return usuarios.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new NoSuchElementException("No existe el usuario autenticado"));
	}
}
