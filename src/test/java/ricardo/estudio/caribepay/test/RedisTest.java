package ricardo.estudio.caribepay.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ricardo.estudio.caribepay.services.RedisService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RedisTest {

    @Autowired
    private RedisService redisService;

    @Test
    public void testRedisBasico() {
        // Guardar
        redisService.set("test:key", "Hello Redis!");

        // Recuperar
        String value = redisService.getString("test:key");

        // Verificar
        assertEquals("Hello Redis!", value);
        System.out.println("✅ Redis funcionando correctamente!");
    }

    @Test
    public void testRedisNumeros() {
        redisService.set("contador", 100);
        redisService.increment("contador", 50);

        Long result = redisService.getAsLong("contador");
        assertEquals(150L, result);
        System.out.println("✅ Incrementos funcionando!");
    }
}
