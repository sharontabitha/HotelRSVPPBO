package hotel.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class Reservation implements Serializable, Billable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private String guestUsername;
    private String guestName;
    private String phone;
    private String roomNumber;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private BigDecimal nightlyRate;
    private String paymentMethod;
    private String notes;
    private ReservationStatus status;

    public Reservation(String guestUsername, String guestName, String phone, String roomNumber,
                       LocalDate checkIn, LocalDate checkOut, int guests, BigDecimal nightlyRate,
                       String paymentMethod, String notes, ReservationStatus status) {
        this(UUID.randomUUID().toString().substring(0, 8).toUpperCase(), guestUsername, guestName,
                phone, roomNumber, checkIn, checkOut, guests, nightlyRate, paymentMethod, notes, status);
    }

    public static Reservation restore(String id, String guestUsername, String guestName, String phone,
                                      String roomNumber, LocalDate checkIn, LocalDate checkOut,
                                      int guests, BigDecimal nightlyRate, String paymentMethod,
                                      String notes, ReservationStatus status) {
        return new Reservation(id, guestUsername, guestName, phone, roomNumber, checkIn, checkOut,
                guests, nightlyRate, paymentMethod, notes, status);
    }

    private Reservation(String id, String guestUsername, String guestName, String phone,
                        String roomNumber, LocalDate checkIn, LocalDate checkOut, int guests,
                        BigDecimal nightlyRate, String paymentMethod, String notes,
                        ReservationStatus status) {
        this.id = required(id, "ID reservasi");
        updateDetails(guestUsername, guestName, phone, roomNumber, checkIn, checkOut, guests,
                nightlyRate, paymentMethod, notes, status);
    }

    public void updateDetails(String guestUsername, String guestName, String phone, String roomNumber,
                              LocalDate checkIn, LocalDate checkOut, int guests, BigDecimal nightlyRate,
                              String paymentMethod, String notes, ReservationStatus status) {
        this.guestUsername = required(guestUsername, "Akun tamu");
        this.guestName = required(guestName, "Nama tamu");
        this.phone = required(phone, "Nomor telepon");
        this.roomNumber = required(roomNumber, "Nomor kamar").toUpperCase();
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Tanggal check-out harus setelah tanggal check-in.");
        }
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        if (guests < 1 || guests > 12) throw new IllegalArgumentException("Jumlah tamu harus antara 1–12.");
        this.guests = guests;
        if (nightlyRate == null || nightlyRate.signum() <= 0) {
            throw new IllegalArgumentException("Tarif kamar tidak valid.");
        }
        this.nightlyRate = nightlyRate;
        this.paymentMethod = required(paymentMethod, "Metode pembayaran");
        this.notes = notes == null ? "" : notes.trim();
        if (status == null) throw new IllegalArgumentException("Status reservasi wajib dipilih.");
        this.status = status;
    }

    @Override
    public BigDecimal calculateTotal() {
        return calculateTotal((int) ChronoUnit.DAYS.between(checkIn, checkOut));
    }

    public BigDecimal calculateTotal(int nights) {
        if (nights < 1) throw new IllegalArgumentException("Jumlah malam minimal satu.");
        return nightlyRate.multiply(BigDecimal.valueOf(nights));
    }

    public BigDecimal calculateTotal(int nights, BigDecimal discountPercent) {
        if (discountPercent == null || discountPercent.signum() < 0
                || discountPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Diskon harus antara 0–100 persen.");
        }
        return calculateTotal(nights)
                .multiply(BigDecimal.ONE.subtract(discountPercent.movePointLeft(2)))
                .setScale(0, RoundingMode.HALF_UP);
    }

    public boolean overlaps(LocalDate from, LocalDate until) {
        return checkIn.isBefore(until) && from.isBefore(checkOut);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " wajib diisi.");
        return value.trim();
    }

    public String getId() { return id; }
    public String getGuestUsername() { return guestUsername; }
    public String getGuestName() { return guestName; }
    public String getPhone() { return phone; }
    public String getRoomNumber() { return roomNumber; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public BigDecimal getNightlyRate() { return nightlyRate; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getNotes() { return notes; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) {
        if (status == null) throw new IllegalArgumentException("Status reservasi wajib dipilih.");
        this.status = status;
    }
}
