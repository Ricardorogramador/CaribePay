package ricardo.estudio.caribepay.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ricardo.estudio.caribepay.dtos.TransaccionDTO;
import ricardo.estudio.caribepay.dtos.TransaccionResponseDTO;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.UsuarioRepository;
import ricardo.estudio.caribepay.services.TransaccionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final UsuarioRepository usuarioRepository;
    private final TransaccionService transaccionService;

    public TransaccionController(UsuarioRepository usuarioRepository, TransaccionService transaccionService) {
        this.usuarioRepository = usuarioRepository;
        this.transaccionService = transaccionService;
    }

    /**
     * POST: Enviar dinero (usuario autenticado)
     * Flujo: JWT → Redis (instantáneo) → MongoDB (async)
     */
    @PostMapping("/enviar")
    public ResponseEntity<Map<String, Object>> enviarDinero(
            @RequestBody TransaccionDTO dto,
            Authentication authentication) {

        // 1️ Obtener usuario autenticado desde JWT
        String emailOrigen = authentication.getName();
        Usuario usuarioOrigen = usuarioRepository.findByEmail(emailOrigen)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + emailOrigen));

        // 2️ Ejecutar flujo principal en MongoDB (saldo + transacción)
        TransaccionResponseDTO tx = transaccionService.crearTransaccion(usuarioOrigen.getId(), dto);

        // 3️ Responder
        Map<String, Object> response = new HashMap<>();
        response.put("exitoso", "COMPLETADA".equals(tx.getEstado()));
        response.put("transaccion", tx);
        response.put("mensaje", "Dinero enviado ✓");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET: Obtener saldo del usuario autenticado
     */
    @GetMapping("/saldo")
    public Map<String, Object> obtenerSaldoUsuario(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Map<String, Object> response = new HashMap<>();
        response.put("usuario", email);
        response.put("telefono", usuario.getTelefono());
        response.put("saldo", usuario.getSaldo() != null ? usuario.getSaldo() : 0.0);
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * GET: Historial de transacciones del usuario
     */
    @GetMapping("/historial")
    public List<TransaccionResponseDTO> obtenerHistorial(Authentication authentication) {

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return transaccionService.obtenerTransaccionesDelUsuario(usuario.getId());
    }
}
