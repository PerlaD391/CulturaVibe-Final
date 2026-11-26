package mx.culturavibe.proyectofinal.seguridad;

import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

// Configuración de Firebase Auth (login con Google por Firebase).
// La llave de la cuenta de servicio se genera en Firebase Console →
// Configuración del proyecto → Cuentas de servicio → Generar nueva
// clave privada. El JSON va en el VPS y su ruta se pasa con la variable
// FIREBASE_SERVICE_ACCOUNT (nunca va en el repositorio). Si no está
// configurada o el archivo no existe, Firebase no se inicializa y la
// app arranca igual: el login con Firebase responde 503.
@Configuration
public class FirebaseConfig {

	private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

	@Value("${app.firebase.service-account:}")
	private String rutaCuentaServicio;

	@Bean
	public FirebaseApp firebaseApp() {
		if (!FirebaseApp.getApps().isEmpty()) {
			return FirebaseApp.getInstance();
		}
		if (rutaCuentaServicio == null || rutaCuentaServicio.isBlank()) {
			log.warn("FIREBASE_SERVICE_ACCOUNT no está configurada; el login con Firebase no funcionará");
			return null;
		}
		try (FileInputStream servicio = new FileInputStream(rutaCuentaServicio.trim())) {
			FirebaseOptions opciones = FirebaseOptions.builder()
					.setCredentials(GoogleCredentials.fromStream(servicio))
					.build();
			FirebaseApp app = FirebaseApp.initializeApp(opciones);
			log.info("Firebase inicializado (proyecto {})", app.getOptions().getProjectId());
			return app;
		} catch (IOException e) {
			log.warn("No se pudo inicializar Firebase con {}: {}; el login con Firebase no funcionará",
					rutaCuentaServicio, e.getMessage());
			return null;
		}
	}
}
