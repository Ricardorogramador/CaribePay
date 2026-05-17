package ricardo.estudio.caribepay.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import ricardo.estudio.caribepay.models.Role;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Configuration
public class DataInitializer {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initializeAdminUser() {
        return args -> {
            log.info("========== INICIALIZANDO DATOS ==========");

            String emailAdmin = "admin@caribepay.com";

            // Verificar si el admin ya existe
            Optional<Usuario> adminExistente = usuarioRepository.findByEmail(emailAdmin);
            if (adminExistente.isPresent()) {
                log.info("✅ Usuario ADMIN ya existe: {}", emailAdmin);
                return;
            }

            // Crear usuario admin
            Usuario admin = new Usuario();
            admin.setEmail(emailAdmin);
            admin.setTelefono("+573101234567");
            admin.setPassword(passwordEncoder.encode("Admin123456"));
            admin.setRole(Role.ADMIN);
            admin.setActivo(true);
            admin.setSaldo(0.0);
            admin.setFechaCreacion(LocalDateTime.now());

            usuarioRepository.save(admin);

            log.info("========== ADMIN CREADO EXITOSAMENTE ==========");
            log.info("📧 Email: {}", emailAdmin);
            log.info("📱 Teléfono: +573101234567");
            log.info("🔐 Contraseña: Admin123456");
            log.info("⚠️  CAMBIAR ESTA CONTRASEÑA DESPUÉS DEL PRIMER LOGIN");
            log.info("=========================================");
        };
    }
}