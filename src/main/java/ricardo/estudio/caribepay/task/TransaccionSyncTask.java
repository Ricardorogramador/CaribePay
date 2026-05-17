package ricardo.estudio.caribepay.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ricardo.estudio.caribepay.services.RedisVidaService;
import ricardo.estudio.caribepay.services.RedisVidaService;
import ricardo.estudio.caribepay.services.TransaccionRedisService;
import ricardo.estudio.caribepay.services.TransaccionSyncService;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransaccionSyncTask {

    private final TransaccionRedisService transaccionRedisService;
    private final TransaccionSyncService transaccionSyncService;
    private final RedisVidaService redisHealthService;



    /**
     * Sincroniza transacciones pendientes cada 5 segundos
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void sincronizarColaPendiente() {
        try {
            if (!redisHealthService.isRedisAvailable()) {
                log.warn("⚠️ Redis no disponible, saltando sincronización");
                return;
            }

            List<Object> cola = transaccionRedisService.obtenerColaPendiente();

            if (!cola.isEmpty()) {
                log.info("📤 Sincronizando {} transacciones a MongoDB...", cola.size());
                transaccionSyncService.sincronizarLote(cola).get();
            }

        } catch (Exception e) {
            log.error("❌ Error en task de sincronización: {}", e.getMessage());
        }
    }

    /**
     * Verifica salud de Redis cada 30 segundos
     */
    @Scheduled(fixedDelay = 30000)
    public void verificarSaludRedis() {
        boolean disponible = redisHealthService.isRedisAvailable();
        String status = disponible ? "✅ DISPONIBLE" : "❌ NO DISPONIBLE";
        log.info("Redis Health Check: {}", status);
    }
}