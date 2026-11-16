package hotel.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class HotelData implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ArrayList<Account> accounts = new ArrayList<>();
    private final ArrayList<Room> rooms = new ArrayList<>();
    private final ArrayList<Reservation> reservations = new ArrayList<>();
    private HomeContent homeContent = new HomeContent(
            "Liburan Berkesan Dimulai di StayEase",
            "Nikmati kamar nyaman, lokasi strategis, dan layanan hangat yang membuat setiap perjalanan terasa istimewa.",
            "Pemesanan kamar aman dan dapat dikelola langsung melalui akun Anda.",
            true, true, true);

    public static HotelData createInitial() {
        HotelData data = new HotelData();
        data.rooms.add(new StandardRoom("101", "Lantai 1", "King Size Royal", 2,
                new BigDecimal("2400000"), "Wi-Fi, sarapan, city view", RoomStatus.AVAILABLE));
        data.rooms.add(new StandardRoom("102", "Lantai 1", "Twin Hollywood", 2,
                new BigDecimal("2600000"), "Wi-Fi, sarapan, rain shower", RoomStatus.AVAILABLE));
        data.rooms.add(new DeluxeRoom("202", "Lantai 2", "King Size Royal", 3,
                new BigDecimal("3100000"), "Balkon, bathtub, sarapan", RoomStatus.AVAILABLE));
        data.rooms.add(new DeluxeRoom("303", "Lantai 3", "Super King Imperial", 3,
                new BigDecimal("4250000"), "City view, lounge, butler", RoomStatus.AVAILABLE));
        data.rooms.add(new SuiteRoom("403", "Lantai 4", "Super King Imperial", 6,
                new BigDecimal("7500000"), "Private balcony, wine cellar, butler 24/7", RoomStatus.AVAILABLE));
        data.setHomeContent(new HomeContent(
                "Liburan Berkesan Dimulai di StayEase",
                "Nikmati kamar nyaman, lokasi strategis, dan layanan hangat yang membuat setiap perjalanan terasa istimewa.",
                "Pesan langsung dengan tenang. Detail reservasi tersimpan aman di akun Anda.",
                true, true, true));
        return data;
    }

    public List<Account> getAccounts() { return accounts; }
    public List<Room> getRooms() { return rooms; }
    public List<Reservation> getReservations() { return reservations; }
    public HomeContent getHomeContent() { return homeContent; }
    public void setHomeContent(HomeContent content) {
        if (content == null) throw new IllegalArgumentException("Konten beranda tidak boleh kosong.");
        homeContent = content;
    }
}
