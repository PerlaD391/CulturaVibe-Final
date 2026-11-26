package mx.culturavibe.proyectofinal.seguridad;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.culturavibe.proyectofinal.dto.AuthRespuesta;
import mx.culturavibe.proyectofinal.servicio.AuthService;

// Tras iniciar sesión con un proveedor OAuth2 (Google o Facebook):
// busca o crea la cuenta en CulturaVibe, genera el JWT propio de la
// app y redirige al frontend con el token en la URL
// (/login?token=...). Sirve para cualquier proveedor que devuelva
// correo y nombre. La contraseña del proveedor nunca se pide ni se
// guarda. Si el proveedor no devolvió correo, se vuelve al login con
// ?error=<proveedor> (google o facebook).
@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

	private final AuthService authService;

	@Value("${app.frontend.url:http://localhost:5173}")
	private String frontendUrl;

	public GoogleLoginSuccessHandler(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		String proveedor = "google";
		if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken token) {
			proveedor = token.getAuthorizedClientRegistrationId();
		}
		String email = null;
		String nombre = null;
		Object principal = authentication.getPrincipal();
		if (principal instanceof OidcUser oidc) {
			email = oidc.getEmail();
			nombre = oidc.getFullName();
		} else if (principal instanceof OAuth2User oauth2) {
			email = oauth2.getAttribute("email");
			nombre = oauth2.getAttribute("name");
		}
		if (email == null || email.isBlank()) {
			response.sendRedirect(frontendUrl + "/login?error=" + proveedor);
			return;
		}
		AuthRespuesta auth = authService.loginConGoogle(email, nombre);
		// El JWT es Base64URL, seguro en la URL tal cual.
		response.sendRedirect(frontendUrl + "/login?token=" + auth.token());
	}
}
