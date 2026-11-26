package mx.culturavibe.proyectofinal.servicio;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.culturavibe.proyectofinal.dto.AuthRespuesta;
import mx.culturavibe.proyectofinal.dto.LoginDTO;
import mx.culturavibe.proyectofinal.dto.OlvidoPasswordDTO;
import mx.culturavibe.proyectofinal.dto.ReenviarCodigoDTO;
import mx.culturavibe.proyectofinal.dto.RegistroDTO;
import mx.culturavibe.proyectofinal.dto.RestablecerPasswordDTO;
import mx.culturavibe.proyectofinal.dto.UsuarioDTO;
import mx.culturavibe.proyectofinal.dto.VerificarCodigoDTO;
import mx.culturavibe.proyectofinal.modelo.CodigoVerificacion;
import mx.culturavibe.proyectofinal.modelo.Rol;
import mx.culturavibe.proyectofinal.modelo.TokenRecuperacion;
import mx.culturavibe.proyectofinal.modelo.TokenRevocado;
import mx.culturavibe.proyectofinal.modelo.Usuario;
import mx.culturavibe.proyectofinal.repositorio.CodigoVerificacionRepository;
import mx.culturavibe.proyectofinal.repositorio.RolRepository;
import mx.culturavibe.proyectofinal.repositorio.TokenRecuperacionRepository;
import mx.culturavibe.proyectofinal.repositorio.TokenRevocadoRepository;
import mx.culturavibe.proyectofinal.repositorio.UsuarioRepository;

@Service
@Transactional
public class AuthService implements UserDetailsService {

	// Intentos fallidos seguidos antes de suspender la cuenta.
	public static final int MAX_INTENTOS_FALLIDOS = 5;

	// Validez del código de verificación de 6 dígitos (minutos).
	public static final int MINUTOS_CODIGO_VERIFICACION = 15;

	private static final SecureRandom ALEATORIO = new SecureRandom();

	private final UsuarioRepository usuarios;

	private final RolRepository roles;

	private final TokenRevocadoRepository tokensRevocados;

	private final TokenRecuperacionRepository tokensRecuperacion;

	private final CodigoVerificacionRepository codigosVerificacion;

	private final PasswordEncoder passwordEncoder;

	private final ObjectProvider<AuthenticationManager> authenticationManager;

	private final JwtService jwtService;

	private final EmailService emailService;

	private final NotificacionService notificaciones;

	// Firebase Auth (login con Google por Firebase). ObjectProvider
	// porque FirebaseApp puede ser null si no está configurado.
	private final ObjectProvider<FirebaseApp> firebaseApp;

	public AuthService(UsuarioRepository usuarios, RolRepository roles,
			TokenRevocadoRepository tokensRevocados,
			TokenRecuperacionRepository tokensRecuperacion,
			CodigoVerificacionRepository codigosVerificacion,
			PasswordEncoder passwordEncoder,
			ObjectProvider<AuthenticationManager> authenticationManager,
			JwtService jwtService, EmailService emailService,
			NotificacionService notificaciones, ObjectProvider<FirebaseApp> firebaseApp) {
		this.usuarios = usuarios;
		this.roles = roles;
		this.tokensRevocados = tokensRevocados;
		this.tokensRecuperacion = tokensRecuperacion;
		this.codigosVerificacion = codigosVerificacion;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.emailService = emailService;
		this.notificaciones = notificaciones;
		this.firebaseApp = firebaseApp;
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
		// Código de 6 dígitos por correo y, si dio teléfono, también por
		// SMS. Si el envío falla, solo queda un warn y el registro sigue.
		generarCodigoVerificacion(usuario);
		String token = jwtService.generarToken(usuario.getEmail());
		return AuthRespuesta.conToken(token, usuario.getEmail(), usuario.getNombre(), rol.getNombre());
	}

