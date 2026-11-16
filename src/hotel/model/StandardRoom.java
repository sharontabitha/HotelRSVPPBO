package hotel.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class StandardRoom extends Room {
    private static final long serialVersionUID = 1L;

    public StandardRoom(String number, String floor, String bed, int capacity,
                        BigDecimal rate, String amenities, RoomStatus status) {
        super(number, floor, bed, capacity, rate, amenities, status);
    }

    @Override
    public String getCategory() { return "Deluxe Heritage Room"; }

    @Override
    protected BigDecimal rateMultiplier() { return BigDecimal.ONE; }

    @Override
    public BigDecimal hitungTarif() {
        // Fasilitas Standar: Wi-Fi, sarapan standar, city view (tarif reguler tanpa biaya ekstra)
        return getBaseRate().multiply(rateMultiplier()).setScale(0, RoundingMode.HALF_UP);
    }
}