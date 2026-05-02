package ricardo.estudio.caribepay.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ricardo.estudio.caribepay.models.Transaccion;
import java.util.List;

@Repository
public interface TransaccionRepository extends MongoRepository<Transaccion, String> {

    List<Transaccion> findByEmisorId(String emisorId);

    List<Transaccion> findByReceptorId(String receptorId);

    List<Transaccion> findByEmisorIdOrReceptorId(String emisorId, String receptorId);
}