package ricardo.estudio.caribepay.dtos;

import java.time.LocalDateTime;

public class UsuarioResponseDTO {

    private String id;
    private String email;
    private String telefono;
    private Double saldo;
    private LocalDateTime fechaCreacion;
    private String role;
    private Boolean activo;
    private LocalDateTime fechaDesactivacion;

    public UsuarioResponseDTO() {}

    public UsuarioResponseDTO(String id, String email, String telefono, Double saldo, LocalDateTime fechaCreacion) {
        this.id = id;
        this.email = email;
        this.telefono = telefono;
        this.saldo = saldo;
        this.fechaCreacion = fechaCreacion;
    }

    public UsuarioResponseDTO(String id, String email, String telefono, Double saldo, LocalDateTime fechaCreacion,
                              String role, Boolean activo, LocalDateTime fechaDesactivacion) {
        this.id = id;
        this.email = email;
        this.telefono = telefono;
        this.saldo = saldo;
        this.fechaCreacion = fechaCreacion;
        this.role = role;
        this.activo = activo;
        this.fechaDesactivacion = fechaDesactivacion;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public Double getSaldo() { return saldo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public String getRole() { return role; }
    public Boolean getActivo() { return activo; }
    public LocalDateTime getFechaDesactivacion() { return fechaDesactivacion; }

    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public void setRole(String role) { this.role = role; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public void setFechaDesactivacion(LocalDateTime fechaDesactivacion) { this.fechaDesactivacion = fechaDesactivacion; }
}