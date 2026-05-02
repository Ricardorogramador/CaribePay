package ricardo.estudio.caribepay.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import ricardo.estudio.caribepay.dtos.UsuarioResponseDTO;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.services.UsuarioService;

import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponseDTO> obtenerPerfil(Authentication authentication) {
        String email = authentication.getName();
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

        if (usuario.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        UsuarioResponseDTO response = usuarioService.convertirAResponse(usuario.get());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuarioPorId(@PathVariable String id) {
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorId(id);

        if (usuario.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        UsuarioResponseDTO response = usuarioService.convertirAResponse(usuario.get());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/agregar-saldo/{monto}")
    public ResponseEntity<String> agregarSaldo(@PathVariable Double monto, Authentication authentication) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("Monto inválido");
        }

        String email = authentication.getName();
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

        if (usuario.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        Double nuevoSaldo = usuario.get().getSaldo() + monto;
        usuarioService.actualizarSaldo(usuario.get().getId(), nuevoSaldo);

        return ResponseEntity.ok("Saldo agregado exitosamente. Nuevo saldo: " + nuevoSaldo);
    }
}