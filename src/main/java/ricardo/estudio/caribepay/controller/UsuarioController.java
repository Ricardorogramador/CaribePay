package ricardo.estudio.caribepay.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import ricardo.estudio.caribepay.dtos.UsuarioResponseDTO;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.services.RecargaService;
import ricardo.estudio.caribepay.services.UsuarioService;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final RecargaService recargaService;

    public UsuarioController(UsuarioService usuarioService, RecargaService recargaService) {
        this.usuarioService = usuarioService;
        this.recargaService = recargaService;
    }

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponseDTO> obtenerPerfil(Authentication authentication) {
        String email = authentication.getName();
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

        if (usuario.isEmpty()) {
            log.error("Usuario no encontrado: {}", email);
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        Usuario u = usuario.get();

        UsuarioResponseDTO respuesta = new UsuarioResponseDTO(
                u.getId(),
                u.getEmail(),
                u.getTelefono(),
                u.getSaldo(),
                u.getFechaCreacion(),
                u.getRole() != null ? u.getRole().getValue() : "USUARIO",
                u.getActivo(),
                u.getFechaDesactivacion()
        );

        log.debug("Perfil obtenido para usuario: {} (Role: {})", email, u.getRole());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuarioPorId(@PathVariable String id) {
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorId(id);

        if (usuario.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        return ResponseEntity.ok(usuarioService.convertirAResponse(usuario.get()));
    }

    @PostMapping("/agregar-saldo/{monto}")
    public ResponseEntity<UsuarioResponseDTO> agregarSaldo(@PathVariable Double monto, Authentication authentication) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("Monto inválido");
        }

        String email = authentication.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Usuario updated = recargaService.recargar(usuario.getId(), monto);

        return ResponseEntity.ok(usuarioService.convertirAResponse(updated));
    }
}