	// Login con bloqueo tras 5 intentos fallidos seguidos: al 5º la
	// cuenta se suspende (activo=false) y se manda el correo de
	// recuperación. Un login correcto resetea el contador a 0. La
	// cuenta suspendida responde 403 con el mensaje de recuperación.
	public AuthRespuesta login(LoginDTO dto) {
		Usuario usuario = usuarios.findByEmailIgnoreCase(dto.email()).orElse(null);
		if (usuario != null && !usuario.isActivo()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Tu cuenta está suspendida por varios intentos fallidos. Revisa tu correo para recuperarla.");
		}
		try {
			authenticationManager.getObject().authenticate(
					new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));
		} catch (DisabledException e) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"Tu cuenta está suspendida por varios intentos fallidos. Revisa tu correo para recuperarla.");
		} catch (BadCredentialsException e) {
			// El intento fallido lo registra el controlador (ver
			// AuthController.login): si se contara aquí, la transacción
			// de este método haría rollback al propagarse la excepción
			// y el contador nunca subiría.
			throw e;
		}
		if (usuario == null) {
			throw new UsernameNotFoundException("No existe un usuario con el correo " + dto.email());
		}
		if (usuario.getIntentosFallidos() > 0) {
			usuario.setIntentosFallidos(0);
			usuarios.save(usuario);
		}
		String token = jwtService.generarToken(usuario.getEmail());
		return AuthRespuesta.conToken(token, usuario.getEmail(), usuario.getNombre(),
				usuario.getRol().getNombre());
	}

	// Registra un intento fallido de login. Lo llama el controlador
	// DESPUÉS de que login() falló (no desde login(), porque el
	// rollback de su transacción borraría el contador). Corre en su
	// propia transacción y sí se guarda. Devuelve true si con este
	// intento la cuenta quedó suspendida.
	public boolean registrarIntentoFallido(String email) {
		Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);
		if (usuario == null) {
			return false;
		}
		int intentos = usuario.getIntentosFallidos() + 1;
		usuario.setIntentosFallidos(intentos);
		if (intentos >= MAX_INTENTOS_FALLIDOS) {
			usuario.setActivo(false);
			usuarios.save(usuario);
			// El correo de recuperación es la forma de reactivarla.
			generarYEnviarRecuperacion(usuario);
			return true;
		}
		usuarios.save(usuario);
		return false;
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
		usuarios.findByEmailIgnoreCase(dto.email()).ifPresent(this::generarYEnviarRecuperacion);
	}

	// Genera el token de recuperación y manda el correo. Lo usan tanto
	// el olvido de contraseña como la suspensión por intentos fallidos.
	private void generarYEnviarRecuperacion(Usuario usuario) {
		String token = UUID.randomUUID().toString();
		TokenRecuperacion registro = new TokenRecuperacion(usuario, token,
				LocalDateTime.now().plusHours(1));
		tokensRecuperacion.save(registro);
		emailService.enviarRecuperacion(usuario.getEmail(), token);
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
		// Restablecer la contraseña reactiva la cuenta suspendida y
		// resetea el contador de intentos fallidos.
		usuario.setActivo(true);
		usuario.setIntentosFallidos(0);
		usuarios.save(usuario);
		registro.setUsado(true);
		tokensRecuperacion.save(registro);
	}

	// --- Verificación de correo/SMS con código de 6 dígitos ---

	// Genera un código nuevo (invalida los anteriores), lo manda por
	// correo y, si el usuario dio teléfono y el SMS está configurado,
	// también por SMS. Nunca rompe el flujo que lo llama.
	private void generarCodigoVerificacion(Usuario usuario) {
		for (CodigoVerificacion anterior : codigosVerificacion.findByUsuarioAndUsadoFalse(usuario)) {
			anterior.setUsado(true);
			codigosVerificacion.save(anterior);
		}
		String codigo = String.format("%06d", ALEATORIO.nextInt(1_000_000));
		CodigoVerificacion registro = new CodigoVerificacion(usuario, codigo,
				LocalDateTime.now().plusMinutes(MINUTOS_CODIGO_VERIFICACION));
		codigosVerificacion.save(registro);
		emailService.enviarCodigoVerificacion(usuario.getEmail(), usuario.getNombre(), codigo);
		notificaciones.enviarCodigoVerificacion(usuario, codigo);
	}

	public String verificarCodigo(VerificarCodigoDTO dto) {
		Usuario usuario = usuarios.findByEmailIgnoreCase(dto.email())
				.orElseThrow(() -> new IllegalArgumentException(
						"No hay un código de verificación para ese correo"));
		CodigoVerificacion registro = codigosVerificacion
				.findFirstByUsuarioAndUsadoFalseOrderByExpiracionDesc(usuario)
				.orElseThrow(() -> new IllegalArgumentException(
						"No hay un código activo para ese correo. Pide uno nuevo."));
		if (registro.getExpiracion().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("El código ya expiró. Pide uno nuevo.");
		}
		if (!registro.getCodigo().equals(dto.codigo())) {
			throw new IllegalArgumentException("El código no es correcto. Revisa e inténtalo de nuevo.");
		}
		registro.setUsado(true);
		codigosVerificacion.save(registro);
		usuario.setEmailVerificado(true);
		if (usuario.getTelefono() != null && !usuario.getTelefono().isBlank()) {
			usuario.setTelefonoVerificado(true);
		}
		usuarios.save(usuario);
		return usuario.getTelefono() != null && !usuario.getTelefono().isBlank()
				? "Correo y teléfono verificados correctamente"
				: "Correo verificado correctamente";
	}

	public String reenviarCodigo(ReenviarCodigoDTO dto) {
		Usuario usuario = usuarios.findByEmailIgnoreCase(dto.email())
				.orElseThrow(() -> new IllegalArgumentException(
						"No existe una cuenta con ese correo"));
		if (usuario.isEmailVerificado()) {
			throw new IllegalArgumentException("El correo ya está verificado.");
		}
		generarCodigoVerificacion(usuario);
		return usuario.getTelefono() != null && !usuario.getTelefono().isBlank()
				? "Te enviamos un código nuevo por correo y por SMS"
				: "Te enviamos un código nuevo por correo";
	}

	// --- Login con Google ---

	// Busca el usuario por el correo de Google; si no existe, crea la
	// cuenta sola como CLIENTE (nombre del perfil de Google, sin
	// contraseña utilizable, correo ya verificado). Devuelve el JWT
	// propio de la app, igual que el login normal.
	public AuthRespuesta loginConGoogle(String email, String nombre) {
		Usuario usuario = usuarios.findByEmailIgnoreCase(email).orElse(null);
		if (usuario == null) {
			Rol cliente = roles.findByNombreIgnoreCase("CLIENTE")
					.orElseThrow(() -> new IllegalStateException("No existe el rol CLIENTE; revisa el seed de la base de datos"));
			String nombreGoogle = nombre == null || nombre.isBlank() ? email : nombre;
			// Contraseña aleatoria que nadie conoce: la cuenta solo se
			// usa con Google (el login con contraseña no la acepta
			// porque nadie sabe ese valor).
			usuario = new Usuario(nombreGoogle, email,
					passwordEncoder.encode(UUID.randomUUID().toString()), null, cliente);
			usuario.setEmailVerificado(true);
			usuarios.save(usuario);
		}
		String token = jwtService.generarToken(usuario.getEmail());
		return AuthRespuesta.conToken(token, usuario.getEmail(), usuario.getNombre(),
				usuario.getRol().getNombre());
	}

	// Login con Firebase Auth (Google por Firebase): verifica el ID
	// token de Firebase, toma el correo y el nombre del token y entra
	// con la cuenta del sistema (la crea como CLIENTE si no existe,
	// igual que loginConGoogle). Si Firebase no está configurado en
	// el servidor responde 503; si el token es inválido, 401.
	public AuthRespuesta loginConFirebase(String idToken) {
		FirebaseApp app = firebaseApp.getIfAvailable();
		if (app == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"El login con Firebase no está configurado en el servidor");
		}
		FirebaseToken tokenFirebase;
		try {
			tokenFirebase = FirebaseAuth.getInstance(app).verifyIdToken(idToken);
		} catch (FirebaseAuthException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"El token de Firebase no es válido");
		}
		String email = tokenFirebase.getEmail();
		if (email == null || email.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"El token de Firebase no trae correo electrónico");
		}
		return loginConGoogle(email, tokenFirebase.getName());
	}

	// Perfil del usuario del token (para el login con Google, que llega
	// solo con el JWT y necesita nombre y rol para la sesión).
	public UsuarioDTO obtenerPerfil(String email) {
		Usuario usuario = usuarios.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UsernameNotFoundException("No existe un usuario con el correo " + email));
		return UsuarioDTO.desde(usuario);
	}
}
