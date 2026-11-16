package hotel.model;

public enum RoomStatus {
    AVAILABLE("Tersedia"),
    MAINTENANCE("Pemeliharaan");

    private final String label;

    RoomStatus(String label) { this.label = label; }

    @Override
    public String toString() { return label; }
}
