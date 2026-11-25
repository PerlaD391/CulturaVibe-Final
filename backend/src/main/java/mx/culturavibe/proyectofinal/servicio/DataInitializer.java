package mx.culturavibe.proyectofinal.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import mx.culturavibe.proyectofinal.modelo.Categoria;
import mx.culturavibe.proyectofinal.modelo.Evento;
import mx.culturavibe.proyectofinal.modelo.Reserva;
import mx.culturavibe.proyectofinal.modelo.Rol;
import mx.culturavibe.proyectofinal.modelo.Usuario;
import mx.culturavibe.proyectofinal.repositorio.CategoriaRepository;
import mx.culturavibe.proyectofinal.repositorio.EventoRepository;
import mx.culturavibe.proyectofinal.repositorio.ReservaRepository;
import mx.culturavibe.proyectofinal.repositorio.RolRepository;
import mx.culturavibe.proyectofinal.repositorio.UsuarioRepository;

// Datos de PRUEBA para el Proyecto Final (no son datos reales).
// Idempotente: si ya existen roles, no vuelve a sembrar.
// Contraseñas de prueba (cumplen la política): ver README.md del backend.
//   ADMIN:        admin@culturavibe.mx / Admin123!
//   ORGANIZADOR:  organizador@culturavibe.mx / Organizador123!
//   CLIENTE:      cliente@culturavibe.mx / Cliente123!
@Configuration
public class DataInitializer {

