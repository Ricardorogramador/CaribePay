package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ricardo.estudio.caribepay.dtos.TransaccionRedisDTO;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TransaccionRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisService redisService;

    @Autowired
    private TransaccionSyncService transaccionSyncService;

    @Autowired
    private RedisVidaService redisHealthService;

    private static final String SALDO_PREFIX = "saldo:";
    private static final String TRANSACCION_PREFIX = "tx:";
    private static final String COLA_TRANSACCIONES = "tx:queue";
    private static final String CONTADOR_TX = "tx:contador";

    public TransaccionRedisService(RedisTemplate<String, Object> redisTemplate,
                                   RedisService redisService) {
        this.redisTemplate = redisTemplate;
        this.redisService = redisService;
    }

    // ===== OPERACIONES DE SALDO =====

    public Long obtenerSaldo(String telefono) {
        String key = SALDO_PREFIX + telefono;
        Long saldo = redisService.getAsLong(key);
        log.debug("Saldo de {}: {}", telefono, saldo);
        return saldo != null ? saldo : 0L;
    }

    public void establecerSaldo(String telefono, Long saldo) {
        String key = SALDO_PREFIX + telefono;
        redisService.set(key, saldo);
        log.info("Saldo establecido para {}: {}", telefono, saldo);
    }

    public void incrementarSaldo(String telefono, Long monto) {
        String key = SALDO_PREFIX + telefono;
        redisService.increment(key, monto);
        log.debug("Saldo incrementado para {}: +{}", telefono, monto);
    }

    public void decrementarSaldo(String telefono, Long monto) {
        String key = SALDO_PREFIX + telefono;
        redisService.decrement(key, monto);
        log.debug("Saldo decrementado para {}: -{}", telefono, monto);
    }

    // ===== OPERACIONES DE TRANSACCIONES =====

    public synchronized TransaccionRedisDTO realizarTransaccion(
            String telefonoOrigen,
            String telefonoDestino,
            Long monto,
            String descripcion) {

        long startTime = System.currentTimeMillis();
        String idTransaccion = UUID.randomUUID().toString();

        try {
            if (telefonoOrigen.equals(telefonoDestino)) {
                log.warn("Intento de transferencia a sí mismo: {}", telefonoOrigen);
                return crearTransaccionFallida(idTransaccion, telefonoOrigen, telefonoDestino,
                        monto, "No puedes enviar dinero a ti mismo");
            }

            if (monto <= 0) {
                log.warn("Monto inválido: {}", monto);
                return crearTransaccionFallida(idTransaccion, telefonoOrigen, telefonoDestino,
                        monto, "El monto debe ser mayor a 0");
            }

            Long saldoOrigen = obtenerSaldo(telefonoOrigen);

            if (saldoOrigen < monto) {
                log.warn("Saldo insuficiente. {} intenta enviar {} pero tiene {}",
                        telefonoOrigen, monto, saldoOrigen);
                return crearTransaccionFallida(idTransaccion, telefonoOrigen, telefonoDestino,
                        monto, "Saldo insuficiente");
            }

            decrementarSaldo(telefonoOrigen, monto);
            incrementarSaldo(telefonoDestino, monto);

            TransaccionRedisDTO transaccion = new TransaccionRedisDTO(
                    idTransaccion, telefonoOrigen, telefonoDestino,
                    monto, "COMPLETADA", LocalDateTime.now(), descripcion
            );

            String keyTx = TRANSACCION_PREFIX + idTransaccion;
            redisService.set(keyTx, transaccion, 24, TimeUnit.HOURS);

            redisTemplate.opsForList().rightPush(COLA_TRANSACCIONES, transaccion);
            redisService.increment(CONTADOR_TX, 1);

            long duracion = System.currentTimeMillis() - startTime;
            log.info("✅ TX EXITOSA [{}ms]: {} → {} | ${} | ID: {}",
                    duracion, telefonoOrigen, telefonoDestino, monto, idTransaccion);

            return transaccion;

        } catch (Exception e) {
            log.error("❌ Error en transacción: {}", e.getMessage(), e);
            return crearTransaccionFallida(idTransaccion, telefonoOrigen, telefonoDestino,
                    monto, "Error: " + e.getMessage());
        }
    }

    public TransaccionRedisDTO realizarTransaccionConSync(
            String telefonoOrigen,
            String telefonoDestino,
            Long monto,
            String descripcion) {

        TransaccionRedisDTO tx = realizarTransaccion(telefonoOrigen, telefonoDestino, monto, descripcion);

        if ("COMPLETADA".equals(tx.getEstado()) && transaccionSyncService != null) {
            transaccionSyncService.sincronizarTransaccion(tx);
        }

        return tx;
    }

    /**
     * Sincroniza el lote completo a MongoDB y luego limpia la cola.
     * Este método es el dueño del ciclo de vida del batch.
     */
    public void sincronizarYLimpiarCola() {
        List<Object> cola = obtenerColaPendiente();
        if (!cola.isEmpty()) {
            transaccionSyncService.sincronizarLote(cola);
            limpiarColaPendiente(); // ✅ TransaccionRedisService limpia su propia cola
        }
    }

    private TransaccionRedisDTO crearTransaccionFallida(String id, String origen, String destino,
                                                        Long monto, String razon) {
        return new TransaccionRedisDTO(
                id, origen, destino, monto,
                "FALLIDA", LocalDateTime.now(), razon
        );
    }

    public TransaccionRedisDTO obtenerTransaccion(String idTransaccion) {
        String key = TRANSACCION_PREFIX + idTransaccion;
        Object tx = redisService.get(key);
        if (tx instanceof TransaccionRedisDTO) {
            return (TransaccionRedisDTO) tx;
        }
        log.warn("Transacción no encontrada: {}", idTransaccion);
        return null;
    }

    public Long obtenerContadorTransacciones() {
        Long contador = redisService.getAsLong(CONTADOR_TX);
        return contador != null ? contador : 0L;
    }

    public List<Object> obtenerColaPendiente() {
        Long size = redisTemplate.opsForList().size(COLA_TRANSACCIONES);
        if (size == null || size == 0) return new ArrayList<>();
        return redisTemplate.opsForList().range(COLA_TRANSACCIONES, 0, size - 1);
    }

    public void limpiarColaPendiente() {
        redisTemplate.delete(COLA_TRANSACCIONES);
        log.info("Cola de transacciones limpiada");
    }

    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTransacciones", obtenerContadorTransacciones());
        stats.put("colaPendiente", obtenerColaPendiente().size());
        stats.put("redisDisponible",
                redisHealthService != null ? redisHealthService.isRedisAvailable() : false);
        stats.put("timestamp", LocalDateTime.now());
        return stats;
    }

    public void resetearTodos() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        log.warn("⚠️ Redis completamente reseteado");
    }
}