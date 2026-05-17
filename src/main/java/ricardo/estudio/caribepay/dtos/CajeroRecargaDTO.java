package ricardo.estudio.caribepay.dtos;

public class CajeroRecargaDTO {

    private String telefono;
    private Double monto;
    private String email;
    private String descripcion;

    public CajeroRecargaDTO() {}

    public CajeroRecargaDTO(String telefono, Double monto, String email, String descripcion) {
        this.telefono = telefono;
        this.monto = monto;
        this.email = email;
        this.descripcion = descripcion;
    }

    public String getTelefono() { return telefono; }
    public Double getMonto() { return monto; }
    public String getEmail() { return email; }
    public String getDescripcion() { return descripcion; }

    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setMonto(Double monto) { this.monto = monto; }
    public void setEmail(String email) { this.email = email; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}