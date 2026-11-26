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

// Clientes OAuth2 (Google y Facebook), con las credenciales por
// variables de entorno (GOOGLE_CLIENT_ID/SECRET creadas en Google Cloud
// Console; FACEBOOK_CLIENT_ID/SECRET creadas en developers.facebook.com,
// producto Facebook Login). Cada proveedor que no esté configurado
// simplemente no se registra y la app arranca igual: su botón no
// funciona hasta que se configuren. No se usa el auto-configurado de
// Spring Boot con valores vacíos porque un client-id vacío rompería
// el arranque.
@Configuration
public class GoogleOAuth2Config {

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository(
			@Value("${GOOGLE_CLIENT_ID:}") String googleClientId,
			@Value("${GOOGLE_CLIENT_SECRET:}") String googleClientSecret,
			@Value("${FACEBOOK_CLIENT_ID:}") String facebookClientId,
			@Value("${FACEBOOK_CLIENT_SECRET:}") String facebookClientSecret) {
		List<ClientRegistration> registros = new java.util.ArrayList<>();
		if (googleClientId != null && !googleClientId.isBlank()
				&& googleClientSecret != null && !googleClientSecret.isBlank()) {
			registros.add(ClientRegistration.withRegistrationId("google")
					.clientId(googleClientId)
					.clientSecret(googleClientSecret)
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
					.build());
		}
		if (facebookClientId != null && !facebookClientId.isBlank()
				&& facebookClientSecret != null && !facebookClientSecret.isBlank()) {
			registros.add(ClientRegistration.withRegistrationId("facebook")
					.clientId(facebookClientId)
					.clientSecret(facebookClientSecret)
					.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
					.scope("email", "public_profile")
					.authorizationUri("https://www.facebook.com/v18.0/dialog/oauth")
					.tokenUri("https://graph.facebook.com/v18.0/oauth/access_token")
					.userInfoUri("https://graph.facebook.com/v18.0/me?fields=id,name,email")
					.userNameAttributeName("id")
					.clientName("Facebook")
					.build());
		}
		return new InMemoryClientRegistrationRepository(registros);
	}
}
