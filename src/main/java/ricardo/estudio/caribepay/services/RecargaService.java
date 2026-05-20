package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ricardo.estudio.caribepay.models.Transaccion;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.TransaccionRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
public class RecargaService {

    private final SaldoService saldoService;
    private final TransaccionRepository transaccionRepository;

    public RecargaService(SaldoService saldoService, TransaccionRepository transaccionRepository) {
        this.saldoService = saldoService;
        this.transaccionRepository = transaccionRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Usuario recargar(String usuarioId, double monto) {
        log.info("Iniciando recarga para usuario: {}, monto: ${}", usuarioId, monto);

        if (monto <= 0) {
            log.warn("Intento de recarga con monto inválido: {}", monto);
            throw new IllegalArgumentException("Monto inválido");
        }

        try {
            // 1 Incremento atómico del saldo
            Usuario updated = saldoService.incrementarSaldo(usuarioId, monto);

            // 2 Registrar movimiento de recarga
            Transaccion tx = new Transaccion();
            tx.setEmisorId(usuarioId);
            tx.setReceptorId(usuarioId);
            tx.setMonto(monto);
            tx.setDescripcion("RECARGA");
            tx.setFecha(LocalDateTime.now());
            tx.setEstado("COMPLETADA");

            Transaccion guardada = transaccionRepository.save(tx);
            log.info("Recarga completada: usuario={}, monto=${}, transaccionId={}, nuevoSaldo=${}",
                    usuarioId, monto, guardada.getId(), updated.getSaldo());

            return updated;
        } catch (Exception e) {
            log.error("Error en recarga para usuario {}: {}", usuarioId, e.getMessage(), e);
            throw new RuntimeException("Recarga fallida: " + e.getMessage(), e);
        }
    }
}