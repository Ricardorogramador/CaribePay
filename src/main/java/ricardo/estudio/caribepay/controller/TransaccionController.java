package ricardo.estudio.caribepay.controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ricardo.estudio.caribepay.dtos.TransaccionDTO;
import ricardo.estudio.caribepay.dtos.TransaccionResponseDTO;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.services.TransaccionService;
import ricardo.estudio.caribepay.services.UsuarioService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transacciones")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class TransaccionController {

    @Autowired
    private TransaccionService transaccionService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/enviar")
    public ResponseEntity<?> crearTransaccion(@Valid @RequestBody TransaccionDTO transaccionDTO,
                                              Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

            if (usuario.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Usuario no encontrado");
            }

            TransaccionResponseDTO response = transaccionService.crearTransaccion(usuario.get().getId(), transaccionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno del servidor");
        }
    }

    @GetMapping("/historial")
    public ResponseEntity<List<TransaccionResponseDTO>> obtenerHistorial(Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

            if (usuario.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            List<TransaccionResponseDTO> transacciones = transaccionService.obtenerTransaccionesDelUsuario(usuario.get().getId());
            return ResponseEntity.ok(transacciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/enviadas")
    public ResponseEntity<List<TransaccionResponseDTO>> obtenerTransaccionesEnviadas(Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

            if (usuario.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            List<TransaccionResponseDTO> transacciones = transaccionService.obtenerTransaccionesEnviadas(usuario.get().getId());
            return ResponseEntity.ok(transacciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/recibidas")
    public ResponseEntity<List<TransaccionResponseDTO>> obtenerTransaccionesRecibidas(Authentication authentication) {
        try {
            String email = authentication.getName();
            Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

            if (usuario.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            List<TransaccionResponseDTO> transacciones = transaccionService.obtenerTransaccionesRecibidas(usuario.get().getId());
            return ResponseEntity.ok(transacciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
