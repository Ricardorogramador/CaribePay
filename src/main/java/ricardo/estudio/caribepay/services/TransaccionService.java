package ricardo.estudio.caribepay.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ricardo.estudio.caribepay.dtos.TransaccionDTO;
import ricardo.estudio.caribepay.dtos.TransaccionResponseDTO;
import ricardo.estudio.caribepay.models.Transaccion;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.TransaccionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final UsuarioService usuarioService;
    private final PhoneService phoneService;
    private final SaldoService saldoService;

    public TransaccionService(
            TransaccionRepository transaccionRepository,
            UsuarioService usuarioService,
            PhoneService phoneService,
            SaldoService saldoService
    ) {
        this.transaccionRepository = transaccionRepository;
        this.usuarioService = usuarioService;
        this.phoneService = phoneService;
        this.saldoService = saldoService;
    }

    @Transactional
    public TransaccionResponseDTO crearTransaccion(String emisorId, TransaccionDTO transaccionDTO) {
        log.info("Iniciando transacción: emisor={}, teléfono destino={}, monto={}",
                emisorId, transaccionDTO.getTelefonoDestino(), transaccionDTO.getMonto());

        // 1 Obtener y validar emisor
        Optional<Usuario> emisorOpt = usuarioService.obtenerUsuarioPorId(emisorId);
        if (emisorOpt.isEmpty()) {
            log.error("Usuario emisor no encontrado: {}", emisorId);
            throw new IllegalArgumentException("Usuario emisor no encontrado");
        }
        Usuario emisor = emisorOpt.get();

        // 2 Normalizar teléfono y obtener receptor
        String telefonoDestinoNorm = phoneService.normalizarCO(transaccionDTO.getTelefonoDestino());
        Optional<Usuario> receptorOpt = usuarioService.obtenerUsuarioPorTelefonoNormalizado(telefonoDestinoNorm);
        if (receptorOpt.isEmpty()) {
            log.error("Usuario receptor no encontrado con teléfono: {}", telefonoDestinoNorm);
            throw new IllegalArgumentException("Usuario receptor no encontrado");
        }
        Usuario receptor = receptorOpt.get();

        // 3 Validar que no sea auto-transferencia
        if (emisor.getId().equals(receptor.getId())) {
            log.warn("Intento de auto-transferencia por usuario: {}", emisorId);
            throw new IllegalArgumentException("No puedes enviar dinero a ti mismo");
        }

        // 4 Validar monto
        if (transaccionDTO.getMonto() == null || transaccionDTO.getMonto() <= 0) {
            log.warn("Monto inválido en transacción: {}", transaccionDTO.getMonto());
            throw new IllegalArgumentException("Monto inválido");
        }

        double monto = transaccionDTO.getMonto();

        // 5 transferencia atomica
        log.debug("Ejecutando transferencia atómica: {} -> {} (${}", emisorId, receptor.getId(), monto);
        boolean transferenciaExitosa = saldoService.transferirSaldoAtomico(emisorId, receptor.getId(), monto);

        if (!transferenciaExitosa) {
            log.warn("Transferencia rechazada: saldo insuficiente. Emisor: {}, Monto requerido: ${}", emisorId, monto);
            throw new IllegalArgumentException("Saldo insuficiente");
        }

        // 6 Registrar transacción en BD
        Transaccion transaccion = new Transaccion();
        transaccion.setEmisorId(emisor.getId());
        transaccion.setReceptorId(receptor.getId());
        transaccion.setTelefonoOrigen(emisor.getTelefono());
        transaccion.setTelefonoDestino(receptor.getTelefono());
        transaccion.setMonto(monto);
        transaccion.setMontoLong(Math.round(monto));
        transaccion.setDescripcion(
                (transaccionDTO.getDescripcion() == null || transaccionDTO.getDescripcion().isBlank())
                        ? "Transferencia"
                        : transaccionDTO.getDescripcion().trim()
        );
        transaccion.setFecha(LocalDateTime.now());
        transaccion.setTimestamp(LocalDateTime.now());
        transaccion.setEstado("COMPLETADA");

        try {
            Transaccion guardada = transaccionRepository.save(transaccion);
            log.info("Transacción registrada exitosamente: ID={}, Monto=${}, Estado=COMPLETADA", guardada.getId(), monto);
            return convertirAResponse(guardada);
        } catch (Exception e) {
            log.error("Error guardando transacción. Revirtiendo saldos: {} <- {} (${})",
                    emisor.getId(), receptor.getId(), monto, e);
            try {
                saldoService.transferirSaldoAtomico(receptor.getId(), emisor.getId(), monto);
            } catch (Exception revertError) {
                log.error("Error al revertir saldos tras fallo de persistencia. Emisor={}, Receptor={}, Monto=${}",
                        emisor.getId(), receptor.getId(), monto, revertError);
            }
            throw new RuntimeException("No se pudo guardar la transacción en la base de datos", e);
        }
    }

    public List<TransaccionResponseDTO> obtenerTransaccionesDelUsuario(String usuarioId) {
        log.debug("Obteniendo historial de transacciones para usuario: {}", usuarioId);
        List<Transaccion> transacciones = transaccionRepository.findByEmisorIdOrReceptorId(usuarioId, usuarioId);
        return transacciones.stream().map(this::convertirAResponse).collect(Collectors.toList());
    }

    public List<TransaccionResponseDTO> obtenerTransaccionesEnviadas(String usuarioId) {
        log.debug("Obteniendo transacciones enviadas por usuario: {}", usuarioId);
        return transaccionRepository.findByEmisorId(usuarioId).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    public List<TransaccionResponseDTO> obtenerTransaccionesRecibidas(String usuarioId) {
        log.debug("Obteniendo transacciones recibidas por usuario: {}", usuarioId);
        return transaccionRepository.findByReceptorId(usuarioId).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    public TransaccionResponseDTO convertirAResponse(Transaccion transaccion) {
        String telEmisor = usuarioService.obtenerUsuarioPorId(transaccion.getEmisorId())
                .map(Usuario::getTelefono)
                .orElse(null);

        String telReceptor = usuarioService.obtenerUsuarioPorId(transaccion.getReceptorId())
                .map(Usuario::getTelefono)
                .orElse(null);

        return new TransaccionResponseDTO(
                transaccion.getId(),
                transaccion.getEmisorId(),
                transaccion.getReceptorId(),
                telEmisor,
                telReceptor,
                transaccion.getMonto(),
                transaccion.getDescripcion(),
                transaccion.getFecha(),
                transaccion.getEstado()
        );
    }
}
