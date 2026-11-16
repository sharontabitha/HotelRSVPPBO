package hotel;

import hotel.controller.HotelController;
import hotel.data.HotelRepository;
import hotel.model.Account;
import hotel.model.HotelException;
import hotel.model.HotelData;
import hotel.model.HomeContent;
import hotel.model.Reservation;
import hotel.model.ReservationStatus;
import hotel.model.Room;
import hotel.model.RoomStatus;
import hotel.model.StandardRoom;
import hotel.model.DeluxeRoom;
import hotel.model.SuiteRoom;
import hotel.service.AuthenticationService;
import hotel.service.HotelService;

import java.math.BigDecimal;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.Arrays;

public final class HotelSmokeTest {
    public static void main(String[] args) throws Exception {
        System.setProperty("stayease.admin.initialPassword", "HotelSmokeTest-Admin-2026");
        Path folder = Files.createTempDirectory("kompeni-inn-test-");
        Path file = folder.resolve("kompeni-inn.sqlite");
        Path migrationFile = folder.resolve("migration.sqlite");
        Path legacyFile = folder.resolve("hotel.dat");
        try {
            HotelRepository repository = new HotelRepository(file);
            HotelService service = new HotelService(repository);
            LocalDate start = LocalDate.now().plusDays(5);
            LocalDate end = start.plusDays(2);

            check(service.getRooms().size() == 5, "Initial room inventory");
            Account admin = service.login(AuthenticationService.DEMO_ADMIN_USERNAME,
                    "HotelSmokeTest-Admin-2026".toCharArray());
            check(admin.getRole().name().equals("ADMIN"), "Admin role and initial login");

            // Uji Polimorfisme & Overloading hitungTarif() sesuai rubrik tema
            Room testStd = new StandardRoom("S01", "Lantai 1", "King", 2, new BigDecimal("1000000"), "Wi-Fi", RoomStatus.AVAILABLE);
            Room testDlx = new DeluxeRoom("D01", "Lantai 2", "King", 2, new BigDecimal("1000000"), "Bathtub", RoomStatus.AVAILABLE);
            Room testSte = new SuiteRoom("P01", "Lantai 3", "Imperial", 4, new BigDecimal("1000000"), "Butler", RoomStatus.AVAILABLE);
            check(testStd.hitungTarif().compareTo(new BigDecimal("1000000")) == 0, "Polimorfisme StandardRoom hitungTarif");
            check(testDlx.hitungTarif().compareTo(new BigDecimal("1150000")) == 0, "Polimorfisme DeluxeRoom hitungTarif (+15%)");
            check(testSte.hitungTarif().compareTo(new BigDecimal("1300000")) == 0, "Polimorfisme SuiteRoom hitungTarif (+30%)");
            check(testDlx.hitungTarif(3).compareTo(new BigDecimal("3450000")) == 0, "Overloading hitungTarif(jumlahMalam)");
            check(testDlx.hitungTarif(3, 10.0).compareTo(new BigDecimal("3105000")) == 0, "Overloading hitungTarif(jumlahMalam, diskonPersen)");
            service.register("demo.guest", "Demo Guest", "demo@example.test", "08123456789",
                    "guestPass123".toCharArray());
            check(service.login("DEMO.GUEST", "guestPass123".toCharArray()).getRole().name().equals("GUEST"),
                    "Case-insensitive guest login");

            Reservation first = service.createReservation("demo.guest", "Demo Guest", "08123456789",
                    "101", start, end, 2, "QRIS", "Smoke test");
            check(first.calculateTotal().equals(new BigDecimal("4800000")), "Two-night room total");
            check(service.findAvailableRooms(start, end, 1).stream()
                    .noneMatch(room -> room.getNumber().equals("101")), "Reserved room is not available");

            expectHotelException(() -> service.createReservation("demo.guest", "Demo Guest", "08123456789",
                    "101", start.plusDays(1), end.plusDays(1), 2, "QRIS", ""));

            Reservation nextStay = service.createReservation("demo.guest", "Demo Guest", "08123456789",
                    "101", end, end.plusDays(1), 2, "QRIS", "");
            service.cancelReservation(first.getId(), "demo.guest", false);
            check(service.findAvailableRooms(start, end, 1).stream()
                    .anyMatch(room -> room.getNumber().equals("101")), "Cancelled reservation releases room");
            service.updateReservationStatus(nextStay.getId(), ReservationStatus.CONFIRMED);
            service.updateReservation(nextStay.getId(), "Updated Guest", "08123456789", "101",
                    end, end.plusDays(1), 2, "QRIS", "Updated reservation",
                    ReservationStatus.CONFIRMED);
            check(service.getReservationsFor("demo.guest").stream()
                    .anyMatch(reservation -> reservation.getGuestName().equals("Updated Guest")),
                    "Update reservation");
            Reservation removable = service.createReservation("demo.guest", "Demo Guest", "08123456789",
                    "102", start, start.plusDays(1), 2, "QRIS", "");
            service.deleteReservation(removable.getId());
            check(service.getReservationsFor("demo.guest").size() == 2, "Guest reservation list");

            service.addRoom(new StandardRoom("901", "Lantai 9", "King Size Royal", 2,
                    new BigDecimal("1000000"), "Wi-Fi", RoomStatus.AVAILABLE));
            check(service.getRooms().size() == 6, "Create room");
            byte[] testImage = createTestImage();
            service.updateRoomImage("901", testImage);
            service.updateRoom("901", "Deluxe Heritage Room", "901", "Lantai 9",
                    "Twin Hollywood", 2, new BigDecimal("1100000"), "Wi-Fi, sarapan",
                    RoomStatus.AVAILABLE);
            check(service.findRoom("901").getBedType().equals("Twin Hollywood"), "Update room");
            check(Arrays.equals(service.findRoom("901").getImageData(), testImage),
                    "Room image preserved after room update");
            service.deleteRoom("901");
            check(service.getRooms().size() == 5, "Delete room");

            service.updateHomeContent("Judul Tes", "Deskripsi Tes", "Info Tes",
                    false, true, false);
            HomeContent content = service.getHomeContent();
            check(!content.isShowRoomCatalog() && content.isShowAmenities(), "Homepage visibility settings");

            service.updateRoomImage("101", testImage);
            HotelService reloaded = new HotelService(new HotelRepository(file));
            check(reloaded.getRooms().size() == 5, "Persistent room data");
            check(reloaded.getReservations().size() == 2, "Persistent reservations");
            check(reloaded.getHomeContent().getHeroTitle().equals("Judul Tes"), "Persistent homepage content");
            check(reloaded.login("demo.guest", "guestPass123".toCharArray()).getFullName()
                    .equals("Demo Guest"), "Persistent password hash and account");
            check(Arrays.equals(reloaded.findRoom("101").getImageData(), testImage),
                    "Room image BLOB persistence");
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                 var statement = connection.createStatement();
                 var rows = statement.executeQuery("""
                         SELECT COUNT(*) FROM sqlite_master WHERE type = 'table'
                           AND name IN ('accounts','rooms','reservations','home_content')
                         """)) {
                check(rows.next() && rows.getInt(1) == 4, "Normalized SQLite schema");
            }
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                 var statement = connection.createStatement();
                 var rows = statement.executeQuery("PRAGMA foreign_key_check")) {
                check(!rows.next(), "SQLite referential integrity");
            }
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
                 PreparedStatement statement = connection.prepareStatement("""
                         INSERT INTO reservations(id, guest_username, guest_name, phone, room_number,
                             check_in, check_out, guests, nightly_rate, payment_method, notes, status)
                         VALUES('DATABASE-CONFLICT','demo.guest','Demo Guest','08123456789',
                             '101',?,?,?,?,?,?,?)
                         """)) {
                statement.setString(1, start.plusDays(1).toString());
                statement.setString(2, end.plusDays(1).toString());
                statement.setInt(3, 2);
                statement.setString(4, "2400000");
                statement.setString(5, "QRIS");
                statement.setString(6, "");
                statement.setString(7, ReservationStatus.PENDING.name());
                try {
                    statement.executeUpdate();
                    throw new AssertionError("SQLite must reject overlapping room reservations");
                } catch (SQLException expected) {
                    check(expected.getMessage().contains("sudah dipesan"),
                            "SQLite overlap-protection trigger");
                }
            }

            HotelData oldData = HotelData.createInitial();
            oldData.setHomeContent(new HomeContent("Kenyamanan Grand Royale",
                    "Selamat datang di Grand Royale.", "Info reservasi", true, true, true));
            oldData.getAccounts().add(new hotel.model.AdminAccount("old-admin",
                    "Administrator Grand Royale", "admin@grandroyale.local", "080000000000",
                    new byte[16], new byte[32]));
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(legacyFile))) {
                output.writeObject(oldData);
            }
            HotelService migrated = new HotelService(new HotelRepository(migrationFile, legacyFile));
            check(migrated.getRooms().size() == 5, "Legacy hotel.dat migration");
            check(migrated.getHomeContent().getHeroTitle().contains("Kompeni inn"),
                    "Legacy hotel brand migration");
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + migrationFile);
                 var statement = connection.createStatement();
                 var rows = statement.executeQuery(
                         "SELECT full_name FROM accounts WHERE username='old-admin'")) {
                check(rows.next() && rows.getString(1).contains("Kompeni inn"),
                        "Legacy admin display name migration");
            }
            check(Files.size(migrationFile) > 0, "Migrated SQLite database created");

            HotelController controller = new HotelController(new HotelService(new HotelRepository(file)));
            expectHotelException(() -> controller.deleteRoom("102"));
            controller.login("demo.guest", "guestPass123".toCharArray());
            expectHotelException(() -> controller.updateHomeContent("Unauthorized", "Test", "Test",
                    true, true, true));
            try {
                controller.getReservations();
                throw new AssertionError("Guest must not read all reservations");
            } catch (IllegalStateException expected) {
                check(expected.getMessage().contains("admin"), "Role-protected reservation listing");
            }
            controller.logout();
            check(controller.login("admin", "HotelSmokeTest-Admin-2026".toCharArray())
                            .getRole().name().equals("ADMIN"),
                    "Controller admin session");
            check(controller.getReservations().size() == 2, "Admin reservation listing");
            System.out.println("PASS: registration, roles, password hashing, room CRUD and photos, "
                    + "date conflicts, reservation lifecycle, role authorization, homepage settings, "
                    + "SQLite integrity, and legacy migration.");
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(migrationFile);
            Files.deleteIfExists(legacyFile);
            Files.deleteIfExists(Path.of(file + "-shm"));
            Files.deleteIfExists(Path.of(file + "-wal"));
            Files.deleteIfExists(Path.of(migrationFile + "-shm"));
            Files.deleteIfExists(Path.of(migrationFile + "-wal"));
            Files.deleteIfExists(folder);
        }
    }

    private static byte[] createTestImage() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xD4AF37);
        image.setRGB(1, 0, 0x161C23);
        image.setRGB(0, 1, 0x161C23);
        image.setRGB(1, 1, 0xD4AF37);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    private static void expectHotelException(CheckedAction action) throws Exception {
        try {
            action.run();
        } catch (HotelException expected) {
            return;
        }
        throw new AssertionError("Expected HotelException for overlapping reservation");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
