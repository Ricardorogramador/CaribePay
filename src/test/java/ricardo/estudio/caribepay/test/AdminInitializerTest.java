package ricardo.estudio.caribepay.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import ricardo.estudio.caribepay.models.Role;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
public class AdminInitializerTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Test que crea un usuario ADMIN automáticamente
     * Ejecutar una sola vez con: mvn test -Dtest=AdminInitializerTest
     */
    @Test
    public void crearUsuarioAdmin() {
        log.info("========== INICIANDO CREACIÓN DE USUARIO ADMIN ==========");

        // Email y contraseña del admin
        String emailAdmin = "admin@caribepay.com";
        String telefonoAdmin = "+573101234567";
        String passwordAdmin = "Admin123456"; // Cambiar en producción

        // Verificar si el admin ya existe
        Optional<Usuario> adminExistente = usuarioRepository.findByEmail(emailAdmin);
        if (adminExistente.isPresent()) {
            log.warn("El usuario admin ya existe: {}", emailAdmin);
            assertTrue(true, "Admin ya fue creado anteriormente");
            return;
        }

        // Crear nuevo usuario admin
        Usuario admin = new Usuario();
        admin.setEmail(emailAdmin);
        admin.setTelefono(telefonoAdmin);
        admin.setPassword(passwordEncoder.encode(passwordAdmin));
        admin.setRole(Role.ADMIN);
        admin.setActivo(true);
        admin.setSaldo(0.0);
        admin.setFechaCreacion(LocalDateTime.now());

        // Guardar en base de datos
        Usuario adminGuardado = usuarioRepository.save(admin);

        log.info("✅ Usuario ADMIN creado exitosamente");
        log.info("   Email: {}", adminGuardado.getEmail());
        log.info("   Teléfono: {}", adminGuardado.getTelefono());
        log.info("   Role: {}", adminGuardado.getRole());
        log.info("   ID: {}", adminGuardado.getId());
        log.info("========== CONTRASEÑA TEMPORAL: {} ==========", passwordAdmin);
        log.info("⚠️  IMPORTANTE: Cambiar esta contraseña después del primer login");

        // Verificar que se creó correctamente
        Optional<Usuario> adminVerificado = usuarioRepository.findByEmail(emailAdmin);
        assertTrue(adminVerificado.isPresent(), "El admin debería estar creado");
        assertEquals(Role.ADMIN, adminVerificado.get().getRole(), "El role debería ser ADMIN");
        assertTrue(adminVerificado.get().getActivo(), "El admin debería estar activo");

        log.info("========== TEST COMPLETADO EXITOSAMENTE ==========");
    }
}