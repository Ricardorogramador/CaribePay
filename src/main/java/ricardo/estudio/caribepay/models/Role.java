package ricardo.estudio.caribepay.models;

public enum Role {
    ADMIN("ADMIN"),
    USUARIO("USUARIO");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}