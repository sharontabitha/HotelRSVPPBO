package hotel.model;

import java.math.BigDecimal;

public final class SuiteRoom extends Room {
    private static final long serialVersionUID = 1L;

    public SuiteRoom(String number, String floor, String bed, int capacity,
                     BigDecimal rate, String amenities, RoomStatus status) {
        super(number, floor, bed, capacity, rate, amenities, status);
    }

    @Override
    public String getCategory() { return "Presidential Royal Penthouse"; }
    @Override
    protected BigDecimal rateMultiplier() { return new BigDecimal("1.30"); }
}