	@Bean
	public ApplicationRunner sembrarDatos(RolRepository roles, UsuarioRepository usuarios,
			CategoriaRepository categorias, EventoRepository eventos, ReservaRepository reservas,
			PasswordEncoder passwordEncoder) {
		return args -> {
			if (roles.count() > 0) {
				return;
			}
			// --- Roles (3) ---
			Rol admin = roles.save(new Rol("ADMIN", "Administra usuarios, categorías y eventos."));
			Rol organizador = roles.save(new Rol("ORGANIZADOR", "Crea y gestiona sus propios eventos."));
			Rol cliente = roles.save(new Rol("CLIENTE", "Reserva boletos para eventos."));

			// --- Usuarios (12) ---
			Usuario uAdmin = usuarios.save(new Usuario("Admin CulturaVibe", "admin@culturavibe.mx",
					passwordEncoder.encode("Admin123!"), "+52 951 100 0001", admin));
			Usuario uRicardo = usuarios.save(new Usuario("Ricardo Lemus Sánchez", "organizador@culturavibe.mx",
					passwordEncoder.encode("Organizador123!"), "+52 951 100 0002", organizador));
			Usuario uPerla = usuarios.save(new Usuario("Perla Danae Gallardo Vasquez", "cliente@culturavibe.mx",
					passwordEncoder.encode("Cliente123!"), "+52 951 100 0003", cliente));
			Usuario uMaria = usuarios.save(new Usuario("María Fernanda López", "maria@culturavibe.mx",
					passwordEncoder.encode("Organizador123!"), "+52 951 100 0004", organizador));
			Usuario uCarlos = usuarios.save(new Usuario("Carlos Hernández Ruiz", "carlos@culturavibe.mx",
					passwordEncoder.encode("Organizador123!"), "+52 951 100 0005", organizador));
			Usuario uAna = crearCliente(usuarios, passwordEncoder, "Ana Paola Mendoza", "ana@culturavibe.mx", "0006");
			Usuario uLuis = crearCliente(usuarios, passwordEncoder, "Luis Alberto Torres", "luis@culturavibe.mx", "0007");
			Usuario uSofia = crearCliente(usuarios, passwordEncoder, "Sofía Ramírez Cruz", "sofia@culturavibe.mx", "0008");
			Usuario uDiego = crearCliente(usuarios, passwordEncoder, "Diego Morales Vega", "diego@culturavibe.mx", "0009");
			Usuario uValeria = crearCliente(usuarios, passwordEncoder, "Valeria Ortiz Sánchez", "valeria@culturavibe.mx", "0010");
			Usuario uJorge = crearCliente(usuarios, passwordEncoder, "Jorge Castillo León", "jorge@culturavibe.mx", "0011");
			Usuario uCamila = crearCliente(usuarios, passwordEncoder, "Camila Duarte Reyes", "camila@culturavibe.mx", "0012");
			// Las cuentas semilla quedan con el correo verificado (el
			// registro normal empieza sin verificar hasta meter el código).
			for (Usuario semilla : usuarios.findAll()) {
				semilla.setEmailVerificado(true);
				usuarios.save(semilla);
			}
			// uAdmin se guarda para evitar el aviso de variable sin uso.
			if (uAdmin.getId() == null) {
				throw new IllegalStateException("No se pudo crear el usuario administrador");
			}

			// --- Categorías (5) ---
			Categoria musica = categorias.save(new Categoria("Música", "Conciertos y festivales de música en vivo."));
			Categoria teatro = categorias.save(new Categoria("Teatro", "Obras y puestas en escena."));
			Categoria danza = categorias.save(new Categoria("Danza", "Espectáculos de danza clásica y folclórica."));
			Categoria arte = categorias.save(new Categoria("Arte", "Exposiciones de arte y cultura visual."));
			Categoria festivales = categorias.save(new Categoria("Festivales", "Festivales culturales y ferias."));

			// --- Eventos (12): los 6 de la Act3 adaptados + 6 nuevos de diciembre 2026 ---
			List<Evento> lista = List.of(
					evento("Concierto Sinfónico de Gala",
							"Gala sinfónica con obras de compositores mexicanos e invitados internacionales.",
							LocalDate.of(2026, 11, 12), LocalTime.of(20, 0),
							"Av. Juárez s/n, Centro Histórico", "Ciudad de México", "CDMX",
							musica, uRicardo, new BigDecimal("450.00"),
							"https://www.ticketmaster.com.mx", "Orquesta Sinfónica Nacional"),
					evento("Festival Internacional de Cine",
							"Proyecciones de cine nacional e internacional con funciones al aire libre.",
							LocalDate.of(2026, 11, 22), LocalTime.of(18, 0),
							"Av. Periférico Norte 1695", "Guadalajara", "JAL",
							festivales, uMaria, new BigDecimal("180.00"),
							"https://www.ticketmaster.com.mx", "Cineastas nacionales e internacionales"),
					evento("Noche de Jazz en el Barrio Antiguo",
							"Una noche de jazz en vivo con músicos locales y artistas invitados.",
							LocalDate.of(2026, 11, 25), LocalTime.of(21, 0),
							"Calle Morelos 300, Barrio Antiguo", "Monterrey", "NL",
							musica, uCarlos, new BigDecimal("250.00"),
							"https://www.ticketmaster.com.mx", "Jazz Quartet MX"),
					evento("Exposición de Arte Contemporáneo",
							"Muestra de arte contemporáneo de creadores mexicanos.",
							LocalDate.of(2026, 11, 28), LocalTime.of(11, 0),
							"Av. 2 Oriente 607, Centro Histórico", "Puebla", "PUE",
							arte, uMaria, new BigDecimal("0.00"),
							"https://www.ticketmaster.com.mx", "Artistas contemporáneos mexicanos"),
					evento("Feria del Libro",
							"Venta y presentación de libros con autores invitados y talleres.",
							LocalDate.of(2026, 12, 2), LocalTime.of(10, 0),
							"Av. Morelos Sur 100, Centro", "Cuernavaca", "MOR",
							festivales, uCarlos, new BigDecimal("0.00"),
							"https://www.ticketmaster.com.mx", "Autores invitados"),
					evento("Festival de Teatro Clásico",
							"Puesta en escena de clásicos del teatro universal.",
							LocalDate.of(2026, 12, 5), LocalTime.of(19, 0),
							"Av. Costera Miguel Alemán 100", "Acapulco", "GRO",
							teatro, uRicardo, new BigDecimal("320.00"),
							"https://www.ticketmaster.com.mx", "Compañía Nacional de Teatro"),
					evento("Concierto de Rock en Español",
							"Las bandas más representativas del rock en español en una sola noche.",
							LocalDate.of(2026, 12, 8), LocalTime.of(20, 30),
							"Av. Paseo de la Reforma 50", "Ciudad de México", "CDMX",
							musica, uRicardo, new BigDecimal("850.00"),
							"https://www.ticketmaster.com.mx", "Caifanes, Café Tacvba"),
					evento("Obra: Don Juan Tenorio",
							"La clásica obra de José Zorrilla en el teatro más emblemático de Oaxaca.",
							LocalDate.of(2026, 12, 10), LocalTime.of(19, 0),
							"Av. Independencia 900, Centro", "Oaxaca de Juárez", "OAX",
							teatro, uMaria, new BigDecimal("280.00"),
							"https://www.ticketmaster.com.mx", "Compañía Oaxaqueña de Teatro"),
					evento("Festival de Danza Folclórica",
							"Bailes tradicionales de todas las regiones de México en un solo escenario.",
							LocalDate.of(2026, 12, 12), LocalTime.of(18, 0),
							"Donceles 36, Centro Histórico", "Ciudad de México", "CDMX",
							danza, uCarlos, new BigDecimal("200.00"),
							"https://www.ticketmaster.com.mx", "Ballet Folclórico de México"),
					evento("Noche de Mariachi en Garibaldi",
							"Mariachi en vivo en la plaza más famosa de México, con serenata incluida.",
							LocalDate.of(2026, 12, 14), LocalTime.of(21, 0),
							"Plaza Garibaldi s/n, Centro", "Ciudad de México", "CDMX",
							musica, uRicardo, new BigDecimal("150.00"),
							"https://www.ticketmaster.com.mx", "Mariachi Vargas de Tecalitlán"),
					evento("Exposición: Frida Kahlo y el arte mexicano",
							"Recorrido por la obra de Frida Kahlo y sus contemporáneos en la Casa Azul.",
							LocalDate.of(2026, 12, 16), LocalTime.of(10, 0),
							"Londres 247, Coyoacán", "Ciudad de México", "CDMX",
							arte, uMaria, new BigDecimal("120.00"),
							"https://www.ticketmaster.com.mx", "Frida Kahlo"),
					evento("Festival Cultural de Invierno Oaxaca",
							"Festival de cierre de año con música, gastronomía y artesanías oaxaqueñas.",
							LocalDate.of(2026, 12, 18), LocalTime.of(17, 0),
							"Cerro del Fortín s/n", "Oaxaca de Juárez", "OAX",
							festivales, uCarlos, new BigDecimal("0.00"),
							"https://www.ticketmaster.com.mx", "Artistas oaxaqueños")
			);
			List<Evento> guardados = eventos.saveAll(lista);

			// --- Reservas (12), algunas CANCELADA ---
			List<Reserva> reservasSeed = List.of(
					reserva(uPerla, guardados.get(0), 2, "CONFIRMADA"),
					reserva(uPerla, guardados.get(2), 1, "CONFIRMADA"),
					reserva(uAna, guardados.get(1), 4, "CONFIRMADA"),
					reserva(uLuis, guardados.get(0), 2, "CANCELADA"),
					reserva(uSofia, guardados.get(5), 3, "CONFIRMADA"),
					reserva(uDiego, guardados.get(6), 2, "CONFIRMADA"),
					reserva(uValeria, guardados.get(7), 1, "CONFIRMADA"),
					reserva(uJorge, guardados.get(3), 2, "CONFIRMADA"),
					reserva(uCamila, guardados.get(8), 2, "CANCELADA"),
					reserva(uAna, guardados.get(9), 3, "CONFIRMADA"),
					reserva(uLuis, guardados.get(11), 4, "CONFIRMADA"),
					reserva(uPerla, guardados.get(10), 1, "CONFIRMADA")
			);
			reservas.saveAll(reservasSeed);
		};
	}

