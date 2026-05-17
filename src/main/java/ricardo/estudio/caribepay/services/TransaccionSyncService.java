package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ricardo.estudio.caribepay.dtos.TransaccionRedisDTO;
import ricardo.estudio.caribepay.models.Transaccion;
import ricardo.estudio.caribepay.repository.TransaccionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class TransaccionSyncService {

    private final TransaccionRepository transaccionRepository;

    public TransaccionSyncService(TransaccionRepository transaccionRepository) {
        this.transaccionRepository = transaccionRepository;
    }

    // ✅ Método interno SIN @Async — lógica real de guardado
    private void guardarEnMongo(TransaccionRedisDTO txRedis) {
        Transaccion tx = new Transaccion();
        tx.setId(txRedis.getId());
        tx.setTelefonoOrigen(txRedis.getTelefonoOrigen());
        tx.setTelefonoDestino(txRedis.getTelefonoDestino());
        tx.setMonto(Double.valueOf(txRedis.getMonto()));
        tx.setEstado(txRedis.getEstado());
        tx.setTimestamp(txRedis.getTimestamp());
        tx.setTimestampSync(LocalDateTime.now());
        tx.setDescripcion(txRedis.getDescripcion());
        transaccionRepository.save(tx);
        log.debug("✅ TX sincronizada a MongoDB: {}", txRedis.getId());
    }

    // ✅ Async para llamadas individuales desde producción
    @Async
    public CompletableFuture<Void> sincronizarTransaccion(TransaccionRedisDTO txRedis) {
        try {
            guardarEnMongo(txRedis); // delega al método interno
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("❌ Error sincronizando TX {} a MongoDB: {}", txRedis.getId(), e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    // ✅ Async para el lote — llama al método interno directamente, NO al @Async
    @Async
    public CompletableFuture<Void> sincronizarLote(List<Object> colaPendiente) {
        try {
            if (colaPendiente.isEmpty()) return CompletableFuture.completedFuture(null);

            long startTime = System.currentTimeMillis();

            colaPendiente.forEach(tx -> {
                if (tx instanceof TransaccionRedisDTO) {
                    try {
                        guardarEnMongo((TransaccionRedisDTO) tx); // ✅ directo, sin @Async
                    } catch (Exception e) {
                        log.error("Error en batch sync: {}", e.getMessage());
                    }
                }
            });

            long duracion = System.currentTimeMillis() - startTime;
            log.info("✅ Lote de {} transacciones sincronizado en {}ms",
                    colaPendiente.size(), duracion);

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("❌ Error en sincronización batch: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    // resto de métodos igual...
    public List<Transaccion> recuperarTransaccionesDeMongoDb(String telefono) {
        try {
            log.warn("⚠️ Redis no disponible, recuperando desde MongoDB: {}", telefono);
            List<Transaccion> txOrigen = transaccionRepository.findByTelefonoOrigen(telefono);
            List<Transaccion> txDestino = transaccionRepository.findByTelefonoDestino(telefono);
            txOrigen.addAll(txDestino);
            log.info("✅ Recuperadas {} transacciones de MongoDB", txOrigen.size());
            return txOrigen;
        } catch (Exception e) {
            log.error("❌ Error recuperando de MongoDB: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Transaccion> obtenerTransaccionesCompletadas() {
        return transaccionRepository.findByEstado("COMPLETADA");
    }

    public Map<String, Object> obtenerEstadisticasMongoDb() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCompletadas", transaccionRepository.countByEstado("COMPLETADA"));
        stats.put("totalFallidas", transaccionRepository.countByEstado("FALLIDA"));
        stats.put("total", transaccionRepository.count());
        return stats;
    }
}