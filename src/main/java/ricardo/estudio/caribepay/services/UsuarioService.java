package ricardo.estudio.caribepay.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ricardo.estudio.caribepay.dtos.RegistroDTO;
import ricardo.estudio.caribepay.dtos.UsuarioResponseDTO;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Usuario registrarUsuario(RegistroDTO registroDTO) {
        if (usuarioRepository.findByEmail(registroDTO.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(registroDTO.getEmail());
        usuario.setPassword(passwordEncoder.encode(registroDTO.getPassword()));
        usuario.setSaldo(0.0);
        usuario.setFechaCreacion(LocalDateTime.now());

        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public Optional<Usuario> obtenerUsuarioPorId(String id) {
        return usuarioRepository.findById(id);
    }

    public UsuarioResponseDTO convertirAResponse(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getSaldo(),
                usuario.getFechaCreacion()
        );
    }

    public void actualizarSaldo(String usuarioId, Double nuevoSaldo) {
        Optional<Usuario> usuario = usuarioRepository.findById(usuarioId);
        if (usuario.isPresent()) {
            usuario.get().setSaldo(nuevoSaldo);
            usuarioRepository.save(usuario.get());
        } else {
            throw new RuntimeException("Usuario no encontrado");
        }
    }
}
