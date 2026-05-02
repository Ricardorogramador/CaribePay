package ricardo.estudio.caribepay.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private String id;

    private String email;

    private Double saldo;

    private LocalDateTime fechaCreacion;
}
