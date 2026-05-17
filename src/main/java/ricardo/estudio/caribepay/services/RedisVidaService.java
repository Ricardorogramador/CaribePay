package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class RedisVidaService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisVidaService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Verifica si Redis está disponible
     */
    public boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("✅ Redis disponible");
            return true;
        } catch (Exception e) {
            log.error("❌ Redis no disponible: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene estado de Redis
     */
    public Map<String, Object> getRedisStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean available = isRedisAvailable();

        status.put("disponible", available);
        status.put("timestamp", System.currentTimeMillis());

        if (available) {
            try {
                status.put("info", redisTemplate.getConnectionFactory()
                        .getConnection().info());
            } catch (Exception e) {
                status.put("info", "No disponible");
            }
        }

        return status;
    }
}