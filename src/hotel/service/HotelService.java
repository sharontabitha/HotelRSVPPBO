package hotel.service;

import hotel.data.HotelDataStore;
import hotel.model.Account;
import hotel.model.AdminAccount;
import hotel.model.DeluxeRoom;
import hotel.model.GuestAccount;
import hotel.model.HotelData;
import hotel.model.HotelException;
import hotel.model.HomeContent;
import hotel.model.Reservation;
import hotel.model.ReservationStatus;
import hotel.model.Room;
import hotel.model.RoomStatus;
import hotel.model.StandardRoom;
import hotel.model.SuiteRoom;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class HotelService {
    private final HotelDataStore repository;
    private final HotelData data;
    private final AuthenticationService authentication;

    public HotelService(HotelDataStore repository) throws IOException {
        this.repository = repository;
        this.data = repository.load();
        this.authentication = new AuthenticationService(repository, data);
        this.authentication.ensureAdministrator();
    }

    public Account login(String username, char[] password) throws HotelException {
        return authentication.login(username, password);
    }

    public GuestAccount register(String username, String name, String email,
                                 String phone, char[] password) throws HotelException {
        return authentication.register(username, name, email, phone, password);
    }

    public List<Room> getRooms() {
        return data.getRooms().stream().sorted(Comparator.comparing(Room::getNumber)).toList();
    }

    public Room findRoom(String number) throws HotelException {
        return data.getRooms().stream()
                .filter(room -> room.getNumber().equalsIgnoreCase(number))
                .findFirst().orElseThrow(() -> new HotelException("Kamar tidak ditemukan."));
    }

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, int guests) throws HotelException {
        validateDates(checkIn, checkOut);
        return data.getRooms().stream()
                .filter(room -> room.getStatus() == RoomStatus.AVAILABLE && room.getCapacity() >= guests)
                .filter(room -> data.getReservations().stream().noneMatch(reservation ->
                        reservation.getRoomNumber().equals(room.getNumber())
                                && reservation.getStatus() != ReservationStatus.CANCELLED
                                && reservation.overlaps(checkIn, checkOut)))
                .sorted(Comparator.comparing(Room::getNightlyRate))
                .toList();
    }

    public void addRoom(Room room) throws HotelException {
        if (room == null) throw new HotelException("Data kamar tidak valid.");
        if (data.getRooms().stream().anyMatch(existing ->
                existing.getNumber().equalsIgnoreCase(room.getNumber()))) {
            throw new HotelException("Nomor kamar tersebut sudah digunakan.");
        }
        data.getRooms().add(room);
        save();
    }

    public void updateRoom(String originalNumber, String category, String number, String floor,
                           String bed, int capacity, BigDecimal rate, String amenities,
                           RoomStatus status) throws HotelException {
        Room existing = findRoom(originalNumber);
        if (data.getRooms().stream().anyMatch(room -> room != existing
                && room.getNumber().equalsIgnoreCase(number))) {
            throw new HotelException("Nomor kamar tersebut sudah digunakan.");
        }
        if (data.getReservations().stream().anyMatch(reservation ->
                reservation.getRoomNumber().equalsIgnoreCase(originalNumber)
                        && reservation.getStatus() != ReservationStatus.CANCELLED
                        && (reservation.getStatus() == ReservationStatus.CHECKED_IN
                        || reservation.getCheckOut().isAfter(LocalDate.now())))) {
            throw new HotelException("Kamar memiliki reservasi aktif; data dasarnya tidak dapat diubah.");
        }
        Room replacement = createRoom(category, number, floor, bed, capacity, rate, amenities, status);
        replacement.setImageData(existing.getImageData());
        int index = data.getRooms().indexOf(existing);
        data.getRooms().set(index, replacement);
        if (!originalNumber.equalsIgnoreCase(number)) {
            data.getReservations().stream()
                    .filter(reservation -> reservation.getRoomNumber().equalsIgnoreCase(originalNumber))
                    .forEach(reservation -> reservation.updateDetails(reservation.getGuestUsername(),
                            reservation.getGuestName(), reservation.getPhone(), number,
                            reservation.getCheckIn(), reservation.getCheckOut(), reservation.getGuests(),
                            reservation.getNightlyRate(), reservation.getPaymentMethod(),
                            reservation.getNotes(), reservation.getStatus()));
        }
        save();
    }

    public void deleteRoom(String number) throws HotelException {
        Room room = findRoom(number);
        boolean hasHistory = data.getReservations().stream().anyMatch(reservation ->
                reservation.getRoomNumber().equalsIgnoreCase(number));
        if (hasHistory) throw new HotelException("Kamar memiliki riwayat reservasi dan tidak dapat dihapus.");
        data.getRooms().remove(room);
        save();
    }

    public void updateRoomImage(String number, byte[] imageData) throws HotelException {
        Room room = findRoom(number);
        byte[] previousImage = room.getImageData();
        try {
            room.setImageData(imageData);
        } catch (IllegalArgumentException exception) {
            throw new HotelException(exception.getMessage(), exception);
        }
        try {
            save();
        } catch (HotelException exception) {
            room.setImageData(previousImage);
            throw exception;
        }
    }

    public Reservation createReservation(String username, String guestName, String phone, String roomNumber,
                                         LocalDate checkIn, LocalDate checkOut, int guests,
                                         String paymentMethod, String notes) throws HotelException {
        Account account = data.getAccounts().stream()
                .filter(item -> item.getUsername().equalsIgnoreCase(username))
                .findFirst().orElseThrow(() -> new HotelException("Akun tamu tidak ditemukan."));
        if (account.getRole() != hotel.model.Role.GUEST) {
            throw new HotelException("Hanya akun tamu yang dapat memiliki reservasi.");
        }
        Room room = findRoom(roomNumber);
        ensureRoomAvailable(room, checkIn, checkOut, guests, null);
        Reservation reservation = new Reservation(account.getUsername(), guestName, phone, roomNumber,
                checkIn, checkOut, guests, room.getNightlyRate(), paymentMethod, notes,
                ReservationStatus.PENDING);
        data.getReservations().add(reservation);
        save();
        return reservation;
    }

    public Reservation createManualReservation(String guestName, String phone, String roomNumber,
                                               LocalDate checkIn, LocalDate checkOut, int guests,
                                               String paymentMethod, String notes) throws HotelException {
        Room room = findRoom(roomNumber);
        ensureRoomAvailable(room, checkIn, checkOut, guests, null);
        Reservation reservation = new Reservation("frontdesk", guestName, phone, roomNumber,
                checkIn, checkOut, guests, room.getNightlyRate(), paymentMethod, notes,
                ReservationStatus.CONFIRMED);
        data.getReservations().add(reservation);
        save();
        return reservation;
    }

    public List<Reservation> getReservations() {
        return data.getReservations().stream()
                .sorted(Comparator.comparing(Reservation::getCheckIn).reversed()).toList();
    }

    public List<Reservation> getReservationsFor(String username) {
        return getReservations().stream()
                .filter(reservation -> reservation.getGuestUsername().equalsIgnoreCase(username))
                .toList();
    }

    public void updateReservation(String id, String guestName, String phone, String roomNumber,
                                  LocalDate checkIn, LocalDate checkOut, int guests,
                                  String paymentMethod, String notes, ReservationStatus status)
            throws HotelException {
        Reservation reservation = findReservation(id);
        Room room = findRoom(roomNumber);
        ensureRoomAvailable(room, checkIn, checkOut, guests, id);
        reservation.updateDetails(reservation.getGuestUsername(), guestName, phone, roomNumber,
                checkIn, checkOut, guests, room.getNightlyRate(), paymentMethod, notes, status);
        save();
    }

    public void updateReservationStatus(String id, ReservationStatus status) throws HotelException {
        findReservation(id).setStatus(status);
        save();
    }

    public void cancelReservation(String id, String username, boolean administrator) throws HotelException {
        Reservation reservation = findReservation(id);
        if (!administrator && !reservation.getGuestUsername().equalsIgnoreCase(username)) {
            throw new HotelException("Anda tidak memiliki akses ke reservasi ini.");
        }
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN
                || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new HotelException("Reservasi yang sudah check-in atau check-out tidak dapat dibatalkan.");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        save();
    }

    public void deleteReservation(String id) throws HotelException {
        Reservation reservation = findReservation(id);
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            throw new HotelException("Reservasi yang sedang berlangsung tidak dapat dihapus.");
        }
        data.getReservations().remove(reservation);
        save();
    }

    public HomeContent getHomeContent() { return data.getHomeContent(); }

    public void updateHomeContent(String title, String welcome, String notice,
                                  boolean showRooms, boolean showAmenities, boolean showNotice)
            throws HotelException {
        data.setHomeContent(new HomeContent(title, welcome, notice, showRooms, showAmenities, showNotice));
        save();
    }

    public int getOccupancyCount() {
        return (int) data.getReservations().stream().filter(reservation ->
                reservation.getStatus() == ReservationStatus.CHECKED_IN
                        || (reservation.getStatus() != ReservationStatus.CANCELLED
                        && reservation.getCheckIn().equals(LocalDate.now()))).count();
    }

    public PathInfo getDataLocation() {
        return new PathInfo(repository.getLocation());
    }

    private Reservation findReservation(String id) throws HotelException {
        return data.getReservations().stream().filter(item -> item.getId().equals(id))
                .findFirst().orElseThrow(() -> new HotelException("Reservasi tidak ditemukan."));
    }

    private void ensureRoomAvailable(Room room, LocalDate checkIn, LocalDate checkOut, int guests,
                                     String exceptId) throws HotelException {
        validateDates(checkIn, checkOut);
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new HotelException("Kamar sedang tidak tersedia.");
        }
        if (guests < 1 || guests > room.getCapacity()) {
            throw new HotelException("Kapasitas kamar ini maksimal " + room.getCapacity() + " tamu.");
        }
        boolean occupied = data.getReservations().stream().anyMatch(reservation ->
                !Objects.equals(reservation.getId(), exceptId)
                        && reservation.getRoomNumber().equals(room.getNumber())
                        && reservation.getStatus() != ReservationStatus.CANCELLED
                        && reservation.overlaps(checkIn, checkOut));
        if (occupied) throw new HotelException("Kamar sudah dipesan pada rentang tanggal tersebut.");
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) throws HotelException {
        if (checkIn == null || checkOut == null || checkIn.isBefore(LocalDate.now())
                || !checkOut.isAfter(checkIn)) {
            throw new HotelException("Tanggal check-in harus hari ini atau sesudahnya, dan check-out harus setelah check-in.");
        }
    }

    private Room createRoom(String category, String number, String floor, String bed, int capacity,
                            BigDecimal rate, String amenities, RoomStatus status) throws HotelException {
        try {
            return switch (category) {
                case "Deluxe Heritage Room" -> new StandardRoom(number, floor, bed, capacity, rate, amenities, status);
                case "Grand Executive Suite" -> new DeluxeRoom(number, floor, bed, capacity, rate, amenities, status);
                case "Presidential Royal Penthouse" -> new SuiteRoom(number, floor, bed, capacity, rate, amenities, status);
                default -> throw new HotelException("Tipe kamar tidak dikenal.");
            };
        } catch (IllegalArgumentException exception) {
            throw new HotelException(exception.getMessage(), exception);
        }
    }

    private void save() throws HotelException {
        try {
            repository.save(data);
        } catch (IOException exception) {
            throw new HotelException("Data gagal disimpan: " + exception.getMessage(), exception);
        }
    }

    public record PathInfo(String location) {}
}
