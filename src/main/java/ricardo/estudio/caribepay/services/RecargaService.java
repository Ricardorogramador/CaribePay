package ricardo.estudio.caribepay.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ricardo.estudio.caribepay.models.Transaccion;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.TransaccionRepository;

import java.time.LocalDateTime;

@Service
public class RecargaService {

    private final SaldoService saldoService;
    private final TransaccionRepository transaccionRepository;

    public RecargaService(SaldoService saldoService, TransaccionRepository transaccionRepository) {
        this.saldoService = saldoService;
        this.transaccionRepository = transaccionRepository;
    }

    /**
     * Recarga saldo y registra un movimiento tipo "RECARGA".
     * Nota: @Transactional con Mongo requiere replica set para transacciones reales,
     * pero incluso sin ello, al menos el $inc es atómico y la transacción se guarda aparte.
     */
    @Transactional
    public Usuario recargar(String usuarioId, double monto) {
        if (monto <= 0) throw new IllegalArgumentException("Monto inválido");

        // 1) Incremento atómico
        Usuario updated = saldoService.incrementarSaldo(usuarioId, monto);

        // 2) Registrar movimiento
        Transaccion tx = new Transaccion();
        tx.setEmisorId(usuarioId);
        tx.setReceptorId(usuarioId);
        tx.setMonto(monto);
        tx.setDescripcion("RECARGA");
        tx.setFecha(LocalDateTime.now());
        tx.setEstado("COMPLETADA");

        transaccionRepository.save(tx);

        return updated;
    }
}