package mx.culturavibe.proyectofinal.seguridad;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

// Cliente OAuth2 de Google, con las credenciales por variables de
// entorno (GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET, creadas en
// Google Cloud Console). Si no están configuradas, el repositorio
// queda vacío y la app arranca igual: el botón "Continuar con
// Google" simplemente no funciona hasta que se configuren. No se
// usa el auto-configurado de Spring Boot con valores vacíos porque
// un client-id vacío rompería el arranque.
@Configuration
public class GoogleOAuth2Config {

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository(
			@Value("${GOOGLE_CLIENT_ID:}") String clientId,
			@Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret) {
		if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
			return new InMemoryClientRegistrationRepository(List.of());
		}
		ClientRegistration google = ClientRegistration.withRegistrationId("google")
				.clientId(clientId)
				.clientSecret(clientSecret)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
				.scope("openid", "profile", "email")
				.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
				.tokenUri("https://oauth2.googleapis.com/token")
				.userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
				.userNameAttributeName("sub")
				.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
				.clientName("Google")
				.build();
		return new InMemoryClientRegistrationRepository(google);
	}
}
