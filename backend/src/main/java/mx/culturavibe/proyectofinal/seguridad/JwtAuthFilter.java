package mx.culturavibe.proyectofinal.seguridad;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.culturavibe.proyectofinal.repositorio.TokenRevocadoRepository;
import mx.culturavibe.proyectofinal.servicio.JwtService;

// Lee el Bearer, valida firma y expiración, y verifica que el token
// NO esté en tokens_revocados (cerró sesión). Sin token válido, la
// petición sigue sin autenticar y SecurityConfig responde 401 o 403.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	private final UserDetailsService userDetailsService;

	private final TokenRevocadoRepository tokensRevocados;

	public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService,
			TokenRevocadoRepository tokensRevocados) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.tokensRevocados = tokensRevocados;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String encabezado = request.getHeader("Authorization");
		if (encabezado == null || !encabezado.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		String token = encabezado.substring(7);
		try {
			if (!tokensRevocados.existsByToken(token)) {
				String email = jwtService.extraerEmail(token);
				if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
					UserDetails usuario = userDetailsService.loadUserByUsername(email);
					if (jwtService.esValido(token, usuario.getUsername())) {
						UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
								usuario, null, usuario.getAuthorities());
						auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(auth);
					}
				}
			}
		} catch (Exception e) {
			SecurityContextHolder.clearContext();
		}
		filterChain.doFilter(request, response);
	}
}
