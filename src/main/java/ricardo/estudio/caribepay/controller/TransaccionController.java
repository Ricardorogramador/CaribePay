package ricardo.estudio.caribepay.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ricardo.estudio.caribepay.dtos.TransaccionDTO;
import ricardo.estudio.caribepay.dtos.TransaccionRedisDTO;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.UsuarioRepository;
import ricardo.estudio.caribepay.services.TransaccionRedisService;
import ricardo.estudio.caribepay.services.TransaccionSyncService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    @Autowired
    private TransaccionRedisService transaccionRedisService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransaccionSyncService transaccionSyncService;

    /**
     * POST: Enviar dinero (usuario autenticado)
     * Flujo: JWT → Redis (instantáneo) → MongoDB (async)
     */
    @PostMapping("/enviar")
    public Map<String, Object> enviarDinero(
            @RequestBody TransaccionDTO dto,
            Authentication authentication) {

        // 1️ Obtener usuario autenticado desde JWT
        String emailOrigen = authentication.getName();
        Usuario usuarioOrigen = usuarioRepository.findByEmail(emailOrigen)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + emailOrigen));

        String telefonoOrigen = usuarioOrigen.getTelefono();
        String telefonoDestino = normalizarTelefono(dto.getTelefonoDestino());
        Long monto = dto.getMonto();
        String descripcion = dto.getDescripcion() != null ? dto.getDescripcion() : "Transferencia";

        // 2️ Validaciones
        if (monto == null || monto <= 0) {
            return crearRespuestaError("El monto debe ser mayor a 0");
        }

        if (telefonoOrigen.equals(telefonoDestino)) {
            return crearRespuestaError("No puedes enviar dinero a tu mismo número");
        }

        // 3️ Verificar destinatario existe
        Usuario usuarioDestino = usuarioRepository.findByTelefono(telefonoDestino)
                .orElse(null);

        if (usuarioDestino == null) {
            return crearRespuestaError("Destinatario no encontrado");
        }

        // 4️ Realizar transacción (Redis primero)
        log.info(" [{}] → [{}] | ${}", emailOrigen, usuarioDestino.getEmail(), monto);

        TransaccionRedisDTO tx = transaccionRedisService.realizarTransaccionConSync(
                telefonoOrigen, telefonoDestino, monto, descripcion
        );

        // 5️ Si es exitosa, auditar en MongoDB (async)
        if (tx.getEstado().equals("COMPLETADA")) {
            log.info(" Transacción {} completada - sync a MongoDB", tx.getId());
        }

        // 6️ Responder
        Map<String, Object> response = new HashMap<>();
        response.put("exitoso", tx.getEstado().equals("COMPLETADA"));
        response.put("transaccion", tx);
        response.put("mensaje", tx.getEstado().equals("COMPLETADA") ?
                "Dinero enviado ✓" : tx.getDescripcion());
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * GET: Obtener saldo del usuario autenticado
     */
    @GetMapping("/saldo")
    public Map<String, Object> obtenerSaldoUsuario(Authentication authentication) {
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Long saldo = transaccionRedisService.obtenerSaldo(usuario.getTelefono());

        Map<String, Object> response = new HashMap<>();
        response.put("usuario", email);
        response.put("telefono", usuario.getTelefono());
        response.put("saldo", saldo);
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    /**
     * GET: Historial de transacciones del usuario
     */
    @GetMapping("/historial")
    public Map<String, Object> obtenerHistorial(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));


        Map<String, Object> response = new HashMap<>();
        response.put("usuario", email);
        response.put("telefono", usuario.getTelefono());
        response.put("page", page);
        response.put("size", size);
        response.put("transacciones", new Object[0]);
        response.put("total", 0);

        return response;
    }

    /**
     * Normalizar teléfono: +57XXXXXXXXXX
     */
    private String normalizarTelefono(String telefono) {
        String limpio = telefono
                .trim()
                .replaceAll(" ", "")
                .replaceAll("-", "")
                .replaceAll("\\(", "")
                .replaceAll("\\)", "");

        if (!limpio.startsWith("+")) {
            limpio = "+" + limpio;
        }

        return limpio;
    }

    private Map<String, Object> crearRespuestaError(String mensaje) {
        Map<String, Object> response = new HashMap<>();
        response.put("exitoso", false);
        response.put("mensaje", mensaje);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}