	private Usuario crearCliente(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
			String nombre, String email, String sufijoTelefono) {
		// El rol CLIENTE ya existe cuando se crean los usuarios (se siembra primero).
		Rol cliente = usuarios.findByEmailIgnoreCase("cliente@culturavibe.mx")
				.map(Usuario::getRol)
				.orElseThrow(() -> new IllegalStateException("Falta el usuario cliente de prueba"));
		return usuarios.save(new Usuario(nombre, email,
				passwordEncoder.encode("Cliente123!"), "+52 951 100 " + sufijoTelefono, cliente));
	}

	private Evento evento(String titulo, String descripcion, LocalDate fecha, LocalTime hora,
			String direccion, String ciudad, String estado, Categoria categoria, Usuario organizador,
			BigDecimal precio, String url, String artistas) {
		Evento evento = new Evento();
		evento.setTitulo(titulo);
		evento.setDescripcion(descripcion);
		evento.setFecha(fecha);
		evento.setHora(hora);
		evento.setDireccion(direccion);
		evento.setCiudad(ciudad);
		evento.setEstado(estado);
		evento.setCategoria(categoria);
		evento.setOrganizador(organizador);
		evento.setPrecio(precio);
		evento.setEstadoVenta("onsale");
		evento.setUrl(url);
		evento.setArtistas(artistas);
		return evento;
	}

	private Reserva reserva(Usuario usuario, Evento evento, int cantidad, String estado) {
		Reserva reserva = new Reserva(usuario, evento, cantidad);
		reserva.setEstado(estado);
		return reserva;
	}
}
