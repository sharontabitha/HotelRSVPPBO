package hotel.model;

import java.math.BigDecimal;

public interface Billable {
    BigDecimal calculateTotal();
}
