package ricardo.estudio.caribepay.dtos;

import java.time.LocalDateTime;

public class TransaccionResponseDTO {

    private String id;
    private String emisorId;
    private String receptorId;

    private String telefonoEmisor;
    private String telefonoReceptor;

    private Double monto;
    private String descripcion;
    private LocalDateTime fecha;
    private String estado;

    public TransaccionResponseDTO() {}

    public TransaccionResponseDTO(
            String id,
            String emisorId,
            String receptorId,
            String telefonoEmisor,
            String telefonoReceptor,
            Double monto,
            String descripcion,
            LocalDateTime fecha,
            String estado
    ) {
        this.id = id;
        this.emisorId = emisorId;
        this.receptorId = receptorId;
        this.telefonoEmisor = telefonoEmisor;
        this.telefonoReceptor = telefonoReceptor;
        this.monto = monto;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.estado = estado;
    }

    public String getId() { return id; }
    public String getEmisorId() { return emisorId; }
    public String getReceptorId() { return receptorId; }
    public String getTelefonoEmisor() { return telefonoEmisor; }
    public String getTelefonoReceptor() { return telefonoReceptor; }
    public Double getMonto() { return monto; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFecha() { return fecha; }
    public String getEstado() { return estado; }

    public void setId(String id) { this.id = id; }
    public void setEmisorId(String emisorId) { this.emisorId = emisorId; }
    public void setReceptorId(String receptorId) { this.receptorId = receptorId; }
    public void setTelefonoEmisor(String telefonoEmisor) { this.telefonoEmisor = telefonoEmisor; }
    public void setTelefonoReceptor(String telefonoReceptor) { this.telefonoReceptor = telefonoReceptor; }
    public void setMonto(Double monto) { this.monto = monto; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public void setEstado(String estado) { this.estado = estado; }
}