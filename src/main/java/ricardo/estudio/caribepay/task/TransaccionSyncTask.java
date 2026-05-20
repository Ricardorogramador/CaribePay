package ricardo.estudio.caribepay.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ricardo.estudio.caribepay.services.RedisVidaService;
import ricardo.estudio.caribepay.services.TransaccionRedisService;
import ricardo.estudio.caribepay.services.TransaccionSyncService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransaccionSyncTask {

    private final TransaccionRedisService transaccionRedisService;
    private final TransaccionSyncService transaccionSyncService;
    private final RedisVidaService redisHealthService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Sincroniza transacciones pendientes cada 5 segundos
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void sincronizarColaPendiente() {
        try {
            if (!redisHealthService.isRedisAvailable()) {
                log.warn("Redis no disponible, saltando sincronización");
                return;
            }

            List<Object> cola = transaccionRedisService.obtenerColaPendiente();

            if (!cola.isEmpty()) {
                log.info("[{}] Sincronizando {} transacciones a MongoDB...",
                        LocalDateTime.now().format(formatter), cola.size());

                // Sincroniza el lote
                transaccionSyncService.sincronizarLote(cola).get();

                log.info("Lote sincronizado correctamente. Esperando próximo ciclo de limpieza...");
            } else {
                log.debug("Cola vacía, nada que sincronizar");
            }

        } catch (Exception e) {
            log.error("Error en task de sincronización: {}", e.getMessage(), e);
        }
    }

    /**
     * Limpia la cola cada 10 minutos (600000 ms)
     */
    @Scheduled(fixedDelay = 600000, initialDelay = 600000)
    public void limpiarColaPendiente() {
        try {
            long colaSize = transaccionRedisService.obtenerColaPendiente().size();

            if (colaSize > 0) {
                log.info("[{}] Limpiando cola de Redis... ({} transacciones pendientes)",
                        LocalDateTime.now().format(formatter), colaSize);

                transaccionRedisService.limpiarColaPendiente();

                log.info("[{}] Cola limpiada exitosamente. {} transacciones fueron removidas de Redis",
                        LocalDateTime.now().format(formatter), colaSize);
            } else {
                log.info("[{}] Cola ya estaba vacía. Nada que limpiar",
                        LocalDateTime.now().format(formatter));
            }

        } catch (Exception e) {
            log.error("Error al limpiar cola: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica salud de Redis cada 30 segundos
     */
    @Scheduled(fixedDelay = 30000)
    public void verificarSaludRedis() {
        try {
            boolean disponible = redisHealthService.isRedisAvailable();
            String status = disponible ? "✅ DISPONIBLE" : "❌ NO DISPONIBLE";
            log.debug("Redis Health Check: {}", status);
        } catch (Exception e) {
            log.error("Error verificando salud de Redis: {}", e.getMessage());
        }
    }

    /**
     * Muestra estadísticas cada 60 segundos
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 30000)
    public void mostrarEstadisticas() {
        try {
            long colaPendiente = transaccionRedisService.obtenerColaPendiente().size();
            long totalTransacciones = transaccionRedisService.obtenerContadorTransacciones();

            log.info("Estadistica - Total TX: {} | Cola pendiente: {}",
                    totalTransacciones, colaPendiente);
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas: {}", e.getMessage());
        }
    }
}