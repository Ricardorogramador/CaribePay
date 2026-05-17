package ricardo.estudio.caribepay.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ricardo.estudio.caribepay.dtos.TransaccionRedisDTO;
import ricardo.estudio.caribepay.models.Transaccion;
import ricardo.estudio.caribepay.repository.TransaccionRepository;
import ricardo.estudio.caribepay.services.TransaccionRedisService;
import ricardo.estudio.caribepay.services.TransaccionSyncService;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(SyncAsyncTestConfig.class) // ✅ desactiva async en este test
public class TransaccionSyncTest {

    @Autowired
    private TransaccionRedisService transaccionRedisService;

    @Autowired
    private TransaccionSyncService transaccionSyncService;

    @Autowired
    private TransaccionRepository transaccionRepository;

    @BeforeEach
    public void setup() {
        transaccionRedisService.resetearTodos();
        transaccionRepository.deleteAll();
    }

    @Test
    public void testSincronizacionAMongoDb() throws Exception {
        transaccionRedisService.establecerSaldo("3001111111", 100000L);
        transaccionRedisService.establecerSaldo("3002222222", 50000L);

        TransaccionRedisDTO tx = transaccionRedisService.realizarTransaccion(
                "3001111111", "3002222222", 30000L, "Test sync"
        );

        transaccionSyncService.sincronizarTransaccion(tx).get();

        Transaccion txMongo = transaccionRepository.findById(tx.getId()).orElse(null);
        assertNotNull(txMongo);
        assertEquals("COMPLETADA", txMongo.getEstado());
        System.out.println("✅ Sincronización exitosa!");
    }

    @Test
    public void testSincronizacionLote() throws Exception {
        transaccionRedisService.establecerSaldo("user1", 1000000L);
        transaccionRedisService.establecerSaldo("user2", 100000L);

        for (int i = 0; i < 50; i++) {
            transaccionRedisService.realizarTransaccion("user1", "user2", 1000L, "Test " + i);
        }

        List<Object> cola = transaccionRedisService.obtenerColaPendiente();
        assertEquals(50, cola.size());

        transaccionSyncService.sincronizarLote(cola).get(); // ✅ ahora sí espera

        List<Transaccion> txMongo = transaccionRepository.findByEstado("COMPLETADA");
        assertEquals(50, txMongo.size());
        System.out.println("✅ Sincronización de lote exitosa!");
    }
}