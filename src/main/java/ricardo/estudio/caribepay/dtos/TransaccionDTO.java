package ricardo.estudio.caribepay.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionDTO {

    @NotBlank(message = "El email del receptor no puede estar vacío")
    @Email(message = "El email debe ser válido")
    private String emailDestino;

    @NotNull(message = "El monto no puede estar vacío")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private Double monto;

    private String descripcion;
}