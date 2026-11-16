package hotel.model;

import java.math.BigDecimal;

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
}
