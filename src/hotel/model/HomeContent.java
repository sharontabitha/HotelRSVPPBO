package hotel.model;

import java.io.Serializable;

public final class HomeContent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String heroTitle;
    private String welcomeMessage;
    private String bookingNotice;
    private boolean showRoomCatalog;
    private boolean showAmenities;
    private boolean showBookingNotice;

    public HomeContent(String heroTitle, String welcomeMessage, String bookingNotice,
                       boolean showRoomCatalog, boolean showAmenities, boolean showBookingNotice) {
        update(heroTitle, welcomeMessage, bookingNotice, showRoomCatalog, showAmenities, showBookingNotice);
    }

    public void update(String heroTitle, String welcomeMessage, String bookingNotice,
                       boolean showRoomCatalog, boolean showAmenities, boolean showBookingNotice) {
        this.heroTitle = required(heroTitle, "Judul beranda");
        this.welcomeMessage = required(welcomeMessage, "Deskripsi beranda");
        this.bookingNotice = bookingNotice == null ? "" : bookingNotice.trim();
        this.showRoomCatalog = showRoomCatalog;
        this.showAmenities = showAmenities;
        this.showBookingNotice = showBookingNotice;
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " wajib diisi.");
        return value.trim();
    }

    public String getHeroTitle() { return heroTitle; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public String getBookingNotice() { return bookingNotice; }
    public boolean isShowRoomCatalog() { return showRoomCatalog; }
    public boolean isShowAmenities() { return showAmenities; }
    public boolean isShowBookingNotice() { return showBookingNotice; }
}
