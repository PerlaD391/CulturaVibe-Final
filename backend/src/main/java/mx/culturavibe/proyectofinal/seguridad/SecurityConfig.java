package mx.culturavibe.proyectofinal.seguridad;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthFilter jwtAuthFilter;

	private final ObjectMapper objectMapper;

	private final GoogleLoginSuccessHandler googleLoginSuccessHandler;

	// Orígenes permitidos, separados por comas (variable CORS_ALLOWED_ORIGINS).
	@Value("${app.cors.allowed-origins:http://localhost:5173,http://13.140.226.55,https://13.140.226.55}")
	private String origenesPermitidos;

	// URL del frontend, a donde vuelve el login con Google (con el token).
	@Value("${app.frontend.url:http://localhost:5173}")
	private String frontendUrl;

	public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper,
			GoogleLoginSuccessHandler googleLoginSuccessHandler) {
		this.jwtAuthFilter = jwtAuthFilter;
		this.objectMapper = objectMapper;
		this.googleLoginSuccessHandler = googleLoginSuccessHandler;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/eventos", "/api/eventos/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/categorias", "/api/categorias/**").permitAll()
						.anyRequest().authenticated())
				// Login con Google: al volver de Google se genera el JWT
				// propio y se redirige al frontend con ?token=. Si Google
				// no está configurado, /oauth2/authorization/google no
				// existe y el botón simplemente no funciona.
				.oauth2Login(oauth -> oauth
						.successHandler(googleLoginSuccessHandler)
						.failureHandler((peticion, respuesta, ex) -> respuesta
								.sendRedirect(frontendUrl + "/login?error=google")))
				.exceptionHandling(e -> e
						.authenticationEntryPoint(authenticationEntryPoint())
						.accessDeniedHandler(accessDeniedHandler()))
				.addFilterBefore(jwtAuthFilter,
						org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	// 401 con cuerpo JSON, igual que el resto de errores de la API
	// (antes el entry point por defecto devolvía un 401 vacío).
	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint() {
		return (request, response, ex) -> {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write(objectMapper.writeValueAsString(java.util.Map.of(
					"timestamp", java.time.LocalDateTime.now().toString(),
					"status", 401,
					"error", "Unauthorized",
					"message", "No has iniciado sesión o tu sesión expiró",
					"path", request.getRequestURI())));
		};
	}

	// 403 con cuerpo JSON, igual que el resto de errores de la API.
	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
		return (request, response, ex) -> {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("application/json");
			response.getWriter().write(objectMapper.writeValueAsString(java.util.Map.of(
					"timestamp", java.time.LocalDateTime.now().toString(),
					"status", 403,
					"error", "Forbidden",
					"message", "No tienes permiso para realizar esta acción",
					"path", request.getRequestURI())));
		};
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		List<String> origenes = Arrays.stream(origenesPermitidos.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
		config.setAllowedOrigins(origenes);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", config);
		return fuente;
	}
}
