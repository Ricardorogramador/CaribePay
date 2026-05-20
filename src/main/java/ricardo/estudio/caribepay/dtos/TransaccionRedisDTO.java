package ricardo.estudio.caribepay.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionRedisDTO {
    private String id;
    private String telefonoOrigen;
    private String telefonoDestino;
    private Long monto;
    private String estado;
    private LocalDateTime timestamp;
    private String descripcion;
}