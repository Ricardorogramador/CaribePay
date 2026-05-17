package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ricardo.estudio.caribepay.dtos.RegistroDTO;
import ricardo.estudio.caribepay.dtos.UsuarioResponseDTO;
import ricardo.estudio.caribepay.models.Role;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneService phoneService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, PhoneService phoneService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.phoneService = phoneService;
    }

    public Usuario registrarUsuario(RegistroDTO registroDTO) {
        log.info("Iniciando registro de usuario: {}", registroDTO.getEmail());

        String email = registroDTO.getEmail().trim().toLowerCase();
        String telefonoNormalizado = phoneService.normalizarCO(registroDTO.getTelefono());

        if (usuarioRepository.findByEmail(email).isPresent()) {
            log.warn("Intento de registro con email duplicado: {}", email);
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (usuarioRepository.findByTelefono(telefonoNormalizado).isPresent()) {
            log.warn("Intento de registro con teléfono duplicado: {}", telefonoNormalizado);
            throw new IllegalArgumentException("El teléfono ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setTelefono(telefonoNormalizado);
        usuario.setPassword(passwordEncoder.encode(registroDTO.getPassword()));
        usuario.setSaldo(0.0);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setRole(Role.USUARIO); // Por defecto es usuario
        usuario.setActivo(true); // Por defecto activo

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario registrado exitosamente: {} (ID: {}, Role: {})", email, guardado.getId(), guardado.getRole());

        return guardado;
    }

    public Optional<Usuario> obtenerUsuarioPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmail(email.trim().toLowerCase());
    }

    public Optional<Usuario> obtenerUsuarioPorId(String id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> obtenerUsuarioPorTelefonoNormalizado(String telefonoNormalizado) {
        return usuarioRepository.findByTelefono(telefonoNormalizado);
    }

    public UsuarioResponseDTO convertirAResponse(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getSaldo(),
                usuario.getFechaCreacion()
        );
    }

    public void actualizarSaldo(String usuarioId, Double nuevoSaldo) {
        log.debug("Actualizando saldo manual para usuario: {}, nuevo saldo: ${}", usuarioId, nuevoSaldo);
        Optional<Usuario> usuario = usuarioRepository.findById(usuarioId);
        if (usuario.isPresent()) {
            usuario.get().setSaldo(nuevoSaldo);
            usuarioRepository.save(usuario.get());
            log.info("Saldo actualizado para usuario: {}", usuarioId);
        } else {
            log.error("Usuario no encontrado para actualizar saldo: {}", usuarioId);
            throw new IllegalArgumentException("Usuario no encontrado");
        }
    }

    // ===== MÉTODOS PARA ADMIN =====

    public List<Usuario> obtenerTodosLosUsuarios() {
        log.info("Obteniendo lista de todos los usuarios");
        return usuarioRepository.findAll();
    }

    public List<Usuario> obtenerUsuariosActivos() {
        log.info("Obteniendo usuarios activos");
        return usuarioRepository.findByActivo(true);
    }

    public List<Usuario> obtenerUsuariosDesactivados() {
        log.info("Obteniendo usuarios desactivados");
        return usuarioRepository.findByActivo(false);
    }

    public Usuario desactivarUsuario(String usuarioId) {
        log.warn("Desactivando usuario: {}", usuarioId);

        Optional<Usuario> usuario = usuarioRepository.findById(usuarioId);
        if (usuario.isEmpty()) {
            log.error("Usuario no encontrado para desactivar: {}", usuarioId);
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        Usuario u = usuario.get();
        u.setActivo(false);
        u.setFechaDesactivacion(LocalDateTime.now());
        usuarioRepository.save(u);

        log.warn("Usuario desactivado: {} ({})", u.getEmail(), usuarioId);
        return u;
    }

    public Usuario activarUsuario(String usuarioId) {
        log.info("Activando usuario: {}", usuarioId);

        Optional<Usuario> usuario = usuarioRepository.findById(usuarioId);
        if (usuario.isEmpty()) {
            log.error("Usuario no encontrado para activar: {}", usuarioId);
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        Usuario u = usuario.get();
        u.setActivo(true);
        u.setFechaDesactivacion(null);
        usuarioRepository.save(u);

        log.info("Usuario activado: {} ({})", u.getEmail(), usuarioId);
        return u;
    }
}