package ricardo.estudio.caribepay.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ricardo.estudio.caribepay.models.Usuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByTelefono(String telefono);
    
    List<Usuario> findByActivo(Boolean activo);
}