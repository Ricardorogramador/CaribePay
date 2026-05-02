package ricardo.estudio.caribepay.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TransaccionDTO {

    @NotBlank(message = "El teléfono destino es obligatorio")
    private String telefonoDestino;

    @NotNull(message = "El monto es obligatorio")
    @Min(value = 1, message = "El monto debe ser mayor a 0")
    private Double monto;

    private String descripcion;

    public TransaccionDTO() {}

    public TransaccionDTO(String telefonoDestino, Double monto, String descripcion) {
        this.telefonoDestino = telefonoDestino;
        this.monto = monto;
        this.descripcion = descripcion;
    }

    public String getTelefonoDestino() { return telefonoDestino; }
    public Double getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }

    public void setTelefonoDestino(String telefonoDestino) { this.telefonoDestino = telefonoDestino; }
    public void setMonto(Double monto) { this.monto = monto; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}