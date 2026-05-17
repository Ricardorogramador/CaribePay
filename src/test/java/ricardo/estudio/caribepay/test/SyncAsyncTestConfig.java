package ricardo.estudio.caribepay.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@TestConfiguration
public class SyncAsyncTestConfig {

    // Reemplaza el executor async por uno síncrono en tests
    @Bean
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor(); // ✅ ejecuta en el mismo hilo, sin async
    }
}