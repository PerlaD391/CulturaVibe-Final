package mx.culturavibe.proyectofinal.servicio;

import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.culturavibe.proyectofinal.dto.ReservaDTO;
import mx.culturavibe.proyectofinal.modelo.Evento;
import mx.culturavibe.proyectofinal.modelo.Reserva;
import mx.culturavibe.proyectofinal.modelo.Usuario;
import mx.culturavibe.proyectofinal.repositorio.EventoRepository;
import mx.culturavibe.proyectofinal.repositorio.ReservaRepository;
import mx.culturavibe.proyectofinal.repositorio.UsuarioRepository;

@Service
@Transactional
public class ReservaService {

	private final ReservaRepository reservas;

	private final EventoRepository eventos;

	private final UsuarioRepository usuarios;

	private final NotificacionService notificaciones;

	private final EmailService correo;

	public ReservaService(ReservaRepository reservas, EventoRepository eventos,
			UsuarioRepository usuarios, NotificacionService notificaciones, EmailService correo) {
		this.reservas = reservas;
		this.eventos = eventos;
		this.usuarios = usuarios;
		this.notificaciones = notificaciones;
		this.correo = correo;
	}

	// Crear reserva: solo se puede reservar un evento a la venta.
	// Al crearla se envía correo de confirmación al usuario y, si tiene
	// teléfono, SMS por Vonage/Twilio y WhatsApp por Cloud API (cada
	// canal solo si está configurado).
	public ReservaDTO crear(ReservaDTO dto) {
		Usuario usuario = usuarioActual();
		Evento evento = eventos.findById(dto.eventoId())
				.orElseThrow(() -> new NoSuchElementException("No existe un evento con id " + dto.eventoId()));
		if (evento.getEstadoVenta() != null && !"onsale".equalsIgnoreCase(evento.getEstadoVenta())) {
			throw new IllegalStateException("La venta de boletos para este evento ya está cerrada");
		}
		Reserva reserva = new Reserva(usuario, evento, dto.cantidad());
		Reserva guardada = reservas.save(reserva);
		correo.enviarConfirmacionReserva(usuario.getEmail(), usuario.getNombre(),
				evento.getTitulo(), dto.cantidad());
		notificaciones.enviarConfirmacionReserva(usuario, evento, dto.cantidad());
		return ReservaDTO.desde(guardada);
	}

	@Transactional(readOnly = true)
	public Page<ReservaDTO> misReservas(Pageable pageable) {
		return reservas.findByUsuarioId(usuarioActual().getId(), pageable).map(ReservaDTO::desde);
	}

	// ADMIN ve todas; ORGANIZADOR solo las de sus eventos.
	@Transactional(readOnly = true)
	public Page<ReservaDTO> listarTodas(Long eventoId, String estado, Pageable pageable) {
		Usuario actual = usuarioActual();
		String rol = actual.getRol().getNombre();
		if ("ORGANIZADOR".equals(rol)) {
			return reservas.findByEventoOrganizadorId(actual.getId(), pageable).map(ReservaDTO::desde);
		}
		if (eventoId != null && estado != null) {
			return reservas.findByEventoIdAndEstado(eventoId, estado, pageable).map(ReservaDTO::desde);
		}
		if (eventoId != null) {
			return reservas.findByEventoId(eventoId, pageable).map(ReservaDTO::desde);
		}
		if (estado != null) {
			return reservas.findByEstado(estado, pageable).map(ReservaDTO::desde);
		}
		return reservas.findAll(pageable).map(ReservaDTO::desde);
	}

	// Cancelar: pone estado=CANCELADA. Puede hacerlo el dueño de la
	// reserva, un ADMIN, o el ORGANIZADOR del evento.
	public ReservaDTO cancelar(Long id) {
		Reserva reserva = reservas.findById(id)
				.orElseThrow(() -> new NoSuchElementException("No existe una reserva con id " + id));
		Usuario actual = usuarioActual();
		String rol = actual.getRol().getNombre();
		boolean esDuenio = reserva.getUsuario().getId().equals(actual.getId());
		boolean esOrganizadorDelEvento = reserva.getEvento().getOrganizador() != null
				&& reserva.getEvento().getOrganizador().getId().equals(actual.getId());
		if (!"ADMIN".equals(rol) && !esDuenio && !esOrganizadorDelEvento) {
			throw new AccessDeniedException("No tienes permiso para cancelar esta reserva");
		}
		if ("CANCELADA".equals(reserva.getEstado())) {
			throw new IllegalStateException("La reserva ya está cancelada");
		}
		reserva.setEstado("CANCELADA");
		return ReservaDTO.desde(reservas.save(reserva));
	}

	private Usuario usuarioActual() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return usuarios.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new NoSuchElementException("No existe el usuario autenticado"));
	}
}
