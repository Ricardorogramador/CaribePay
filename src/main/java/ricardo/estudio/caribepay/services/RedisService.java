package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ===== OPERACIONES BÁSICAS =====

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Redis SET: {} = {}", key, value);
        } catch (Exception e) {
            log.error("Error en Redis SET: {}", e.getMessage());
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Redis SET con TTL: {} = {} (TTL: {}{})", key, value, timeout, unit);
        } catch (Exception e) {
            log.error("Error en Redis SET con TTL: {}", e.getMessage());
        }
    }

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Error en Redis GET: {}", e.getMessage());
            return null;
        }
    }

    public String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }

    public Long getAsLong(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    public Double getAsDouble(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis DELETE: {}", key);
        } catch (Exception e) {
            log.error("Error en Redis DELETE: {}", e.getMessage());
        }
    }

    public Boolean exists(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Error en Redis EXISTS: {}", e.getMessage());
            return false;
        }
    }

    public void increment(String key, long delta) {
        try {
            redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Error en Redis INCREMENT: {}", e.getMessage());
        }
    }

    public void decrement(String key, long delta) {
        try {
            redisTemplate.opsForValue().decrement(key, delta);
        } catch (Exception e) {
            log.error("Error en Redis DECREMENT: {}", e.getMessage());
        }
    }
}