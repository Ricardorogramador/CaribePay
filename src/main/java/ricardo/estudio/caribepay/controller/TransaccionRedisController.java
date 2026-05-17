package ricardo.estudio.caribepay.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ricardo.estudio.caribepay.dtos.TransaccionRedisDTO;
import ricardo.estudio.caribepay.services.RedisVidaService;
import ricardo.estudio.caribepay.services.TransaccionRedisService;
import ricardo.estudio.caribepay.services.TransaccionSyncService;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/redis/transacciones")
public class TransaccionRedisController {

    private final TransaccionRedisService transaccionRedisService;

    @Autowired
    private TransaccionSyncService transaccionSyncService;

    @Autowired
    private RedisVidaService redisVidaService;

    public TransaccionRedisController(TransaccionRedisService transaccionRedisService) {
        this.transaccionRedisService = transaccionRedisService;
    }

    /**
     * POST: Realizar una transacción CON sincronización a MongoDB
     */
    @PostMapping("/enviar")
    public Map<String, Object> enviarDinero(
            @RequestParam String telefonoOrigen,
            @RequestParam String telefonoDestino,
            @RequestParam Long monto,
            @RequestParam(required = false, defaultValue = "Transferencia") String descripcion) {

        // Usar la versión con sync automático
        TransaccionRedisDTO tx = transaccionRedisService.realizarTransaccionConSync(
                telefonoOrigen, telefonoDestino, monto, descripcion
        );

        Map<String, Object> response = new HashMap<>();
        response.put("exitoso", tx.getEstado().equals("COMPLETADA"));
        response.put("transaccion", tx);
        response.put("mensaje", tx.getEstado().equals("COMPLETADA") ? "Dinero enviado ✓" : tx.getDescripcion());
        return response;
    }

    /**
     * GET: Obtener saldo de un usuario
     */
    @GetMapping("/saldo/{telefono}")
    public Map<String, Object> obtenerSaldo(@PathVariable String telefono) {
        Long saldo = transaccionRedisService.obtenerSaldo(telefono);
        Map<String, Object> response = new HashMap<>();
        response.put("telefono", telefono);
        response.put("saldo", saldo);
        return response;
    }

    /**
     * POST: Establecer saldo inicial (para testing)
     */
    @PostMapping("/saldo/{telefono}/establecer")
    public Map<String, Object> establecerSaldo(
            @PathVariable String telefono,
            @RequestParam Long saldo) {
        transaccionRedisService.establecerSaldo(telefono, saldo);
        Map<String, Object> response = new HashMap<>();
        response.put("telefono", telefono);
        response.put("saldo", saldo);
        response.put("mensaje", "Saldo establecido correctamente");
        return response;
    }

    /**
     * GET: Obtener estadísticas de Redis
     */
    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticas() {
        return transaccionRedisService.obtenerEstadisticas();
    }

    /**
     * GET: Obtener transacción por ID
     */
    @GetMapping("/{idTransaccion}")
    public Map<String, Object> obtenerTransaccion(@PathVariable String idTransaccion) {
        TransaccionRedisDTO tx = transaccionRedisService.obtenerTransaccion(idTransaccion);
        Map<String, Object> response = new HashMap<>();
        response.put("transaccion", tx);
        return response;
    }

    /**
     * GET: Estado de Redis (Health Check)
     */
    @GetMapping("/health/redis")
    public Map<String, Object> healthRedis() {
        return redisVidaService.getRedisStatus();
    }

    /**
     * GET: Estadísticas de MongoDB
     */
    @GetMapping("/estadisticas/mongodb")
    public Map<String, Object> estadisticasMongoDb() {
        return transaccionSyncService.obtenerEstadisticasMongoDb();
    }

    /**
     * GET: Cola pendiente de sincronización
     */
    @GetMapping("/cola-pendiente")
    public Map<String, Object> colaPendiente() {
        Map<String, Object> response = new HashMap<>();
        response.put("pendientes", transaccionRedisService.obtenerColaPendiente().size());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * POST: Reset (solo testing)
     */
    @PostMapping("/reset")
    public Map<String, String> reset() {
        transaccionRedisService.resetearTodos();
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Redis reseteado");
        return response;
    }
}