package ricardo.estudio.caribepay.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import ricardo.estudio.caribepay.dtos.UsuarioResponseDTO;
import ricardo.estudio.caribepay.models.Role;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.services.UsuarioService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class AdminController {

    private final UsuarioService usuarioService;

    public AdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Validar que el usuario sea admin
     */
    private void validarAdmin(Authentication authentication) {
        if (authentication == null) {
            log.warn("Intento de acceso admin sin autenticación");
            throw new IllegalArgumentException("No autenticado");
        }

        String email = authentication.getName();
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

        if (usuario.isEmpty() || usuario.get().getRole() != Role.ADMIN) {
            log.warn("Intento de acceso admin por usuario no autorizado: {}", email);
            throw new IllegalArgumentException("No tienes permisos de administrador");
        }
    }

    /**
     * Obtener todos los usuarios (activos e inactivos)
     */
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerTodosUsuarios(Authentication authentication) {
        validarAdmin(authentication);

        log.info("Admin solicitando lista de todos los usuarios");
        List<Usuario> usuarios = usuarioService.obtenerTodosLosUsuarios();

        List<UsuarioResponseDTO> respuesta = usuarios.stream()
                .map(u -> new UsuarioResponseDTO(
                        u.getId(),
                        u.getEmail(),
                        u.getTelefono(),
                        u.getSaldo(),
                        u.getFechaCreacion(),
                        u.getRole().getValue(),
                        u.getActivo(),
                        u.getFechaDesactivacion()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Obtener usuarios activos
     */
    @GetMapping("/usuarios/activos")
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerUsuariosActivos(Authentication authentication) {
        validarAdmin(authentication);

        log.info("Admin solicitando lista de usuarios activos");
        List<Usuario> usuarios = usuarioService.obtenerUsuariosActivos();

        List<UsuarioResponseDTO> respuesta = usuarios.stream()
                .map(u -> new UsuarioResponseDTO(
                        u.getId(),
                        u.getEmail(),
                        u.getTelefono(),
                        u.getSaldo(),
                        u.getFechaCreacion(),
                        u.getRole().getValue(),
                        u.getActivo(),
                        u.getFechaDesactivacion()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Obtener usuarios desactivados
     */
    @GetMapping("/usuarios/desactivados")
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerUsuariosDesactivados(Authentication authentication) {
        validarAdmin(authentication);

        log.info("Admin solicitando lista de usuarios desactivados");
        List<Usuario> usuarios = usuarioService.obtenerUsuariosDesactivados();

        List<UsuarioResponseDTO> respuesta = usuarios.stream()
                .map(u -> new UsuarioResponseDTO(
                        u.getId(),
                        u.getEmail(),
                        u.getTelefono(),
                        u.getSaldo(),
                        u.getFechaCreacion(),
                        u.getRole().getValue(),
                        u.getActivo(),
                        u.getFechaDesactivacion()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Desactivar un usuario
     */
    @PostMapping("/usuarios/{usuarioId}/desactivar")
    public ResponseEntity<UsuarioResponseDTO> desactivarUsuario(
            @PathVariable String usuarioId,
            Authentication authentication
    ) {
        validarAdmin(authentication);

        log.warn("Admin desactivando usuario: {}", usuarioId);
        Usuario usuario = usuarioService.desactivarUsuario(usuarioId);

        UsuarioResponseDTO respuesta = new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getSaldo(),
                usuario.getFechaCreacion(),
                usuario.getRole().getValue(),
                usuario.getActivo(),
                usuario.getFechaDesactivacion()
        );

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Activar un usuario
     */
    @PostMapping("/usuarios/{usuarioId}/activar")
    public ResponseEntity<UsuarioResponseDTO> activarUsuario(
            @PathVariable String usuarioId,
            Authentication authentication
    ) {
        validarAdmin(authentication);

        log.info("Admin activando usuario: {}", usuarioId);
        Usuario usuario = usuarioService.activarUsuario(usuarioId);

        UsuarioResponseDTO respuesta = new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getSaldo(),
                usuario.getFechaCreacion(),
                usuario.getRole().getValue(),
                usuario.getActivo(),
                usuario.getFechaDesactivacion()
        );

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Obtener estadísticas del sistema
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<Object> obtenerEstadisticas(Authentication authentication) {
        validarAdmin(authentication);

        log.info("Admin solicitando estadísticas del sistema");

        List<Usuario> todosUsuarios = usuarioService.obtenerTodosLosUsuarios();
        List<Usuario> activos = usuarioService.obtenerUsuariosActivos();
        List<Usuario> desactivados = usuarioService.obtenerUsuariosDesactivados();

        double saldoTotal = todosUsuarios.stream()
                .mapToDouble(u -> u.getSaldo() != null ? u.getSaldo() : 0.0)
                .sum();

        return ResponseEntity.ok(new Object() {
            public int totalUsuarios = todosUsuarios.size();
            public int usuariosActivos = activos.size();
            public int usuariosDesactivados = desactivados.size();
            public double saldoTotalCirculante = saldoTotal;
        });
    }
}