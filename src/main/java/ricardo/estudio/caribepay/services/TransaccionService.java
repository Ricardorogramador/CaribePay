package ricardo.estudio.caribepay.services;

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

@Service
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final UsuarioService usuarioService;
    private final PhoneService phoneService;

    public TransaccionService(TransaccionRepository transaccionRepository, UsuarioService usuarioService, PhoneService phoneService) {
        this.transaccionRepository = transaccionRepository;
        this.usuarioService = usuarioService;
        this.phoneService = phoneService;
    }

    @Transactional
    public TransaccionResponseDTO crearTransaccion(String emisorId, TransaccionDTO transaccionDTO) {
        Optional<Usuario> emisorOpt = usuarioService.obtenerUsuarioPorId(emisorId);
        if (emisorOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario emisor no encontrado");
        }
        Usuario emisor = emisorOpt.get();

        String telefonoDestinoNorm = phoneService.normalizarCO(transaccionDTO.getTelefonoDestino());
        Optional<Usuario> receptorOpt = usuarioService.obtenerUsuarioPorTelefonoNormalizado(telefonoDestinoNorm);
        if (receptorOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuario receptor no encontrado");
        }
        Usuario receptor = receptorOpt.get();

        if (emisor.getId().equals(receptor.getId())) {
            throw new IllegalArgumentException("No puedes enviar dinero a ti mismo");
        }

        if (transaccionDTO.getMonto() == null || transaccionDTO.getMonto() <= 0) {
            throw new IllegalArgumentException("Monto inválido");
        }

        if (emisor.getSaldo() < transaccionDTO.getMonto()) {
            throw new IllegalArgumentException("Saldo insuficiente");
        }

        // Actualizar saldos (nota: para producción se recomienda operación atómica/transacciones reales)
        Double nuevoSaldoEmisor = emisor.getSaldo() - transaccionDTO.getMonto();
        usuarioService.actualizarSaldo(emisor.getId(), nuevoSaldoEmisor);

        Double nuevoSaldoReceptor = receptor.getSaldo() + transaccionDTO.getMonto();
        usuarioService.actualizarSaldo(receptor.getId(), nuevoSaldoReceptor);

        Transaccion transaccion = new Transaccion();
        transaccion.setEmisorId(emisor.getId());
        transaccion.setReceptorId(receptor.getId());
        transaccion.setMonto(transaccionDTO.getMonto());
        transaccion.setDescripcion((transaccionDTO.getDescripcion() == null || transaccionDTO.getDescripcion().isBlank())
                ? "Transferencia" : transaccionDTO.getDescripcion().trim());
        transaccion.setFecha(LocalDateTime.now());
        transaccion.setEstado("COMPLETADA");

        Transaccion guardada = transaccionRepository.save(transaccion);
        return convertirAResponse(guardada);
    }

    public List<TransaccionResponseDTO> obtenerTransaccionesDelUsuario(String usuarioId) {
        List<Transaccion> transacciones = transaccionRepository.findByEmisorIdOrReceptorId(usuarioId, usuarioId);
        return transacciones.stream().map(this::convertirAResponse).collect(Collectors.toList());
    }

    public List<TransaccionResponseDTO> obtenerTransaccionesEnviadas(String usuarioId) {
        return transaccionRepository.findByEmisorId(usuarioId).stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    public List<TransaccionResponseDTO> obtenerTransaccionesRecibidas(String usuarioId) {
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