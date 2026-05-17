package ricardo.estudio.caribepay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ricardo.estudio.caribepay.dtos.TransaccionRedisDTO;
import ricardo.estudio.caribepay.services.TransaccionRedisService;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TransaccionRedisTest {

    @Autowired
    private TransaccionRedisService transaccionRedisService;

    @BeforeEach
    public void setup() {
        transaccionRedisService.resetearTodos();
    }

    @Test
    public void testTransaccionExitosa() {
        // Setup
        transaccionRedisService.establecerSaldo("3001111111", 100000L);
        transaccionRedisService.establecerSaldo("3002222222", 50000L);

        // Realizar transacción
        TransaccionRedisDTO tx = transaccionRedisService.realizarTransaccion(
                "3001111111", "3002222222", 30000L, "Pago servicios"
        );

        // Verificaciones
        assertEquals("COMPLETADA", tx.getEstado());
        assertEquals(70000L, transaccionRedisService.obtenerSaldo("3001111111"));
        assertEquals(80000L, transaccionRedisService.obtenerSaldo("3002222222"));
        System.out.println("✅ Transacción exitosa!");
    }

    @Test
    public void testSaldoInsuficiente() {
        // Setup
        transaccionRedisService.establecerSaldo("3001111111", 10000L);
        transaccionRedisService.establecerSaldo("3002222222", 50000L);

        // Intentar transacción con saldo insuficiente
        TransaccionRedisDTO tx = transaccionRedisService.realizarTransaccion(
                "3001111111", "3002222222", 50000L, "Pago"
        );

        // Verificaciones
        assertEquals("FALLIDA", tx.getEstado());
        assertEquals(10000L, transaccionRedisService.obtenerSaldo("3001111111"));
        System.out.println("✅ Validación de saldo funcionando!");
    }

    @Test
    public void testMultiplesTransacciones() {
        // Setup
        transaccionRedisService.establecerSaldo("user1", 1000000L);
        transaccionRedisService.establecerSaldo("user2", 100000L);
        transaccionRedisService.establecerSaldo("user3", 100000L);

        // Realizar múltiples transacciones
        for (int i = 0; i < 100; i++) {
            transaccionRedisService.realizarTransaccion("user1", "user2", 1000L, "Test " + i);
            transaccionRedisService.realizarTransaccion("user2", "user3", 500L, "Test " + i);
        }

        // Verificar contador
        Long total = transaccionRedisService.obtenerContadorTransacciones();
        assertEquals(200L, total);
        System.out.println("✅ " + total + " transacciones completadas!");
    }
}