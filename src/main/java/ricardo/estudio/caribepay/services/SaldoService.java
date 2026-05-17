package ricardo.estudio.caribepay.services;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import ricardo.estudio.caribepay.models.Usuario;

@Service
public class SaldoService {

    private final MongoTemplate mongoTemplate;

    public SaldoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Incrementa saldo de forma ATÓMICA usando $inc y devuelve el usuario actualizado.
     */
    public Usuario incrementarSaldo(String usuarioId, double monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("Monto inválido");
        }

        Query query = new Query(Criteria.where("_id").is(usuarioId));
        Update update = new Update().inc("saldo", monto);

        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)   // devuelve el doc ya actualizado
                .upsert(false);

        Usuario updated = mongoTemplate.findAndModify(query, update, options, Usuario.class);

        if (updated == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        return updated;
    }
}