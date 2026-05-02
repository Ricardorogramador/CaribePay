package ricardo.estudio.caribepay.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
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

    private String emisorId;

    private String receptorId;

    private Double monto;

    private String descripcion;

    private LocalDateTime fecha;

    private String estado;
}