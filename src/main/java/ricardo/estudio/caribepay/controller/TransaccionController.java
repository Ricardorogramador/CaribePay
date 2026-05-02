package ricardo.estudio.caribepay.controller;

import jakarta.validation.Valid;
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

    private final TransaccionService transaccionService;
    private final UsuarioService usuarioService;

    public TransaccionController(TransaccionService transaccionService, UsuarioService usuarioService) {
        this.transaccionService = transaccionService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/enviar")
    public ResponseEntity<TransaccionResponseDTO> crearTransaccion(
            @Valid @RequestBody TransaccionDTO transaccionDTO,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new IllegalArgumentException("No autenticado");
        }

        String email = authentication.getName();
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

        if (usuario.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        TransaccionResponseDTO response =
                transaccionService.crearTransaccion(usuario.get().getId(), transaccionDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/historial")
    public ResponseEntity<List<TransaccionResponseDTO>> obtenerHistorial(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("No autenticado");
        }

        String email = authentication.getName();
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

        if (usuario.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        List<TransaccionResponseDTO> transacciones =
                transaccionService.obtenerTransaccionesDelUsuario(usuario.get().getId());

        return ResponseEntity.ok(transacciones);
    }

    @GetMapping("/enviadas")
    public ResponseEntity<List<TransaccionResponseDTO>> obtenerTransaccionesEnviadas(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("No autenticado");
        }

        String email = authentication.getName();
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

        if (usuario.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        List<TransaccionResponseDTO> transacciones =
                transaccionService.obtenerTransaccionesEnviadas(usuario.get().getId());

        return ResponseEntity.ok(transacciones);
    }

    @GetMapping("/recibidas")
    public ResponseEntity<List<TransaccionResponseDTO>> obtenerTransaccionesRecibidas(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("No autenticado");
        }

        String email = authentication.getName();
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(email);

        if (usuario.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        List<TransaccionResponseDTO> transacciones =
                transaccionService.obtenerTransaccionesRecibidas(usuario.get().getId());

        return ResponseEntity.ok(transacciones);
    }
}