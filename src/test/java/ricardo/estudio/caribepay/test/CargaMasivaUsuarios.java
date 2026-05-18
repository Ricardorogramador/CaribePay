package ricardo.estudio.caribepay.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import ricardo.estudio.caribepay.models.Role;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.UsuarioRepository;
import ricardo.estudio.caribepay.services.TransaccionRedisService;

import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * STEP 1: Carga 15.000 usuarios en MongoDB + saldos en Redis.
 *         Genera credentials.csv para que JMeter haga login con cada uno.
 *
 * FLUJO COMPLETO:
 *   1. Corre cargarUsuariosEnMongoDB() — una sola vez
 *   2. Copia credentials.csv junto al .jmx
 *   3. Lanza caribepay_loadtest.jmx en JMeter
 */
@SpringBootTest
public class CargaMasivaUsuarios {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransaccionRedisService transaccionRedisService;

    private static final int TOTAL_USUARIOS = 15_000;
    private static final String PASSWORD_PLANA = "Password123";
    private static final Double SALDO_INICIAL = 500_000.0;

    // Prefijos colombianos reales (+57 + 10 dígitos)
    private static final String[] PREFIJOS = {
            "300", "301", "302", "303", "304",
            "310", "311", "312", "313", "314",
            "315", "316", "317", "318", "319"
    };

    @Test
    public void cargarUsuariosEnMongoDB() throws Exception {
        System.out.println("🚀 Iniciando carga de " + TOTAL_USUARIOS + " usuarios...");

        // Limpiar datos previos
        usuarioRepository.deleteAll();
        transaccionRedisService.resetearTodos();
        System.out.println("🧹 MongoDB y Redis limpiados");

        // BCrypt es costoso — hashear UNA sola vez y reutilizar
        String passwordHash = passwordEncoder.encode(PASSWORD_PLANA);
        System.out.println("🔐 Password hasheada (BCrypt)");

        List<Usuario> lote = new ArrayList<>(500);
        FileWriter csv = new FileWriter("credentials.csv");
        csv.write("email,password,telefono_origen,telefono_destino\n");

        // Guardamos todos los teléfonos para generar pares origen-destino en el CSV
        List<String> todosTelefonos = new ArrayList<>(TOTAL_USUARIOS);

        long startTime = System.currentTimeMillis();
        int guardados = 0;

        for (int i = 0; i < TOTAL_USUARIOS; i++) {
            String prefijo = PREFIJOS[i % PREFIJOS.length];
            // Genera teléfonos únicos: +573001000000, +573011000001, etc.
            String telefono = String.format("+57%s%07d", prefijo, i).trim();
            String email = String.format("usuario%07d@caribepay.com", i).trim();

            Usuario u = new Usuario();
            u.setEmail(email);
            u.setTelefono(telefono);
            u.setPassword(passwordHash);
            u.setSaldo(SALDO_INICIAL);
            u.setFechaCreacion(LocalDateTime.now());
            u.setRole(Role.USUARIO);
            u.setActivo(true);
            u.setFechaDesactivacion(null);

            lote.add(u);
            todosTelefonos.add(telefono);

            // Insertar en lotes de 500
            if (lote.size() == 500) {
                usuarioRepository.saveAll(lote);

                // Cargar saldo en Redis por teléfono
                for (Usuario usuario : lote) {
                    transaccionRedisService.establecerSaldo(usuario.getTelefono(), 500_000L);
                }

                guardados += lote.size();
                lote.clear();

                System.out.printf("📦 %d/%d usuarios guardados%n", guardados, TOTAL_USUARIOS);
            }
        }

        // Último lote si sobró
        if (!lote.isEmpty()) {
            usuarioRepository.saveAll(lote);
            for (Usuario u : lote) {
                transaccionRedisService.establecerSaldo(u.getTelefono(), 500_000L);
            }
            guardados += lote.size();
        }

        // Generar CSV con pares origen-destino para las transacciones en JMeter
        // Cada fila: el usuario hace login Y luego envía dinero a otro
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < TOTAL_USUARIOS; i++) {
            String email = String.format("usuario%07d@caribepay.com", i).trim();
            String telefonoOrigen = todosTelefonos.get(i).trim();

            // Destino aleatorio distinto al origen
            int idxDestino;
            do { idxDestino = rnd.nextInt(TOTAL_USUARIOS); } while (idxDestino == i);
            String telefonoDestino = todosTelefonos.get(idxDestino).trim();

            csv.write(email + "," + PASSWORD_PLANA + "," + telefonoOrigen + "," + telefonoDestino + "\n");
        }

        csv.flush();
        csv.close();

        long duracion = System.currentTimeMillis() - startTime;
        System.out.println("═══════════════════════════════════════════════");
        System.out.printf("✅ Carga completada en %d ms%n", duracion);
        System.out.printf("   👥 Usuarios en MongoDB: %d%n", usuarioRepository.count());
        System.out.printf("   💰 Saldos en Redis: %d%n", guardados);
        System.out.printf("   ⚡ Velocidad: %.0f usuarios/seg%n", (guardados * 1000.0) / duracion);
        System.out.println("   📁 credentials.csv generado para JMeter");
        System.out.println("   🔑 Password de todos: " + PASSWORD_PLANA);
        System.out.println("═══════════════════════════════════════════════");
        System.out.println("👉 Siguiente paso: lanza caribepay_loadtest.jmx en JMeter");
    }
}