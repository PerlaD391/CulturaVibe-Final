package mx.culturavibe.proyectofinal.servicio;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

// JWT HS256, 24 horas. El secreto llega por la variable JWT_SECRET
// en Base64URL (32 bytes); JJWT exige ese formato.
@Service
public class JwtService {

	private final SecretKey llave;

	private final long expiracionMs;

	public JwtService(
			@Value("${app.jwt.secreto}") String secreto,
			@Value("${app.jwt.expiracion-ms:86400000}") long expiracionMs) {
		this.llave = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secreto.trim()));
		this.expiracionMs = expiracionMs;
	}

	public String generarToken(String email) {
		Date ahora = new Date();
		return Jwts.builder()
				.subject(email)
				.issuedAt(ahora)
				.expiration(new Date(ahora.getTime() + expiracionMs))
				.signWith(llave)
				.compact();
	}

	public String extraerEmail(String token) {
		return extraerClaims(token).getSubject();
	}

	public LocalDateTime extraerExpiracion(String token) {
		Date expiracion = extraerClaims(token).getExpiration();
		return LocalDateTime.ofInstant(expiracion.toInstant(), ZoneId.systemDefault());
	}

	public boolean esValido(String token, String email) {
		return extraerEmail(token).equals(email)
				&& extraerClaims(token).getExpiration().after(new Date());
	}

	private Claims extraerClaims(String token) {
		return Jwts.parser()
				.verifyWith(llave)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
