package mx.culturavibe.proyectofinal.servicio;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.culturavibe.proyectofinal.dto.AuthRespuesta;
import mx.culturavibe.proyectofinal.dto.LoginDTO;
import mx.culturavibe.proyectofinal.dto.OlvidoPasswordDTO;
import mx.culturavibe.proyectofinal.dto.RegistroDTO;
import mx.culturavibe.proyectofinal.dto.RestablecerPasswordDTO;
import mx.culturavibe.proyectofinal.modelo.Rol;
import mx.culturavibe.proyectofinal.modelo.TokenRecuperacion;
import mx.culturavibe.proyectofinal.modelo.TokenRevocado;
import mx.culturavibe.proyectofinal.modelo.Usuario;
import mx.culturavibe.proyectofinal.repositorio.RolRepository;
import mx.culturavibe.proyectofinal.repositorio.TokenRecuperacionRepository;
import mx.culturavibe.proyectofinal.repositorio.TokenRevocadoRepository;
import mx.culturavibe.proyectofinal.repositorio.UsuarioRepository;

@Service
@Transactional
public class AuthService implements UserDetailsService {

	private final UsuarioRepository usuarios;

	private final RolRepository roles;

	private final TokenRevocadoRepository tokensRevocados;

	private final TokenRecuperacionRepository tokensRecuperacion;

	private final PasswordEncoder passwordEncoder;

	private final ObjectProvider<AuthenticationManager> authenticationManager;

	private final JwtService jwtService;

	private final EmailService emailService;

	public AuthService(UsuarioRepository usuarios, RolRepository roles,
			TokenRevocadoRepository tokensRevocados,
			TokenRecuperacionRepository tokensRecuperacion,
			PasswordEncoder passwordEncoder,
			ObjectProvider<AuthenticationManager> authenticationManager,
			JwtService jwtService, EmailService emailService) {
		this.usuarios = usuarios;
		this.roles = roles;
		this.tokensRevocados = tokensRevocados;
		this.tokensRecuperacion = tokensRecuperacion;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.emailService = emailService;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return usuarios.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con el correo " + email));
	}

	// El registro público crea CLIENTE por defecto; también se puede
	// elegir ORGANIZADOR (empresa). El rol ADMIN no se puede elegir
	// al registrarse; lo asigna otro ADMIN desde /api/usuarios.
	public AuthRespuesta registrar(RegistroDTO dto) {
		if (usuarios.existsByEmailIgnoreCase(dto.email())) {
			throw new IllegalStateException("Ya existe una cuenta con el correo " + dto.email());
		}
		String rolPedido = dto.rol() == null ? "" : dto.rol().trim();
		String nombreRol;
		if (rolPedido.isEmpty() || rolPedido.equalsIgnoreCase("CLIENTE")) {
			nombreRol = "CLIENTE";
		} else if (rolPedido.equalsIgnoreCase("ORGANIZADOR")) {
			nombreRol = "ORGANIZADOR";
		} else if (rolPedido.equalsIgnoreCase("ADMIN")) {
			throw new IllegalArgumentException("El rol ADMIN no se puede elegir al registrarse");
		} else {
			throw new IllegalArgumentException("El rol " + dto.rol() + " no es válido");
		}
		Rol rol = roles.findByNombreIgnoreCase(nombreRol)
				.orElseThrow(() -> new IllegalStateException("No existe el rol " + nombreRol + "; revisa el seed de la base de datos"));
		Usuario usuario = new Usuario(dto.nombre(), dto.email(),
				passwordEncoder.encode(dto.password()), dto.telefono(), rol);
		usuarios.save(usuario);
		emailService.enviarBienvenida(usuario.getEmail(), usuario.getNombre());
		String token = jwtService.generarToken(usuario.getEmail());
		return AuthRespuesta.conToken(token, usuario.getEmail(), usuario.getNombre(), rol.getNombre());
	}

	public AuthRespuesta login(LoginDTO dto) {
		authenticationManager.getObject().authenticate(
				new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));
		Usuario usuario = usuarios.findByEmailIgnoreCase(dto.email())
				.orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con el correo " + dto.email()));
		String token = jwtService.generarToken(usuario.getEmail());
		return AuthRespuesta.conToken(token, usuario.getEmail(), usuario.getNombre(),
				usuario.getRol().getNombre());
	}

	// Guarda el token en tokens_revocados para que ya no sirva.
	public void logout(String token) {
		if (token == null || token.isBlank() || tokensRevocados.existsByToken(token)) {
			return;
		}
		LocalDateTime expiracion;
		try {
			expiracion = jwtService.extraerExpiracion(token);
		} catch (Exception e) {
			expiracion = null;
		}
		tokensRevocados.save(new TokenRevocado(token, expiracion));
	}

	// Genera un token UUID válido 1 hora y envía el enlace por correo.
	// Si el correo no existe, no se revela: se responde igual.
	public void solicitarRecuperacion(OlvidoPasswordDTO dto) {
		usuarios.findByEmailIgnoreCase(dto.email()).ifPresent(usuario -> {
			String token = UUID.randomUUID().toString();
			TokenRecuperacion registro = new TokenRecuperacion(usuario, token,
					LocalDateTime.now().plusHours(1));
			tokensRecuperacion.save(registro);
			emailService.enviarRecuperacion(usuario.getEmail(), token);
		});
	}

	public void restablecerPassword(RestablecerPasswordDTO dto) {
		TokenRecuperacion registro = tokensRecuperacion.findByToken(dto.token())
				.orElseThrow(() -> new NoSuchElementException("El enlace de recuperación no es válido"));
		if (registro.isUsado()) {
			throw new IllegalStateException("Este enlace de recuperación ya fue usado");
		}
		if (registro.getExpiracion().isBefore(LocalDateTime.now())) {
			throw new IllegalStateException("El enlace de recuperación ya expiró");
		}
		Usuario usuario = registro.getUsuario();
		usuario.setPassword(passwordEncoder.encode(dto.nuevaPassword()));
		usuarios.save(usuario);
		registro.setUsado(true);
		tokensRecuperacion.save(registro);
	}
}
