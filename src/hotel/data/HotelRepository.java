package hotel.data;

import hotel.model.Account;
import hotel.model.AdminAccount;
import hotel.model.DeluxeRoom;
import hotel.model.GuestAccount;
import hotel.model.HotelData;
import hotel.model.HomeContent;
import hotel.model.Role;
import hotel.model.Reservation;
import hotel.model.ReservationStatus;
import hotel.model.Room;
import hotel.model.RoomStatus;
import hotel.model.StandardRoom;
import hotel.model.SuiteRoom;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public final class HotelRepository implements HotelDataStore {
    private static final String DRIVER = "org.sqlite.JDBC";
    private static final int SCHEMA_VERSION = 1;
    private final Path file;
    private final Path legacyFile;

    public HotelRepository(Path file) {
        this(file, file.toAbsolutePath().normalize().getParent().resolve("hotel.dat"));
    }

    public HotelRepository(Path file, Path legacyFile) {
        this.file = file.toAbsolutePath().normalize();
        this.legacyFile = legacyFile.toAbsolutePath().normalize();
    }

    public HotelData load() throws IOException {
        Path parent = file.getParent();
        try {
            if (parent != null) Files.createDirectories(parent);
            loadDriver();
            try (Connection connection = connect()) {
                initializeSchema(connection);
                HotelData data;
                if (tableIsEmpty(connection, "rooms")) {
                    data = migrateLegacyFile();
                    if (data == null) data = HotelData.createInitial();
                    String title = data.getHomeContent().getHeroTitle();
                    if (title.contains("Grand Royale")) {
                        HomeContent old = data.getHomeContent();
                        data.setHomeContent(new HomeContent(title.replace("Grand Royale", "Kompeni inn"),
                                old.getWelcomeMessage().replace("Grand Royale", "Kompeni inn"),
                                old.getBookingNotice(), old.isShowRoomCatalog(),
                                old.isShowAmenities(), old.isShowBookingNotice()));
                    }
                    for (int i = 0; i < data.getAccounts().size(); i++) {
                        Account account = data.getAccounts().get(i);
                        if (account.getRole() == Role.ADMIN
                                && account.getFullName().contains("Grand Royale")) {
                            data.getAccounts().set(i, new AdminAccount(account.getUsername(),
                                    account.getFullName().replace("Grand Royale", "Kompeni inn"),
                                    account.getEmail(), account.getPhone(),
                                    account.getPasswordSalt(), account.getPasswordHash()));
                        }
                    }
                    save(connection, data);
                } else {
                    data = readData(connection);
                }
                return data;
            }
        } catch (SQLException | ClassNotFoundException exception) {
            throw new IOException("Tidak dapat membuka basis data hotel: " + exception.getMessage(), exception);
        }
    }

    public void save(HotelData data) throws IOException {
        Path parent = file.getParent();
        try {
            if (parent != null) Files.createDirectories(parent);
            loadDriver();
            try (Connection connection = connect()) {
                initializeSchema(connection);
                save(connection, data);
            }
        } catch (SQLException | ClassNotFoundException exception) {
            throw new IOException("Tidak dapat menyimpan data ke basis data hotel: "
                    + exception.getMessage(), exception);
        }
    }

    public Path getFile() { return file; }

    @Override
    public String getLocation() { return file.toString(); }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = FULL");
        }
        return connection;
    }

    private static void loadDriver() throws ClassNotFoundException {
        Class.forName(DRIVER);
    }

    private void initializeSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet version = statement.executeQuery("PRAGMA user_version")) {
                if (version.next() && version.getInt(1) > SCHEMA_VERSION) {
                    throw new SQLException("Versi basis data lebih baru daripada versi aplikasi ini.");
                }
            }
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        username TEXT PRIMARY KEY COLLATE NOCASE,
                        role TEXT NOT NULL CHECK (role IN ('ADMIN', 'GUEST')),
                        full_name TEXT NOT NULL,
                        email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                        phone TEXT NOT NULL,
                        password_salt BLOB NOT NULL,
                        password_hash BLOB NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        room_number TEXT PRIMARY KEY COLLATE NOCASE,
                        category TEXT NOT NULL,
                        floor TEXT NOT NULL,
                        bed_type TEXT NOT NULL,
                        capacity INTEGER NOT NULL CHECK (capacity BETWEEN 1 AND 12),
                        base_rate TEXT NOT NULL,
                        amenities TEXT NOT NULL,
                        status TEXT NOT NULL CHECK (status IN ('AVAILABLE', 'MAINTENANCE')),
                        image_data BLOB
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id TEXT PRIMARY KEY,
                        guest_username TEXT NOT NULL,
                        guest_name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        room_number TEXT NOT NULL COLLATE NOCASE,
                        check_in TEXT NOT NULL,
                        check_out TEXT NOT NULL,
                        guests INTEGER NOT NULL CHECK (guests BETWEEN 1 AND 12),
                        nightly_rate TEXT NOT NULL,
                        payment_method TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        status TEXT NOT NULL CHECK (
                            status IN ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED')
                        ),
                        FOREIGN KEY(room_number) REFERENCES rooms(room_number)
                            ON UPDATE CASCADE ON DELETE RESTRICT,
                        CHECK (check_out > check_in)
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_reservations_room_dates
                    ON reservations(room_number, check_in, check_out, status)
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_reservations_guest
                    ON reservations(guest_username, check_in)
                    """);
            statement.execute("""
                    CREATE TRIGGER IF NOT EXISTS reservations_no_overlap_insert
                    BEFORE INSERT ON reservations
                    WHEN NEW.status <> 'CANCELLED' AND EXISTS (
                        SELECT 1 FROM reservations existing
                        WHERE existing.room_number = NEW.room_number COLLATE NOCASE
                          AND existing.status <> 'CANCELLED'
                          AND NEW.check_in < existing.check_out
                          AND existing.check_in < NEW.check_out
                    )
                    BEGIN
                        SELECT RAISE(ABORT, 'Kamar sudah dipesan pada rentang tanggal tersebut.');
                    END
                    """);
            statement.execute("""
                    CREATE TRIGGER IF NOT EXISTS reservations_no_overlap_update
                    BEFORE UPDATE ON reservations
                    WHEN NEW.status <> 'CANCELLED' AND EXISTS (
                        SELECT 1 FROM reservations existing
                        WHERE existing.id <> NEW.id
                          AND existing.room_number = NEW.room_number COLLATE NOCASE
                          AND existing.status <> 'CANCELLED'
                          AND NEW.check_in < existing.check_out
                          AND existing.check_in < NEW.check_out
                    )
                    BEGIN
                        SELECT RAISE(ABORT, 'Kamar sudah dipesan pada rentang tanggal tersebut.');
                    END
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS home_content (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        hero_title TEXT NOT NULL,
                        welcome_message TEXT NOT NULL,
                        booking_notice TEXT NOT NULL,
                        show_room_catalog INTEGER NOT NULL,
                        show_amenities INTEGER NOT NULL,
                        show_booking_notice INTEGER NOT NULL
                    )
                    """);
            statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
        }
    }

    private HotelData readData(Connection connection) throws SQLException, IOException {
        HotelData data = new HotelData();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT username, role, full_name, email, phone, password_salt, password_hash FROM accounts")) {
            while (rows.next()) {
                String username = rows.getString("username");
                String name = rows.getString("full_name");
                String email = rows.getString("email");
                String phone = rows.getString("phone");
                byte[] salt = rows.getBytes("password_salt");
                byte[] hash = rows.getBytes("password_hash");
                String role = rows.getString("role");
                Account account = "ADMIN".equals(role)
                        ? new AdminAccount(username, name, email, phone, salt, hash)
                        : new GuestAccount(username, name, email, phone, salt, hash);
                data.getAccounts().add(account);
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT room_number, category, floor, bed_type, capacity, base_rate,
                            amenities, status, image_data FROM rooms ORDER BY room_number
                     """)) {
            while (rows.next()) {
                Room room = createRoom(rows.getString("category"), rows.getString("room_number"),
                        rows.getString("floor"), rows.getString("bed_type"), rows.getInt("capacity"),
                        new BigDecimal(rows.getString("base_rate")), rows.getString("amenities"),
                        RoomStatus.valueOf(rows.getString("status")));
                room.setImageData(rows.getBytes("image_data"));
                data.getRooms().add(room);
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT id, guest_username, guest_name, phone, room_number, check_in, check_out,
                            guests, nightly_rate, payment_method, notes, status
                     FROM reservations ORDER BY check_in DESC
                     """)) {
            while (rows.next()) {
                data.getReservations().add(Reservation.restore(rows.getString("id"),
                        rows.getString("guest_username"), rows.getString("guest_name"),
                        rows.getString("phone"), rows.getString("room_number"),
                        LocalDate.parse(rows.getString("check_in")),
                        LocalDate.parse(rows.getString("check_out")), rows.getInt("guests"),
                        new BigDecimal(rows.getString("nightly_rate")),
                        rows.getString("payment_method"), rows.getString("notes"),
                        ReservationStatus.valueOf(rows.getString("status"))));
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("""
                     SELECT hero_title, welcome_message, booking_notice, show_room_catalog,
                            show_amenities, show_booking_notice FROM home_content WHERE id = 1
                     """)) {
            if (row.next()) {
                data.setHomeContent(new HomeContent(row.getString("hero_title"),
                        row.getString("welcome_message"), row.getString("booking_notice"),
                        row.getInt("show_room_catalog") != 0, row.getInt("show_amenities") != 0,
                        row.getInt("show_booking_notice") != 0));
            }
        }
        return data;
    }

    private void save(Connection connection, HotelData data) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            saveAccounts(connection, data);
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM reservations");
            }
            saveRooms(connection, data);
            saveReservations(connection, data);
            saveHomeContent(connection, data.getHomeContent());
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private void saveAccounts(Connection connection, HotelData data) throws SQLException {
        String sql = """
                INSERT INTO accounts(username, role, full_name, email, phone, password_salt, password_hash)
                VALUES(?,?,?,?,?,?,?)
                ON CONFLICT(username) DO UPDATE SET role=excluded.role, full_name=excluded.full_name,
                    email=excluded.email, phone=excluded.phone, password_salt=excluded.password_salt,
                    password_hash=excluded.password_hash
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Account account : data.getAccounts()) {
                statement.setString(1, account.getUsername());
                statement.setString(2, account.getRole().name());
                statement.setString(3, account.getFullName());
                statement.setString(4, account.getEmail());
                statement.setString(5, account.getPhone());
                statement.setBytes(6, account.getPasswordSalt());
                statement.setBytes(7, account.getPasswordHash());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void saveRooms(Connection connection, HotelData data) throws SQLException {
        String sql = """
                INSERT INTO rooms(room_number, category, floor, bed_type, capacity, base_rate,
                                  amenities, status, image_data)
                VALUES(?,?,?,?,?,?,?,?,?)
                ON CONFLICT(room_number) DO UPDATE SET category=excluded.category, floor=excluded.floor,
                    bed_type=excluded.bed_type, capacity=excluded.capacity, base_rate=excluded.base_rate,
                    amenities=excluded.amenities, status=excluded.status, image_data=excluded.image_data
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Room room : data.getRooms()) {
                statement.setString(1, room.getNumber());
                statement.setString(2, room.getCategory());
                statement.setString(3, room.getFloor());
                statement.setString(4, room.getBedType());
                statement.setInt(5, room.getCapacity());
                statement.setString(6, room.getBaseRate().toPlainString());
                statement.setString(7, room.getAmenities());
                statement.setString(8, room.getStatus().name());
                statement.setBytes(9, room.getImageData());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        if (data.getRooms().isEmpty()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM rooms");
            }
        } else {
            String placeholders = String.join(",", java.util.Collections.nCopies(data.getRooms().size(), "?"));
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM rooms WHERE room_number NOT IN (" + placeholders + ")")) {
                for (int i = 0; i < data.getRooms().size(); i++) {
                    statement.setString(i + 1, data.getRooms().get(i).getNumber());
                }
                statement.executeUpdate();
            }
        }
    }

    private void saveReservations(Connection connection, HotelData data) throws SQLException {
        String sql = """
                INSERT INTO reservations(id, guest_username, guest_name, phone, room_number, check_in,
                    check_out, guests, nightly_rate, payment_method, notes, status)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Reservation reservation : data.getReservations()) {
                statement.setString(1, reservation.getId());
                statement.setString(2, reservation.getGuestUsername());
                statement.setString(3, reservation.getGuestName());
                statement.setString(4, reservation.getPhone());
                statement.setString(5, reservation.getRoomNumber());
                statement.setString(6, reservation.getCheckIn().toString());
                statement.setString(7, reservation.getCheckOut().toString());
                statement.setInt(8, reservation.getGuests());
                statement.setString(9, reservation.getNightlyRate().toPlainString());
                statement.setString(10, reservation.getPaymentMethod());
                statement.setString(11, reservation.getNotes());
                statement.setString(12, reservation.getStatus().name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void saveHomeContent(Connection connection, HomeContent content) throws SQLException {
        String sql = """
                INSERT INTO home_content(id, hero_title, welcome_message, booking_notice,
                    show_room_catalog, show_amenities, show_booking_notice)
                VALUES(1,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET hero_title=excluded.hero_title,
                    welcome_message=excluded.welcome_message, booking_notice=excluded.booking_notice,
                    show_room_catalog=excluded.show_room_catalog, show_amenities=excluded.show_amenities,
                    show_booking_notice=excluded.show_booking_notice
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, content.getHeroTitle());
            statement.setString(2, content.getWelcomeMessage());
            statement.setString(3, content.getBookingNotice());
            statement.setInt(4, content.isShowRoomCatalog() ? 1 : 0);
            statement.setInt(5, content.isShowAmenities() ? 1 : 0);
            statement.setInt(6, content.isShowBookingNotice() ? 1 : 0);
            statement.executeUpdate();
        }
    }

    private HotelData migrateLegacyFile() throws IOException {
        if (!Files.isRegularFile(legacyFile) || legacyFile.equals(file)) return null;
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(legacyFile))) {
            input.setObjectInputFilter(HotelRepository::filterStoredData);
            Object value = input.readObject();
            if (value instanceof HotelData data) return data;
            throw new IOException("File hotel.dat lama bukan data Kompeni inn yang dikenal.");
        } catch (ClassNotFoundException | RuntimeException exception) {
            throw new IOException("Gagal memigrasikan hotel.dat lama: " + exception.getMessage(), exception);
        }
    }

    private static boolean tableIsEmpty(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT EXISTS(SELECT 1 FROM " + table + " LIMIT 1)")) {
            return !rows.next() || rows.getInt(1) == 0;
        }
    }

    private static Room createRoom(String category, String number, String floor, String bed, int capacity,
                                   BigDecimal rate, String amenities, RoomStatus status) throws IOException {
        try {
            return switch (category) {
                case "Deluxe Heritage Room" -> new StandardRoom(number, floor, bed, capacity, rate, amenities, status);
                case "Grand Executive Suite" -> new DeluxeRoom(number, floor, bed, capacity, rate, amenities, status);
                case "Presidential Royal Penthouse" -> new SuiteRoom(number, floor, bed, capacity, rate, amenities, status);
                default -> throw new IOException("Tipe kamar di database tidak dikenal: " + category);
            };
        } catch (IllegalArgumentException exception) {
            throw new IOException("Data kamar di database tidak valid.", exception);
        }
    }

    private static ObjectInputFilter.Status filterStoredData(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 30 || info.references() > 100_000 || info.streamBytes() > 20_000_000) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) return ObjectInputFilter.Status.UNDECIDED;
        while (type.isArray()) type = type.getComponentType();
        if (type.isPrimitive()) return ObjectInputFilter.Status.ALLOWED;
        String name = type.getName();
        if (name.startsWith("hotel.model.")
                || name.startsWith("java.lang.")
                || name.startsWith("java.util.")
                || name.startsWith("java.math.")
                || name.startsWith("java.time.")) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }
}
