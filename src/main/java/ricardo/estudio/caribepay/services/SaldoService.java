package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import ricardo.estudio.caribepay.models.Usuario;

@Slf4j
@Service
public class SaldoService {

    private final MongoTemplate mongoTemplate;

    public SaldoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Incrementa saldo de forma atomica usando $inc y devuelve el usuario actualizado.
     */
    public Usuario incrementarSaldo(String usuarioId, double monto) {
        if (monto <= 0) {
            log.warn("Intento de incrementar saldo con monto inválido: {}", monto);
            throw new IllegalArgumentException("Monto inválido");
        }

        Query query = new Query(Criteria.where("_id").is(usuarioId));
        Update update = new Update().inc("saldo", monto);

        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(false);

        Usuario updated = mongoTemplate.findAndModify(query, update, options, Usuario.class);

        if (updated == null) {
            log.error("Usuario no encontrado para incrementar saldo: {}", usuarioId);
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        log.info("Saldo incrementado para usuario {}: +${} (nuevo saldo: ${})", usuarioId, monto, updated.getSaldo());
        return updated;
    }

    /**
     * Decrementa saldo de forma ATÓMICA validando saldo suficiente.
     * Retorna el usuario actualizado si es exitoso, null si saldo insuficiente.
     */
    public Usuario decrementarSaldoConValidacion(String usuarioId, double monto) {
        if (monto <= 0) {
            log.warn("Intento de decrementar saldo con monto inválido: {}", monto);
            throw new IllegalArgumentException("Monto inválido");
        }

        // Validar saldo suficiente Y descontar en una sola operación
        Query query = new Query(Criteria.where("_id").is(usuarioId).and("saldo").gte(monto));
        Update update = new Update().inc("saldo", -monto);

        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(false);

        Usuario updated = mongoTemplate.findAndModify(query, update, options, Usuario.class);

        if (updated == null) {
            log.warn("No se pudo descontar saldo: usuario {} no existe o saldo insuficiente", usuarioId);
            return null;
        }

        log.info("Saldo decrementado para usuario {}: -${} (nuevo saldo: ${})", usuarioId, monto, updated.getSaldo());
        return updated;
    }

    /**
     * Transfiere saldo entre dos usuarios de forma ATÓMICA.
     * Garantiza que si el emisor no tiene saldo, ninguna operación ocurre.
     *
     * @param emisorId ID del usuario que envía dinero
     * @param receptorId ID del usuario que recibe dinero
     * @param monto Cantidad a transferir
     * @return true si la transferencia fue exitosa, false si saldo insuficiente
     */
    public boolean transferirSaldoAtomico(String emisorId, String receptorId, double monto) {
        if (monto <= 0) {
            log.warn("Intento de transferencia con monto inválido: {}", monto);
            throw new IllegalArgumentException("Monto inválido");
        }

        if (emisorId.equals(receptorId)) {
            log.warn("Intento de auto-transferencia por usuario: {}", emisorId);
            throw new IllegalArgumentException("No puedes transferir dinero a ti mismo");
        }

        log.debug("Iniciando transferencia atómica: {} → {} (${}", emisorId, receptorId, monto);

        // 1 Descontar del emisor ATÓMICAMENTE con validación
        Query queryEmisor = new Query(
                Criteria.where("_id").is(emisorId).and("saldo").gte(monto)
        );
        Update updateEmisor = new Update().inc("saldo", -monto);

        FindAndModifyOptions optionsEmisor = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(false);

        Usuario emisorActualizado = mongoTemplate.findAndModify(
                queryEmisor, updateEmisor, optionsEmisor, Usuario.class
        );

        // Si falla descontar (saldo insuficiente o usuario no existe), retornar false
        if (emisorActualizado == null) {
            log.warn("Transferencia rechazada: usuario {} no existe o saldo insuficiente", emisorId);
            return false;
        }

        // 2 Acreditar al receptor atomicamente
        Query queryReceptor = new Query(Criteria.where("_id").is(receptorId));
        Update updateReceptor = new Update().inc("saldo", monto);

        FindAndModifyOptions optionsReceptor = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(false);

        Usuario receptorActualizado = mongoTemplate.findAndModify(
                queryReceptor, updateReceptor, optionsReceptor, Usuario.class
        );

        // Si falla acreditar (receptor no existe), esto es un error grave
        if (receptorActualizado == null) {
            log.error("ERROR CRÍTICO: Saldo descontado de {} pero receptor {} no existe. Reversión necesaria.", emisorId, receptorId);
            // Revertir: acreditar nuevamente al emisor
            Query queryRevertir = new Query(Criteria.where("_id").is(emisorId));
            Update updateRevertir = new Update().inc("saldo", monto);
            mongoTemplate.findAndModify(queryRevertir, updateRevertir, optionsReceptor, Usuario.class);

            throw new RuntimeException("Error en transferencia: receptor no encontrado (saldo revertido)");
        }

        log.info("Transferencia exitosa: {} → {} (${}) - Emisor nuevo saldo: ${}, Receptor nuevo saldo: ${}",
                emisorId, receptorId, monto, emisorActualizado.getSaldo(), receptorActualizado.getSaldo());
        return true;
    }
}