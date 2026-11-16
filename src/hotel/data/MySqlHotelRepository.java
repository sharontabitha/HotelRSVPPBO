package hotel.data;

import hotel.model.Account;
import hotel.model.AdminAccount;
import hotel.model.DeluxeRoom;
import hotel.model.GuestAccount;
import hotel.model.HotelData;
import hotel.model.HomeContent;
import hotel.model.Reservation;
import hotel.model.ReservationStatus;
import hotel.model.Room;
import hotel.model.RoomStatus;
import hotel.model.StandardRoom;
import hotel.model.SuiteRoom;

import java.io.IOException;
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

public final class MySqlHotelRepository implements HotelDataStore {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/stayease?serverTimezone=UTC";
    private static final String IMPORT_MARKER = "initial-data-import-v1";

    private final String url;
    private final String username;
    private final String password;
    private final Path sqliteFile;

    public MySqlHotelRepository(String url, String username, String password, Path sqliteFile) {
        this.url = required(url, "STAYEASE_DB_URL");
        this.username = required(username, "STAYEASE_DB_USER");
        this.password = password == null ? "" : password;
        this.sqliteFile = sqliteFile.toAbsolutePath().normalize();
    }

    public static MySqlHotelRepository fromEnvironment(Path sqliteFile) {
        return new MySqlHotelRepository(
                environmentOrDefault("STAYEASE_DB_URL", DEFAULT_URL),
                environmentOrDefault("STAYEASE_DB_USER", "root"),
                environmentOrDefault("STAYEASE_DB_PASSWORD", ""),
                sqliteFile);
    }

