package ricardo.estudio.caribepay.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionResponseDTO {

    private String id;

    private String emisorId;

    private String receptorId;

    private Double monto;

    private String descripcion;

    private LocalDateTime fecha;

    private String estado;
}