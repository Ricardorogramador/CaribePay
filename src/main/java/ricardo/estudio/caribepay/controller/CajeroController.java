package ricardo.estudio.caribepay.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ricardo.estudio.caribepay.dtos.CajeroRecargaDTO;
import ricardo.estudio.caribepay.dtos.CajeroResponseDTO;
import ricardo.estudio.caribepay.models.Role;
import ricardo.estudio.caribepay.models.Transaccion;
import ricardo.estudio.caribepay.models.Usuario;
import ricardo.estudio.caribepay.repository.TransaccionRepository;
import ricardo.estudio.caribepay.services.PhoneService;
import ricardo.estudio.caribepay.services.SaldoService;
import ricardo.estudio.caribepay.services.UsuarioService;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/cajero")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class CajeroController {

    private final UsuarioService usuarioService;
    private final PhoneService phoneService;
    private final SaldoService saldoService;
    private final TransaccionRepository transaccionRepository;

    public CajeroController(UsuarioService usuarioService, PhoneService phoneService,
                            SaldoService saldoService, TransaccionRepository transaccionRepository) {
        this.usuarioService = usuarioService;
        this.phoneService = phoneService;
        this.saldoService = saldoService;
        this.transaccionRepository = transaccionRepository;
    }

    /**
     * Endpoint público para recargar dinero en el cajero automático
     * No requiere autenticación
     */
    @PostMapping("/recargar")
    public ResponseEntity<CajeroResponseDTO> recargar(@RequestBody CajeroRecargaDTO request) {
        log.info("========== RECARGA POR CAJERO ==========");
        log.info("Teléfono: {}, Monto: ${}", request.getTelefono(), request.getMonto());

        try {
            // 1) Validar datos básicos
            if (request.getTelefono() == null || request.getTelefono().isEmpty()) {
                log.warn("Recarga rechazada: teléfono vacío");
                throw new IllegalArgumentException("El teléfono es requerido");
            }

            if (request.getMonto() == null || request.getMonto() <= 0) {
                log.warn("Recarga rechazada: monto inválido");
                throw new IllegalArgumentException("El monto debe ser mayor a 0");
            }

            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                log.warn("Recarga rechazada: email vacío");
                throw new IllegalArgumentException("El email es requerido");
            }

            // 2) Normalizar teléfono
            String telefonoNormalizado = phoneService.normalizarCO(request.getTelefono());
            log.debug("Teléfono normalizado a: {}", telefonoNormalizado);

            // 3) Buscar usuario por teléfono
            Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorTelefonoNormalizado(telefonoNormalizado);
            if (usuarioOpt.isEmpty()) {
                log.warn("Recarga rechazada: usuario no encontrado con teléfono: {}", telefonoNormalizado);
                throw new IllegalArgumentException("No existe usuario con ese teléfono");
            }

            Usuario usuario = usuarioOpt.get();

            // 4) Validar que el usuario esté activo
            if (!usuario.getActivo()) {
                log.warn("Recarga rechazada: usuario desactivado: {}", usuario.getEmail());
                throw new IllegalArgumentException("Esta cuenta ha sido desactivada");
            }

            // 5) Incrementar saldo ATÓMICAMENTE
            double monto = request.getMonto();
            Usuario usuarioActualizado = saldoService.incrementarSaldo(usuario.getId(), monto);

            // 6) Registrar transacción de recarga
            Transaccion tx = new Transaccion();
            tx.setEmisorId(usuario.getId());
            tx.setReceptorId(usuario.getId());
            tx.setMonto(monto);
            tx.setDescripcion(request.getDescripcion() != null && !request.getDescripcion().isEmpty()
                    ? request.getDescripcion()
                    : "RECARGA de saldo");
            tx.setFecha(LocalDateTime.now());
            tx.setEstado("COMPLETADA");

            Transaccion transaccionGuardada = transaccionRepository.save(tx);

            log.info("✅ Recarga exitosa");
            log.info("   Usuario: {}", usuario.getEmail());
            log.info("   Monto: ${}", monto);
            log.info("   Nuevo saldo: ${}", usuarioActualizado.getSaldo());
            log.info("   ID Transacción: {}", transaccionGuardada.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new CajeroResponseDTO(
                            transaccionGuardada.getId(),
                            usuario.getEmail(),
                            telefonoNormalizado,
                            monto,
                            usuarioActualizado.getSaldo(),
                            "Recarga exitosa",
                            true
                    )
            );

        } catch (IllegalArgumentException e) {
            log.error("Error en recarga: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    new CajeroResponseDTO(null, null, null, null, null, e.getMessage(), false)
            );
        } catch (Exception e) {
            log.error("Error inesperado en recarga: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new CajeroResponseDTO(null, null, null, null, null, "Error interno del servidor", false)
            );
        }
    }
}