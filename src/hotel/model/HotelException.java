package hotel.model;

public final class HotelException extends Exception {
    private static final long serialVersionUID = 1L;

    public HotelException(String message) { super(message); }
    public HotelException(String message, Throwable cause) { super(message, cause); }
}
