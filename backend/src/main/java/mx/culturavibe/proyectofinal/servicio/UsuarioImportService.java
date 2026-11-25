package mx.culturavibe.proyectofinal.servicio;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import mx.culturavibe.proyectofinal.modelo.Rol;
import mx.culturavibe.proyectofinal.modelo.Usuario;
import mx.culturavibe.proyectofinal.repositorio.RolRepository;
import mx.culturavibe.proyectofinal.repositorio.UsuarioRepository;
import mx.culturavibe.proyectofinal.servicio.DummyJsonClient.UsuarioDummy;

// Importa usuarios de DummyJSON a la tabla usuarios, para poblar la
// base de usuarios del Proyecto Final (la misma fuente que usaba la
// app de la Actividad 8 para el login). Todos entran como CLIENTE con
// la contraseña de DummyJSON hasheada con BCrypt, tal cual viene.
// La política de contraseña solo se valida en RegistroDTO (registro
// público); la entidad Usuario no valida el formato de la contraseña,
// así que las contraseñas de DummyJSON se guardan sin problema.
// Upsert por email (insensible a mayúsculas): si ya existe se salta,
// nunca se duplica. Cada guardado es su propia transacción: si un
// usuario falla, se registra un warn y se sigue con los demás.
@Service
public class UsuarioImportService {

	private static final Logger log = LoggerFactory.getLogger(UsuarioImportService.class);

	private final DummyJsonClient cliente;

	private final UsuarioRepository usuarios;

	private final RolRepository roles;

	private final PasswordEncoder passwordEncoder;

	public UsuarioImportService(DummyJsonClient cliente, UsuarioRepository usuarios,
			RolRepository roles, PasswordEncoder passwordEncoder) {
		this.cliente = cliente;
		this.usuarios = usuarios;
		this.roles = roles;
		this.passwordEncoder = passwordEncoder;
	}

	public record ResultadoImportacion(int importados, int yaExistian, int total) {
	}

	public ResultadoImportacion importar(int limit) {
		List<UsuarioDummy> dummies = cliente.buscarUsuarios(limit);
		Rol clienteRol = roles.findByNombreIgnoreCase("CLIENTE")
				.orElseThrow(() -> new IllegalStateException(
						"No existe el rol CLIENTE; revisa el seed de la base de datos"));
		int importados = 0;
		int yaExistian = 0;
		int errores = 0;
		for (UsuarioDummy dummy : dummies) {
			if (usuarios.existsByEmailIgnoreCase(dummy.email())) {
				yaExistian++;
				continue;
			}
			try {
				// Misma construcción que DataInitializer.crearCliente:
				// nombre, correo, contraseña hasheada, teléfono null, rol CLIENTE.
				Usuario usuario = new Usuario(dummy.nombre(), dummy.email(),
						passwordEncoder.encode(dummy.password()), null, clienteRol);
				usuarios.save(usuario);
				importados++;
			} catch (DataIntegrityViolationException e) {
				// Otro proceso lo insertó entre la consulta y el guardado
				// (el índice único de email lo impidió duplicar).
				yaExistian++;
			} catch (Exception e) {
				errores++;
				log.warn("No se pudo importar el usuario de DummyJSON {}: {}", dummy.email(), e.getMessage());
			}
		}
		log.info("Importación DummyJSON: {} nuevos, {} ya existían, {} errores, {} en total",
				importados, yaExistian, errores, dummies.size());
		return new ResultadoImportacion(importados, yaExistian, dummies.size());
	}
}
