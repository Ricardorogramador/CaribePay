package ricardo.estudio.caribepay.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ricardo.estudio.caribepay.models.Transaccion;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransaccionRepository extends MongoRepository<Transaccion, String> {

    List<Transaccion> findByEmisorId(String emisorId);

    List<Transaccion> findByReceptorId(String receptorId);

    List<Transaccion> findByEmisorIdOrReceptorId(String emisorId, String receptorId);

    // Nuevas queries para Redis sync
    List<Transaccion> findByTelefonoOrigen(String telefono);

    List<Transaccion> findByTelefonoDestino(String telefono);

    List<Transaccion> findByEstado(String estado);

    List<Transaccion> findByTimestampBetween(LocalDateTime inicio, LocalDateTime fin);

    Long countByEstado(String estado);

    List<Transaccion> findByTelefonoOrigenAndEstado(String telefono, String estado);

    List<Transaccion> findByTelefonoDestinoAndEstado(String telefono, String estado);
}