    @Override
    public HotelData load() throws IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = connect()) {
                initializeSchema(connection);
                initializeData(connection);
                return readData(connection);
            }
        } catch (ClassNotFoundException exception) {
            throw new IOException("MySQL JDBC driver tidak tersedia. Jalankan aplikasi melalui Maven.", exception);
        } catch (SQLException exception) {
            throw new IOException("Tidak dapat membuka MySQL StayEase: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void save(HotelData data) throws IOException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = connect()) {
                initializeSchema(connection);
                save(connection, data);
            }
        } catch (ClassNotFoundException exception) {
            throw new IOException("MySQL JDBC driver tidak tersedia. Jalankan aplikasi melalui Maven.", exception);
        } catch (SQLException exception) {
            throw new IOException("Tidak dapat menyimpan data ke MySQL StayEase: "
                    + exception.getMessage(), exception);
        }
    }

    @Override
    public String getLocation() {
        return url;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private void initializeSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        username VARCHAR(30) PRIMARY KEY,
                        role VARCHAR(16) NOT NULL,
                        full_name VARCHAR(120) NOT NULL,
                        email VARCHAR(254) NOT NULL UNIQUE,
                        phone VARCHAR(40) NOT NULL,
                        password_salt VARBINARY(64) NOT NULL,
                        password_hash VARBINARY(128) NOT NULL,
                        CONSTRAINT chk_accounts_role CHECK (role IN ('ADMIN', 'GUEST'))
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        room_number VARCHAR(30) PRIMARY KEY,
                        category VARCHAR(100) NOT NULL,
                        floor VARCHAR(80) NOT NULL,
                        bed_type VARCHAR(100) NOT NULL,
                        capacity INT NOT NULL,
                        base_rate DECIMAL(19, 4) NOT NULL,
                        amenities TEXT NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        image_data LONGBLOB,
                        CONSTRAINT chk_rooms_capacity CHECK (capacity BETWEEN 1 AND 12),
                        CONSTRAINT chk_rooms_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE'))
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id VARCHAR(36) PRIMARY KEY,
                        guest_username VARCHAR(30) NOT NULL,
                        guest_name VARCHAR(120) NOT NULL,
                        phone VARCHAR(40) NOT NULL,
                        room_number VARCHAR(30) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INT NOT NULL,
                        nightly_rate DECIMAL(19, 4) NOT NULL,
                        payment_method VARCHAR(100) NOT NULL,
                        notes TEXT NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        CONSTRAINT chk_reservations_dates CHECK (check_out > check_in),
                        CONSTRAINT chk_reservations_guests CHECK (guests BETWEEN 1 AND 12),
                        CONSTRAINT chk_reservations_status CHECK (
                            status IN ('PENDING','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED')
                        ),
                        CONSTRAINT fk_reservations_room FOREIGN KEY (room_number)
                            REFERENCES rooms(room_number)
                            ON UPDATE CASCADE ON DELETE RESTRICT,
                        INDEX idx_reservations_room_dates (room_number, check_in, check_out, status),
                        INDEX idx_reservations_guest (guest_username, check_in)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS home_content (
                        id TINYINT PRIMARY KEY,
                        hero_title VARCHAR(200) NOT NULL,
                        welcome_message TEXT NOT NULL,
                        booking_notice TEXT NOT NULL,
                        show_room_catalog BOOLEAN NOT NULL,
                        show_amenities BOOLEAN NOT NULL,
                        show_booking_notice BOOLEAN NOT NULL
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS app_metadata (
                        metadata_key VARCHAR(100) PRIMARY KEY,
                        metadata_value VARCHAR(255) NOT NULL
                    ) ENGINE=InnoDB
                    """);
        }
    }

    private void initializeData(Connection connection) throws SQLException, IOException {
        if (metadataExists(connection)) return;
        HotelData initialData = null;
        if (roomTableIsEmpty(connection) && Files.isRegularFile(sqliteFile)) {
            initialData = new HotelRepository(sqliteFile).load();
            updateLegacyBranding(initialData);
        } else if (roomTableIsEmpty(connection)) {
            initialData = HotelData.createInitial();
        }
        if (initialData != null) save(connection, initialData);
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO app_metadata(metadata_key, metadata_value) VALUES(?, ?)")) {
            statement.setString(1, IMPORT_MARKER);
            statement.setString(2, initialData == null ? "existing-mysql-data" : "initialized");
            statement.executeUpdate();
        }
    }

    private static void updateLegacyBranding(HotelData data) {
        HomeContent old = data.getHomeContent();
        data.setHomeContent(new HomeContent(
                replaceBrand(old.getHeroTitle()), replaceBrand(old.getWelcomeMessage()),
                old.getBookingNotice(), old.isShowRoomCatalog(),
                old.isShowAmenities(), old.isShowBookingNotice()));
        for (int i = 0; i < data.getAccounts().size(); i++) {
            Account account = data.getAccounts().get(i);
            if (account.getRole() == hotel.model.Role.ADMIN
                    && (account.getFullName().contains("Kompeni inn")
                    || account.getFullName().contains("Grand Royale"))) {
                data.getAccounts().set(i, new AdminAccount(account.getUsername(),
                        replaceBrand(account.getFullName()), account.getEmail(), account.getPhone(),
                        account.getPasswordSalt(), account.getPasswordHash()));
            }
        }
    }

    private static String replaceBrand(String value) {
        return value.replace("Kompeni inn", "StayEase").replace("Grand Royale", "StayEase");
    }

    private static boolean metadataExists(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM app_metadata WHERE metadata_key = ?")) {
            statement.setString(1, IMPORT_MARKER);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private static boolean roomTableIsEmpty(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM rooms")) {
            return rows.next() && rows.getLong(1) == 0;
        }
    }

    private HotelData readData(Connection connection) throws SQLException, IOException {
        HotelData data = new HotelData();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT username, role, full_name, email, phone, password_salt, password_hash
                     FROM accounts
                     """)) {
            while (rows.next()) {
                String role = rows.getString("role");
                Account account = "ADMIN".equals(role)
                        ? new AdminAccount(rows.getString("username"), rows.getString("full_name"),
                                rows.getString("email"), rows.getString("phone"),
                                rows.getBytes("password_salt"), rows.getBytes("password_hash"))
                        : new GuestAccount(rows.getString("username"), rows.getString("full_name"),
                                rows.getString("email"), rows.getString("phone"),
                                rows.getBytes("password_salt"), rows.getBytes("password_hash"));
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
                        rows.getBigDecimal("base_rate"), rows.getString("amenities"),
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
                        rows.getDate("check_in").toLocalDate(), rows.getDate("check_out").toLocalDate(),
                        rows.getInt("guests"), rows.getBigDecimal("nightly_rate"),
                        rows.getString("payment_method"), rows.getString("notes"),
                        ReservationStatus.valueOf(rows.getString("status"))));
            }
        }
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("""
                     SELECT hero_title, welcome_message, booking_notice, show_room_catalog,
                            show_amenities, show_booking_notice FROM home_content WHERE id = 1
                     """)) {
            if (rows.next()) {
                data.setHomeContent(new HomeContent(rows.getString("hero_title"),
                        rows.getString("welcome_message"), rows.getString("booking_notice"),
                        rows.getBoolean("show_room_catalog"), rows.getBoolean("show_amenities"),
                        rows.getBoolean("show_booking_notice")));
            }
        }
        return data;
    }

    private static void save(Connection connection, HotelData data) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM reservations");
                statement.executeUpdate("DELETE FROM rooms");
                statement.executeUpdate("DELETE FROM accounts");
            }
            saveAccounts(connection, data);
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

    private static void saveAccounts(Connection connection, HotelData data) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO accounts(username, role, full_name, email, phone, password_salt, password_hash)
                VALUES(?,?,?,?,?,?,?)
                """)) {
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

    private static void saveRooms(Connection connection, HotelData data) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO rooms(room_number, category, floor, bed_type, capacity, base_rate,
                                  amenities, status, image_data)
                VALUES(?,?,?,?,?,?,?,?,?)
                """)) {
            for (Room room : data.getRooms()) {
                statement.setString(1, room.getNumber());
                statement.setString(2, room.getCategory());
                statement.setString(3, room.getFloor());
                statement.setString(4, room.getBedType());
                statement.setInt(5, room.getCapacity());
                statement.setBigDecimal(6, room.getBaseRate());
                statement.setString(7, room.getAmenities());
                statement.setString(8, room.getStatus().name());
                statement.setBytes(9, room.getImageData());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void saveReservations(Connection connection, HotelData data) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO reservations(id, guest_username, guest_name, phone, room_number, check_in,
                    check_out, guests, nightly_rate, payment_method, notes, status)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                """)) {
            for (Reservation reservation : data.getReservations()) {
                statement.setString(1, reservation.getId());
                statement.setString(2, reservation.getGuestUsername());
                statement.setString(3, reservation.getGuestName());
                statement.setString(4, reservation.getPhone());
                statement.setString(5, reservation.getRoomNumber());
                statement.setDate(6, java.sql.Date.valueOf(reservation.getCheckIn()));
                statement.setDate(7, java.sql.Date.valueOf(reservation.getCheckOut()));
                statement.setInt(8, reservation.getGuests());
                statement.setBigDecimal(9, reservation.getNightlyRate());
                statement.setString(10, reservation.getPaymentMethod());
                statement.setString(11, reservation.getNotes());
                statement.setString(12, reservation.getStatus().name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void saveHomeContent(Connection connection, HomeContent content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO home_content(id, hero_title, welcome_message, booking_notice,
                    show_room_catalog, show_amenities, show_booking_notice)
                VALUES(1,?,?,?,?,?,?)
                """)) {
            statement.setString(1, content.getHeroTitle());
            statement.setString(2, content.getWelcomeMessage());
            statement.setString(3, content.getBookingNotice());
            statement.setBoolean(4, content.isShowRoomCatalog());
            statement.setBoolean(5, content.isShowAmenities());
            statement.setBoolean(6, content.isShowBookingNotice());
            statement.executeUpdate();
        }
    }

    private static Room createRoom(String category, String number, String floor, String bed, int capacity,
                                   BigDecimal rate, String amenities, RoomStatus status) throws IOException {
        try {
            return switch (category) {
                case "Deluxe Heritage Room" -> new StandardRoom(number, floor, bed, capacity, rate, amenities, status);
                case "Grand Executive Suite" -> new DeluxeRoom(number, floor, bed, capacity, rate, amenities, status);
                case "Presidential Royal Penthouse" -> new SuiteRoom(number, floor, bed, capacity, rate, amenities, status);
                default -> throw new IOException("Tipe kamar MySQL tidak dikenal: " + category);
            };
        } catch (IllegalArgumentException exception) {
            throw new IOException("Data kamar MySQL tidak valid: " + exception.getMessage(), exception);
        }
    }

    private static String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Environment variable " + name + " wajib diatur.");
        }
        return value.trim();
    }
}
