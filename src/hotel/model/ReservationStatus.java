package hotel.model;

public enum ReservationStatus {
    PENDING("Menunggu"),
    CONFIRMED("Dikonfirmasi"),
    CHECKED_IN("Checked-In"),
    CHECKED_OUT("Checked-Out"),
    CANCELLED("Dibatalkan");

    private final String label;

    ReservationStatus(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
