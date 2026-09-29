package tacos;

//TC-17 - Nivel de picante con severidad explicita
public enum SpiceLevel {
    NONE(0),
    MILD(1),
    MEDIUM(2),
    HOT(3);

    private final int severity;

    SpiceLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }
}
//Fin TC-17
