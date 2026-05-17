package ricardo.estudio.caribepay.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Document(collection = "transacciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaccion {

    @Id
    private String id;

    @Indexed
    private String emisorId;

    @Indexed
    private String receptorId;

    private String telefonoOrigen;

    private String telefonoDestino;

    private Double monto;

    private Long montoLong; // Para Redis

    private String descripcion;

    private LocalDateTime fecha;

    private LocalDateTime timestamp;

    private LocalDateTime timestampSync; // Cuándo se sincronizó desde Redis

    @Indexed
    private String estado; // COMPLETADA, FALLIDA, PENDIENTE_SYNC

    private String razon; // Si falló, por qué
}