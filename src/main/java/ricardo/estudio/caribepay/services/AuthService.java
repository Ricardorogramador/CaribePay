package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import ricardo.estudio.caribepay.dtos.AuthResponseDTO;
import ricardo.estudio.caribepay.dtos.LoginDTO;
import ricardo.estudio.caribepay.dtos.RegistroDTO;
import ricardo.estudio.caribepay.models.Usuario;

import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsuarioService usuarioService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponseDTO registrar(RegistroDTO registroDTO) {
        log.info("Iniciando registro: {}", registroDTO.getEmail());

        try {
            Usuario usuario = usuarioService.registrarUsuario(registroDTO);
            String token = jwtService.generarToken(usuario.getEmail());

            log.info("Registro exitoso: {}", usuario.getEmail());
            return new AuthResponseDTO(
                    token,
                    usuario.getEmail(),
                    "Usuario registrado exitosamente"
            );
        } catch (Exception e) {
            log.error("Error en registro: {}", e.getMessage(), e);
            throw e;
        }
    }

    public AuthResponseDTO login(LoginDTO loginDTO) {
        log.info("Intento de login: {}", loginDTO.getEmail());

        try {
            Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(loginDTO.getEmail());

            if (usuario.isEmpty()) {
                log.warn("Login fallido: usuario no encontrado - {}", loginDTO.getEmail());
                throw new IllegalArgumentException("Email o contraseña incorrectos");
            }

            if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.get().getPassword())) {
                log.warn("Login fallido: contraseña incorrecta - {}", loginDTO.getEmail());
                throw new IllegalArgumentException("Email o contraseña incorrectos");
            }

            String token = jwtService.generarToken(usuario.get().getEmail());
            log.info("Login exitoso: {}", usuario.get().getEmail());

            return new AuthResponseDTO(
                    token,
                    usuario.get().getEmail(),
                    "Login exitoso"
            );
        } catch (Exception e) {
            log.error("Error en login: {}", e.getMessage());
            throw e;
        }
    }
}