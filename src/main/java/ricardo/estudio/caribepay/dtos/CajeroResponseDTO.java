package ricardo.estudio.caribepay.dtos;

public class CajeroResponseDTO {

    private String id;
    private String email;
    private String telefono;
    private Double monto;
    private Double nuevoSaldo;
    private String mensaje;
    private Boolean exitoso;

    public CajeroResponseDTO() {}

    public CajeroResponseDTO(String id, String email, String telefono, Double monto,
                             Double nuevoSaldo, String mensaje, Boolean exitoso) {
        this.id = id;
        this.email = email;
        this.telefono = telefono;
        this.monto = monto;
        this.nuevoSaldo = nuevoSaldo;
        this.mensaje = mensaje;
        this.exitoso = exitoso;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public Double getMonto() { return monto; }
    public Double getNuevoSaldo() { return nuevoSaldo; }
    public String getMensaje() { return mensaje; }
    public Boolean getExitoso() { return exitoso; }

    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setMonto(Double monto) { this.monto = monto; }
    public void setNuevoSaldo(Double nuevoSaldo) { this.nuevoSaldo = nuevoSaldo; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public void setExitoso(Boolean exitoso) { this.exitoso = exitoso; }
}