package hotel.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public abstract class Room implements Serializable, Billable {
    private static final long serialVersionUID = 1L;
    private String number;
    private String floor;
    private String bedType;
    private int capacity;
    private BigDecimal baseRate;
    private String amenities;
    private RoomStatus status;
    private byte[] imageData;

    protected Room(String number, String floor, String bedType, int capacity,
                   BigDecimal baseRate, String amenities, RoomStatus status) {
        setNumber(number);
        setFloor(floor);
        setBedType(bedType);
        setCapacity(capacity);
        setBaseRate(baseRate);
        setAmenities(amenities);
        setStatus(status);
    }

    public abstract String getCategory();
    protected abstract BigDecimal rateMultiplier();

    public BigDecimal getNightlyRate() {
        return baseRate.multiply(rateMultiplier()).setScale(0, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateTotal() { return getNightlyRate(); }

    public void updateDetails(String floor, String bedType, int capacity, BigDecimal baseRate,
                              String amenities, RoomStatus status) {
        setFloor(floor);
        setBedType(bedType);
        setCapacity(capacity);
        setBaseRate(baseRate);
        setAmenities(amenities);
        setStatus(status);
    }

    public String getNumber() { return number; }
    public String getFloor() { return floor; }
    public String getBedType() { return bedType; }
    public int getCapacity() { return capacity; }
    public BigDecimal getBaseRate() { return baseRate; }
    public String getAmenities() { return amenities; }
    public RoomStatus getStatus() { return status; }
    public byte[] getImageData() { return imageData == null ? null : Arrays.copyOf(imageData, imageData.length); }

    public void setImageData(byte[] value) {
        if (value != null && value.length > 8 * 1024 * 1024) {
            throw new IllegalArgumentException("Ukuran gambar kamar maksimal 8 MB.");
        }
        imageData = value == null ? null : Arrays.copyOf(value, value.length);
    }

    @Override
    public String toString() {
        return "Kamar " + number + " · " + getCategory() + " · " + getNightlyRate();
    }

    private void setNumber(String value) {
        if (value == null || !value.trim().matches("[A-Za-z0-9-]{2,12}")) {
            throw new IllegalArgumentException("Nomor kamar harus 2–12 karakter alfanumerik.");
        }
        number = value.trim().toUpperCase();
    }

    private void setFloor(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Lantai wajib diisi.");
        floor = value.trim();
    }

    private void setBedType(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Tipe ranjang wajib diisi.");
        bedType = value.trim();
    }

    private void setCapacity(int value) {
        if (value < 1 || value > 12) throw new IllegalArgumentException("Kapasitas harus antara 1–12 tamu.");
        capacity = value;
    }

    private void setBaseRate(BigDecimal value) {
        if (value == null || value.signum() <= 0) throw new IllegalArgumentException("Tarif harus lebih dari nol.");
        baseRate = value.setScale(0, RoundingMode.HALF_UP);
    }

    private void setAmenities(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Fasilitas wajib diisi.");
        amenities = value.trim();
    }

    private void setStatus(RoomStatus value) {
        if (value == null) throw new IllegalArgumentException("Status kamar wajib dipilih.");
        status = value;
    }
}
