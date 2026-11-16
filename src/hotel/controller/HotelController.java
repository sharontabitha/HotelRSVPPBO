package hotel.controller;

import hotel.model.Account;
import hotel.model.GuestAccount;
import hotel.model.HotelException;
import hotel.model.HomeContent;
import hotel.model.Reservation;
import hotel.model.ReservationStatus;
import hotel.model.Room;
import hotel.model.RoomStatus;
import hotel.service.HotelService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class HotelController {
    private final HotelService service;
    private Account signedInAccount;

    public HotelController(HotelService service) {
        this.service = service;
    }

    public Account login(String username, char[] password) throws HotelException {
        signedInAccount = null;
        signedInAccount = service.login(username, password);
        return signedInAccount;
    }

    public GuestAccount register(String username, String name, String email,
                                 String phone, char[] password) throws HotelException {
        return service.register(username, name, email, phone, password);
    }

    public void logout() { signedInAccount = null; }

    public List<Room> getRooms() { return service.getRooms(); }
    public Room findRoom(String number) throws HotelException { return service.findRoom(number); }

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, int guests)
            throws HotelException {
        return service.findAvailableRooms(checkIn, checkOut, guests);
    }

    public void addRoom(Room room) throws HotelException {
        requireAdmin();
        service.addRoom(room);
    }

    public void updateRoom(String originalNumber, String category, String number, String floor,
                           String bed, int capacity, BigDecimal rate, String amenities,
                           RoomStatus status) throws HotelException {
        requireAdmin();
        service.updateRoom(originalNumber, category, number, floor, bed, capacity, rate, amenities, status);
    }

    public void deleteRoom(String number) throws HotelException {
        requireAdmin();
        service.deleteRoom(number);
    }

    public void updateRoomImage(String number, byte[] imageData) throws HotelException {
        requireAdmin();
        service.updateRoomImage(number, imageData);
    }

    public Reservation createReservation(String username, String guestName, String phone, String roomNumber,
                                         LocalDate checkIn, LocalDate checkOut, int guests,
                                         String paymentMethod, String notes) throws HotelException {
        requireGuest(username);
        return service.createReservation(username, guestName, phone, roomNumber, checkIn, checkOut,
                guests, paymentMethod, notes);
    }

    public Reservation createManualReservation(String guestName, String phone, String roomNumber,
                                               LocalDate checkIn, LocalDate checkOut, int guests,
                                               String paymentMethod, String notes) throws HotelException {
        requireAdmin();
        return service.createManualReservation(guestName, phone, roomNumber, checkIn, checkOut,
                guests, paymentMethod, notes);
    }

    public List<Reservation> getReservations() {
        requireAdminForRead();
        return service.getReservations();
    }

    public List<Reservation> getReservationsFor(String username) {
        if (!(signedInAccount instanceof GuestAccount)
                || !signedInAccount.getUsername().equalsIgnoreCase(username)) {
            throw new IllegalStateException("Masuk menggunakan akun tamu untuk melihat reservasi.");
        }
        return service.getReservationsFor(username);
    }

    public void updateReservation(String id, String guestName, String phone, String roomNumber,
                                  LocalDate checkIn, LocalDate checkOut, int guests,
                                  String paymentMethod, String notes, ReservationStatus status)
            throws HotelException {
        requireAdmin();
        service.updateReservation(id, guestName, phone, roomNumber, checkIn, checkOut, guests,
                paymentMethod, notes, status);
    }

    public void updateReservationStatus(String id, ReservationStatus status) throws HotelException {
        requireAdmin();
        service.updateReservationStatus(id, status);
    }

    public void cancelReservation(String id, String username, boolean administrator) throws HotelException {
        if (administrator) requireAdmin();
        else requireGuest(username);
        service.cancelReservation(id, username, administrator);
    }

    public void deleteReservation(String id) throws HotelException {
        requireAdmin();
        service.deleteReservation(id);
    }

    public HomeContent getHomeContent() { return service.getHomeContent(); }

    public void updateHomeContent(String title, String welcome, String notice,
                                  boolean showRooms, boolean showAmenities, boolean showNotice)
            throws HotelException {
        requireAdmin();
        service.updateHomeContent(title, welcome, notice, showRooms, showAmenities, showNotice);
    }

    public int getOccupancyCount() {
        requireAdminForRead();
        return service.getOccupancyCount();
    }

    private void requireAdminForRead() {
        if (signedInAccount == null || signedInAccount.getRole() != hotel.model.Role.ADMIN) {
            throw new IllegalStateException("Data ini hanya tersedia untuk admin.");
        }
    }

    private void requireAdmin() throws HotelException {
        if (signedInAccount == null || signedInAccount.getRole() != hotel.model.Role.ADMIN) {
            throw new HotelException("Aksi ini hanya tersedia untuk admin.");
        }
    }

    private void requireGuest(String username) throws HotelException {
        if (!(signedInAccount instanceof GuestAccount)
                || !signedInAccount.getUsername().equalsIgnoreCase(username)) {
            throw new HotelException("Masuk menggunakan akun tamu untuk melanjutkan.");
        }
    }
}
