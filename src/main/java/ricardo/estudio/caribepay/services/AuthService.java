package ricardo.estudio.caribepay.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ricardo.estudio.caribepay.dtos.AuthResponseDTO;
import ricardo.estudio.caribepay.dtos.LoginDTO;
import ricardo.estudio.caribepay.dtos.RegistroDTO;
import ricardo.estudio.caribepay.models.Usuario;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponseDTO registrar(RegistroDTO registroDTO) {
        Usuario usuario = usuarioService.registrarUsuario(registroDTO);
        String token = jwtService.generarToken(usuario.getEmail());

        return new AuthResponseDTO(
                token,
                usuario.getEmail(),
                "Usuario registrado exitosamente"
        );
    }

    public AuthResponseDTO login(LoginDTO loginDTO) {
        Optional<Usuario> usuario = usuarioService.obtenerUsuarioPorEmail(loginDTO.getEmail());

        if (usuario.isEmpty()) {
            throw new RuntimeException("Email o contraseña incorrectos");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), usuario.get().getPassword())) {
            throw new RuntimeException("Email o contraseña incorrectos");
        }

        String token = jwtService.generarToken(usuario.get().getEmail());

        return new AuthResponseDTO(
                token,
                usuario.get().getEmail(),
                "Login exitoso"
        );
    }
}