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
    private static final Long SALDO_INICIAL = 500_000L;

    private static final String[] PREFIJOS = {
            "300", "301", "302", "303", "304",
            "310", "311", "312", "313", "314",
            "315", "316", "317", "318", "319"
    };

    @Test
    public void cargarUsuariosEnMongoDB() throws Exception {
        System.out.println("🚀 Iniciando carga de " + TOTAL_USUARIOS + " usuarios...");

        usuarioRepository.deleteAll();
        transaccionRedisService.resetearTodos();
        System.out.println("🧹 MongoDB y Redis limpiados");

        String passwordHash = passwordEncoder.encode(PASSWORD_PLANA);
        System.out.println("🔐 Password hasheada (BCrypt)");

        List<Usuario> lote = new ArrayList<>(500);
        FileWriter csv = new FileWriter("credentials.csv");
        csv.write("email,password,telefono_origen,telefono_destino\n");

        List<String> todosTelefonos = new ArrayList<>(TOTAL_USUARIOS);

        long startTime = System.currentTimeMillis();
        int guardados = 0;

        for (int i = 0; i < TOTAL_USUARIOS; i++) {
            String prefijo = PREFIJOS[i % PREFIJOS.length];
            String telefono = String.format("+57%s%07d", prefijo, i).trim();
            String email = String.format("usuario%07d@caribepay.com", i).trim();

            Usuario u = new Usuario();
            u.setEmail(email);
            u.setTelefono(telefono);
            u.setPassword(passwordHash);
            u.setSaldo((double) SALDO_INICIAL);
            u.setFechaCreacion(LocalDateTime.now());
            u.setRole(Role.USUARIO);
            u.setActivo(true);
            u.setFechaDesactivacion(null);

            lote.add(u);
            todosTelefonos.add(telefono);

            if (lote.size() == 500) {
                usuarioRepository.saveAll(lote);

                for (Usuario usuario : lote) {
                    transaccionRedisService.establecerSaldo(usuario.getTelefono(), SALDO_INICIAL);
                }

                guardados += lote.size();
                lote.clear();

                System.out.printf("📦 %d/%d usuarios guardados%n", guardados, TOTAL_USUARIOS);
            }
        }

        if (!lote.isEmpty()) {
            usuarioRepository.saveAll(lote);
            for (Usuario u : lote) {
                transaccionRedisService.establecerSaldo(u.getTelefono(), SALDO_INICIAL);
            }
            guardados += lote.size();
        }

        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < TOTAL_USUARIOS; i++) {
            String email = String.format("usuario%07d@caribepay.com", i).trim();
            String telefonoOrigen = todosTelefonos.get(i).trim();

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
        System.out.println("   🔑 Password: " + PASSWORD_PLANA);
        System.out.println("═══════════════════════════════════════════════");
    }
}