package hotel.model;

import java.math.BigDecimal;

public final class DeluxeRoom extends Room {
    private static final long serialVersionUID = 1L;

    public DeluxeRoom(String number, String floor, String bed, int capacity,
                      BigDecimal rate, String amenities, RoomStatus status) {
        super(number, floor, bed, capacity, rate, amenities, status);
    }

    @Override
    public String getCategory() { return "Grand Executive Suite"; }
    @Override
    protected BigDecimal rateMultiplier() { return new BigDecimal("1.15"); }
}
