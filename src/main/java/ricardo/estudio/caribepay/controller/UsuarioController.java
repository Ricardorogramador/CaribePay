package ricardo.estudio.caribepay.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ricardo.estudio.caribepay.dtos.UsuarioResponseDTO;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.services.JwtService;
import ricardo.estudio.caribepay.services.UsuarioService;

import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtService jwtService;

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponseDTO> obtenerPerfil(Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

            if (usuario.isPresent()) {
                UsuarioResponseDTO response = usuarioService.convertirAResponse(usuario.get());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> obtenerUsuarioPorId(@PathVariable String id) {
        try {
            Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorId(id);

            if (usuario.isPresent()) {
                UsuarioResponseDTO response = usuarioService.convertirAResponse(usuario.get());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/agregar-saldo/{monto}")
    public ResponseEntity<?> agregarSaldo(@PathVariable Double monto, Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

            if (usuario.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Usuario no encontrado");
            }

            Double nuevoSaldo = usuario.get().getSaldo() + monto;
            usuarioService.actualizarSaldo(usuario.get().getId(), nuevoSaldo);

            return ResponseEntity.ok("Saldo agregado exitosamente. Nuevo saldo: " + nuevoSaldo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno del servidor");
        }
    }
}