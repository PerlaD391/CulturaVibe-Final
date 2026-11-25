package mx.culturavibe.proyectofinal.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Correo con JavaMailSender (Postfix en localhost:25 en el VPS).
// Si el envío falla, solo se registra un warn: el correo nunca debe
// romper el flujo (registro, recuperación, etc.).
@Service
public class EmailService {

	private static final Logger log = LoggerFactory.getLogger(EmailService.class);

	private final JavaMailSender mailSender;

	@Value("${app.mail.from:no-reply@culturavibe.mx}")
	private String remitente;

	@Value("${app.frontend.url:http://localhost:5173}")
	private String frontendUrl;

	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void enviarBienvenida(String email, String nombre) {
		enviar(email,
				"Bienvenido a CulturaVibe",
				"Hola " + nombre + ",\n\n"
						+ "Tu cuenta en CulturaVibe se creó correctamente. "
						+ "Ya puedes iniciar sesión y reservar boletos para eventos culturales en México.\n\n"
						+ "Saludos,\nEl equipo de CulturaVibe");
	}

	public void enviarRecuperacion(String email, String token) {
		String enlace = frontendUrl + "/restablecer-password?token=" + token;
		enviar(email,
				"Restablecer tu contraseña en CulturaVibe",
				"Hola,\n\n"
						+ "Pediste restablecer tu contraseña en CulturaVibe. "
						+ "Abre este enlace (válido por 1 hora):\n" + enlace + "\n\n"
						+ "Si no pediste este cambio, ignora este correo.\n\n"
						+ "Saludos,\nEl equipo de CulturaVibe");
	}

	public void enviarConfirmacionReserva(String email, String nombre, String tituloEvento, int cantidad) {
		enviar(email,
				"Tu reserva en CulturaVibe está confirmada",
				"Hola " + nombre + ",\n\n"
						+ "Tu reserva para '" + tituloEvento + "' ("
						+ cantidad + (cantidad == 1 ? " boleto" : " boletos")
						+ ") está confirmada. ¡Nos vemos ahí!\n\n"
						+ "Saludos,\nEl equipo de CulturaVibe");
	}

	private void enviar(String destinatario, String asunto, String texto) {
		try {
			SimpleMailMessage mensaje = new SimpleMailMessage();
			mensaje.setFrom(remitente);
			mensaje.setTo(destinatario);
			mensaje.setSubject(asunto);
			mensaje.setText(texto);
			mailSender.send(mensaje);
			log.info("Correo enviado a {}: {}", destinatario, asunto);
		} catch (MailException e) {
			log.warn("No se pudo enviar el correo a {} ({}): {}", destinatario, asunto, e.getMessage());
		}
	}
}
