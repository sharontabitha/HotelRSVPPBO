# StayEase Hotel Reservation System

StayEase is a Java Swing hotel-management desktop application. The admin workspace uses FlatLaf and a responsive navy sidebar, page topbar, real-data dashboard, and room-management screens. MySQL is the runtime database; the previous SQLite database is retained as a one-time import source.

## Technology

- Java 17+
- Java Swing with FlatLaf
- Maven
- MySQL 8+ through JDBC

## Implemented admin modules

- **Dashboard:** live room and reservation totals, availability for today, occupancy, and recent reservations.
- **Data Kamar:** search and filter by room category/status; create, update, delete, and upload room photos.
- **Reservasi:** existing reservation create, edit, status, and delete workflows.
- **Pengaturan Beranda:** retained existing homepage-content controls.
- **Data Tamu, Pembayaran, and Laporan:** shown as disabled navigation until their persistence and business rules are implemented. The application does not fabricate data for these modules.

The guest login, registration, booking, and reservation features are retained. The app uses the existing model, controller, service, and repository layers. SQLite remains available for legacy-data import and the existing regression smoke test.

## Configure MySQL

1. Start a local MySQL server and create the application database:

   ```sql
   CREATE DATABASE stayease CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Create a `.env` file in the project root (copy `.env.example`) and set your local connection:

   ```env
   STAYEASE_DB_URL=jdbc:mysql://localhost:3306/stayease?serverTimezone=UTC
   STAYEASE_DB_USER=root
   STAYEASE_DB_PASSWORD=
   ```

   The blank password is valid for a local Laragon installation whose MySQL `root` account has no password. If you have configured a password, put it after `=`. `.env` is read from the project working directory and is excluded from Git. Process environment variables take precedence. `STAYEASE_DB_URL` defaults to the local URL above; configure TLS in the URL when using a remote server.

3. On its first successful connection, StayEase creates its tables. If the MySQL rooms table is empty, the app imports the existing database at `%USERPROFILE%\.kompeni-inn\kompeni-inn.sqlite`; it records a migration marker so this import is not repeated. The SQLite database is read-only during import and is never deleted. If no SQLite database exists, the initial room inventory is created in MySQL.

   Required database privileges: connect to the `stayease` schema and create/read/write the StayEase tables.

## Build and run

Install Maven and JDK 17 or newer, configure the MySQL connection above, and run these commands from the project directory:

```powershell
mvn clean package
mvn exec:java -Dexec.mainClass=hotel.HotelApp
```

In Eclipse, import the project as an existing Maven project, run **Maven > Update Project**, then launch `hotel.HotelApp`.

When the database has no administrator, the first launch creates the `admin` account with a randomly generated password and prints it once to the application console. Save it securely and change it after signing in. To set the initial password yourself before the first launch, set `STAYEASE_ADMIN_INITIAL_PASSWORD` to a value of at least 12 characters. This setting is only used when creating the first admin and does not reset an existing account's password.

## Preview the GUI without MySQL

To open the desktop interface before configuring MySQL, run the app with the `--preview` argument:

```powershell
mvn exec:java -Dexec.mainClass=hotel.HotelApp -Dexec.args="--preview"
```

Preview mode uses a separate SQLite database at `%USERPROFILE%\.stayease-preview\stayease-preview.sqlite`. It does not connect to MySQL or modify the MySQL database or the legacy Kompeni inn SQLite database. The default launch command above continues to use MySQL.

## Regression test

The existing `hotel.HotelSmokeTest` runs against an isolated temporary SQLite database and covers authentication, role checks, room CRUD and photos, reservation lifecycle/date conflicts, homepage persistence, SQLite integrity, and legacy-file import. After Maven dependencies are resolved:

```powershell
mvn exec:java -Dexec.mainClass=hotel.HotelSmokeTest
```

The headless Swing smoke test checks FlatLaf loading, admin navigation, dashboard summary cards, and recent reservations without requiring a display:

```powershell
mvn exec:java -Dexec.mainClass=hotel.StayEaseUiSmokeTest
```

The MySQL repository uses the same service contract. A live MySQL integration run requires a configured MySQL server and is separate from the SQLite regression test.
