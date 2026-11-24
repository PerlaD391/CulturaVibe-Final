package mx.culturavibe.proyectofinal.servicio;

import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.culturavibe.proyectofinal.dto.UsuarioDTO;
import mx.culturavibe.proyectofinal.modelo.Rol;
import mx.culturavibe.proyectofinal.modelo.Usuario;
import mx.culturavibe.proyectofinal.repositorio.RolRepository;
import mx.culturavibe.proyectofinal.repositorio.UsuarioRepository;

// Gestión de usuarios, solo para ADMIN.
@Service
@Transactional
public class UsuarioService {

	private final UsuarioRepository usuarios;

	private final RolRepository roles;

	public UsuarioService(UsuarioRepository usuarios, RolRepository roles) {
		this.usuarios = usuarios;
		this.roles = roles;
	}

	@Transactional(readOnly = true)
	public Page<UsuarioDTO> listar(Pageable pageable) {
		return usuarios.findAll(pageable).map(UsuarioDTO::desde);
	}

	@Transactional(readOnly = true)
	public UsuarioDTO buscarPorId(Long id) {
		return UsuarioDTO.desde(buscarEntidad(id));
	}

	@Transactional(readOnly = true)
	public Usuario buscarEntidad(Long id) {
		return usuarios.findById(id)
				.orElseThrow(() -> new NoSuchElementException("No existe un usuario con id " + id));
	}

	// Actualiza nombre, teléfono, rol y activo. El correo no se cambia
	// aquí porque es el identificador de inicio de sesión.
	public UsuarioDTO actualizar(Long id, UsuarioDTO dto) {
		Usuario usuario = buscarEntidad(id);
		usuario.setNombre(dto.nombre());
		usuario.setTelefono(dto.telefono());
		if (dto.activo() != null) {
			usuario.setActivo(dto.activo());
		}
		if (dto.rol() != null && !dto.rol().isBlank()) {
			Rol rol = roles.findByNombreIgnoreCase(dto.rol())
					.orElseThrow(() -> new NoSuchElementException("No existe el rol " + dto.rol()));
			usuario.setRol(rol);
		}
		return UsuarioDTO.desde(usuarios.save(usuario));
	}

	public void eliminar(Long id) {
		usuarios.delete(buscarEntidad(id));
	}
}
