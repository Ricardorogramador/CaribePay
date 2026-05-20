package ricardo.estudio.caribepay.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
public class TransaccionDTO {

    @NotBlank(message = "El teléfono destino es obligatorio")
    private String telefonoDestino;

    @NotNull(message = "El monto es obligatorio")
    @Min(value = 1, message = "El monto debe ser mayor a 0")
    private Long monto;

    private String descripcion;

    public TransaccionDTO() {}

}