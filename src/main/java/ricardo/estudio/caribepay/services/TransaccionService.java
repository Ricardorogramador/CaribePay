package ricardo.estudio.caribepay.services;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private TransaccionRepository transaccionRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Transactional
    public TransaccionResponseDTO crearTransaccion(String emisorId, TransaccionDTO transaccionDTO) {
        // Validar que el emisor exista
        Optional<Usuario> emisor = usuarioService.obtenerUsuarioPorId(emisorId);
        if (emisor.isEmpty()) {
            throw new RuntimeException("Usuario emisor no encontrado");
        }

        // Validar que el receptor exista (por EMAIL)
        Optional<Usuario> receptor = usuarioService.obtenerUsuarioPorEmail(transaccionDTO.getEmailDestino());
        if (receptor.isEmpty()) {
            throw new RuntimeException("Usuario receptor no encontrado");
        }

        // Validar que no se envíe dinero a sí mismo
        if (emisorId.equals(receptor.get().getId())) {
            throw new RuntimeException("No puedes enviar dinero a ti mismo");
        }

        // Validar saldo del emisor
        if (emisor.get().getSaldo() < transaccionDTO.getMonto()) {
            throw new RuntimeException("Saldo insuficiente");
        }

        // Restar saldo del emisor
        Double nuevoSaldoEmisor = emisor.get().getSaldo() - transaccionDTO.getMonto();
        usuarioService.actualizarSaldo(emisorId, nuevoSaldoEmisor);

        // Sumar saldo al receptor
        Double nuevoSaldoReceptor = receptor.get().getSaldo() + transaccionDTO.getMonto();
        usuarioService.actualizarSaldo(receptor.get().getId(), nuevoSaldoReceptor);

        // Crear y guardar la transacción
        Transaccion transaccion = new Transaccion();
        transaccion.setEmisorId(emisorId);
        transaccion.setReceptorId(receptor.get().getId());
        transaccion.setMonto(transaccionDTO.getMonto());
        transaccion.setDescripcion(transaccionDTO.getDescripcion());
        transaccion.setFecha(LocalDateTime.now());
        transaccion.setEstado("COMPLETADA");

        Transaccion transaccionGuardada = transaccionRepository.save(transaccion);

        return convertirAResponse(transaccionGuardada);
    }

    public List<TransaccionResponseDTO> obtenerTransaccionesDelUsuario(String usuarioId) {
        List<Transaccion> transacciones = transaccionRepository.findByEmisorIdOrReceptorId(usuarioId, usuarioId);
        return transacciones.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    public List<TransaccionResponseDTO> obtenerTransaccionesEnviadas(String usuarioId) {
        List<Transaccion> transacciones = transaccionRepository.findByEmisorId(usuarioId);
        return transacciones.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    public List<TransaccionResponseDTO> obtenerTransaccionesRecibidas(String usuarioId) {
        List<Transaccion> transacciones = transaccionRepository.findByReceptorId(usuarioId);
        return transacciones.stream()
                .map(this::convertirAResponse)
                .collect(Collectors.toList());
    }

    public TransaccionResponseDTO convertirAResponse(Transaccion transaccion) {
        return new TransaccionResponseDTO(
                transaccion.getId(),
                transaccion.getEmisorId(),
                transaccion.getReceptorId(),
                transaccion.getMonto(),
                transaccion.getDescripcion(),
                transaccion.getFecha(),
                transaccion.getEstado()
        );
    }
}