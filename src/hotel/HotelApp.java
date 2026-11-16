package hotel;

import hotel.model.Account;
import hotel.model.AdminAccount;
import hotel.model.DeluxeRoom;
import hotel.model.GuestAccount;
import hotel.model.HomeContent;
import hotel.model.Reservation;
import hotel.model.ReservationStatus;
import hotel.model.Room;
import hotel.model.RoomStatus;
import hotel.model.StandardRoom;
import hotel.model.SuiteRoom;
import hotel.controller.HotelController;
import hotel.service.HotelService;
import hotel.data.HotelDataStore;
import hotel.data.HotelRepository;
import hotel.data.MySqlHotelRepository;
import hotel.ui.AdminWorkspace;
import hotel.ui.DashboardPanel;
import hotel.ui.MainFrame;
import hotel.ui.StayEaseTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.nio.file.Files;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.awt.image.BufferedImage;

public final class HotelApp {
    private static final Color BG = StayEaseTheme.BACKGROUND;
    private static final Color PANEL = StayEaseTheme.SURFACE;
    private static final Color CARD = StayEaseTheme.SURFACE;
    private static final Color BLUE_ACCENT = StayEaseTheme.BLUE;
    private static final Color TEXT = StayEaseTheme.TEXT;
    private static final Color MUTED = StayEaseTheme.MUTED;
    private static final Color BORDER = StayEaseTheme.BORDER;
    private static final Color PALE_BLUE = StayEaseTheme.PALE_BLUE;
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));

    private final HotelController service;
    private final MainFrame frame = new MainFrame();
    private final CardLayout screenLayout = new CardLayout();
    private final FadingPanel screens = new FadingPanel(screenLayout);
    private Account currentAccount;
    private JTable roomsTable;
    private JTable reservationsTable;
    private JTextField roomSearch;
    private JComboBox<String> roomCategoryFilter;
    private JComboBox<String> roomStatusFilter;
    private JTabbedPane guestTabs;
    private JSpinner bookingCheckIn;
    private JSpinner bookingCheckOut;
    private JSpinner bookingGuests;
    private JComboBox<Room> bookingRoomChoice;
    private Timer screenTransition;

    private HotelApp(HotelController service) {
        this.service = service;
        frame.setContentPane(screens);
        showLogin();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                StayEaseTheme.install();
                boolean previewMode = args.length > 0 && "--preview".equals(args[0]);
                var userHome = java.nio.file.Path.of(System.getProperty("user.home"));
                HotelService hotelService;
                try {
                    HotelDataStore repository;
                    if (previewMode) {
                        var previewDatabase = userHome.resolve(".stayease-preview")
                                .resolve("stayease-preview.sqlite");
                        repository = new HotelRepository(previewDatabase);
                    } else {
                        var legacyDatabase = userHome.resolve(".kompeni-inn")
                                .resolve("kompeni-inn.sqlite");
                        repository = MySqlHotelRepository.fromEnvironment(legacyDatabase);
                    }
                    hotelService = new HotelService(repository);
                } catch (Exception ex) {
                    System.out.println("Catatan: MySQL belum aktif/diatur (" + ex.getMessage() + "). Menggunakan database SQLite lokal.");
                    var previewDatabase = userHome.resolve(".stayease-preview")
                            .resolve("stayease-preview.sqlite");
                    hotelService = new HotelService(new HotelRepository(previewDatabase));
                }
                HotelApp app = new HotelApp(new HotelController(hotelService));
                app.frame.setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,
                        "Aplikasi gagal dibuka:\n" + exception.getMessage(),
                        "Kesalahan", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void showLogin() {
        screens.removeAll();
        JPanel page = new JPanel(new BorderLayout(24, 24));
        page.setBackground(BG);
        page.setBorder(BorderFactory.createEmptyBorder(24, 36, 30, 36));
        page.add(brandHeader("HOTEL & SUITES"), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = constraints();
        c.gridx = 0;
        c.weightx = 0.58;
        c.fill = GridBagConstraints.BOTH;
        JPanel welcome = new JPanel();
        welcome.setOpaque(false);
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        JLabel overline = label("STAYEASE  ·  HOTEL MANAGEMENT", 12, BLUE_ACCENT, true);
        JLabel title = new JLabel("<html>StayEase,<br>nyaman di setiap perjalanan.</html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 44));
        title.setForeground(TEXT);
        JLabel intro = label("Reservasi kamar dan kelola perjalanan Anda melalui layanan hotel yang mudah digunakan.", 16, MUTED, false);
        intro.setMaximumSize(new Dimension(500, 70));
        JLabel features = label("✓  Pilihan kamar terkurasi     ✓  Reservasi praktis     ✓  Layanan ramah", 13, BLUE_ACCENT, true);
        welcome.add(Box.createVerticalGlue());
        welcome.add(overline);
        welcome.add(Box.createVerticalStrut(18));
        welcome.add(title);
        welcome.add(Box.createVerticalStrut(18));
        welcome.add(intro);
        welcome.add(Box.createVerticalStrut(24));
        welcome.add(features);
        welcome.add(Box.createVerticalGlue());
        body.add(welcome, c);

        c.gridx = 1;
        c.weightx = 0.42;
        c.insets = new Insets(0, 28, 0, 0);
        body.add(loginCard(), c);
        page.add(body, BorderLayout.CENTER);

        JLabel footer = label("StayEase  ·  Temukan kenyamanan di setiap perjalanan", 12, MUTED, false);
        page.add(footer, BorderLayout.SOUTH);
        screens.add(page, "auth");
        fadeTo("auth");
        frame.setTitle("StayEase | Masuk");
        screens.revalidate();
        screens.repaint();
    }

    private JPanel loginCard() {
        JPanel card = cardPanel();
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(26, 28, 26, 28)));
        card.setLayout(new BorderLayout(0, 18));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(CARD);
        tabs.addTab("Masuk", loginForm());
        tabs.addTab("Buat Akun", registerForm());
        card.add(tabs, BorderLayout.CENTER);
        JLabel hint = label("Akses admin tersedia melalui akun pengelola.", 12, MUTED, false);
        card.add(hint, BorderLayout.SOUTH);
        return card;
    }

    private JPanel loginForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = constraints();
        c.insets = new Insets(8, 2, 8, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        addFormRow(form, c, 0, "Username", username);
        addFormRow(form, c, 1, "Kata sandi", password);
        JButton submit = primaryButton("Masuk ke akun");
        c.gridy = 2;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(20, 2, 4, 2);
        form.add(submit, c);
        submit.addActionListener(event -> runAction(() -> {
            currentAccount = service.login(username.getText(), password.getPassword());
            showAccountHome();
        }));
        password.addActionListener(event -> submit.doClick());
        return form;
    }

    private JPanel registerForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = constraints();
        c.insets = new Insets(5, 2, 5, 2);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        JTextField name = new JTextField();
        JTextField username = new JTextField();
        JTextField email = new JTextField();
        JTextField phone = new JTextField();
        JPasswordField password = new JPasswordField();
        addFormRow(form, c, 0, "Nama lengkap", name);
        addFormRow(form, c, 1, "Username", username);
        addFormRow(form, c, 2, "Email", email);
        addFormRow(form, c, 3, "WhatsApp / seluler", phone);
        addFormRow(form, c, 4, "Kata sandi (min. 8 karakter)", password);
        JButton submit = primaryButton("Daftar sebagai tamu");
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.insets = new Insets(16, 2, 2, 2);
        form.add(submit, c);
        submit.addActionListener(event -> runAction(() -> {
            service.register(username.getText(), name.getText(), email.getText(), phone.getText(),
                    password.getPassword());
            showInfo("Akun berhasil dibuat. Silakan masuk menggunakan akun Anda.");
        }));
        return form;
    }

    private void showAccountHome() {
        if (currentAccount instanceof AdminAccount) showAdminHome();
        else showGuestHome();
    }

    private void showGuestHome() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(14, 28, 14, 28)));
        header.add(brandHeader("HOTEL & SUITES"), BorderLayout.WEST);
        JPanel accountActions = new JPanel();
        accountActions.setOpaque(false);
        accountActions.add(label("Selamat datang, " + currentAccount.getFullName(), 13, MUTED, false));
        JButton logout = secondaryButton("Keluar");
        logout.addActionListener(event -> logout());
        accountActions.add(logout);
        header.add(accountActions, BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        guestTabs = new JTabbedPane();
        guestTabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        guestTabs.setBorder(BorderFactory.createEmptyBorder(5, 24, 0, 24));
        guestTabs.addTab("Beranda", guestLanding());
        guestTabs.addTab("Pesan kamar", bookingPanel());
        guestTabs.addTab("Reservasi saya", guestReservationsPanel());
        guestTabs.addTab("Kamar & Suites", publicRoomsPanel());
        page.add(guestTabs, BorderLayout.CENTER);
        screens.removeAll();
        screens.add(page, "guest");
        fadeTo("guest");
        frame.setTitle("StayEase | Portal Tamu");
        screens.revalidate();
        screens.repaint();
    }

    private JPanel guestLanding() {
        HomeContent content = service.getHomeContent();
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(22, 30, 24, 30));
        JPanel topContent = new JPanel();
        topContent.setOpaque(false);
        topContent.setLayout(new BoxLayout(topContent, BoxLayout.Y_AXIS));
        GradientPanel hero = new GradientPanel();
        hero.setLayout(new BorderLayout());
        hero.setBorder(BorderFactory.createEmptyBorder(27, 32, 27, 32));
        JPanel heroCopy = new JPanel();
        heroCopy.setOpaque(false);
        heroCopy.setLayout(new BoxLayout(heroCopy, BoxLayout.Y_AXIS));
        heroCopy.add(label("STAYEASE  ·  HOTEL MANAGEMENT", 12, Color.WHITE, true));
        heroCopy.add(Box.createVerticalStrut(14));
        JLabel title = new JLabel("<html><div style='width:680px'>"
                + html(content.getHeroTitle()) + "</div></html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        title.setForeground(Color.WHITE);
        heroCopy.add(title);
        heroCopy.add(Box.createVerticalStrut(8));
        JLabel description = new JLabel("<html><div style='width:760px'>"
                + html(content.getWelcomeMessage()) + "</div></html>");
        description.setFont(new Font("SansSerif", Font.PLAIN, 14));
        description.setForeground(new Color(226, 237, 252));
        heroCopy.add(description);
        hero.add(heroCopy, BorderLayout.WEST);
        topContent.add(hero);
        topContent.add(Box.createVerticalStrut(14));
        topContent.add(bookingSearchBar());
        root.add(topContent, BorderLayout.NORTH);

        JPanel sections = new JPanel();
        sections.setOpaque(false);
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        if (content.isShowRoomCatalog()) {
            sections.add(sectionHeading("Kamar pilihan untuk Anda", "Temukan ruang nyaman yang sesuai dengan rencana perjalanan."));
            sections.add(Box.createVerticalStrut(12));
            sections.add(roomCardGrid(service.getRooms()));
            sections.add(Box.createVerticalStrut(20));
        }
        JPanel details = new JPanel(new GridLayout(1, 0, 12, 0));
        details.setOpaque(false);
        if (content.isShowAmenities()) {
            details.add(infoCard("LAYANAN ISTIMEWA", "Sarapan, city view, bathtub, dan layanan concierge.", "Kenyamanan untuk setiap momen."));
        }
        if (content.isShowBookingNotice()) {
            details.add(infoCard("INFORMASI RESERVASI", content.getBookingNotice(), "Reservasi dapat dilihat dari menu Reservasi Saya."));
        }
        if (details.getComponentCount() > 0) {
            sections.add(details);
        }
        JScrollPane scroll = new JScrollPane(sections);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        root.add(scroll, BorderLayout.CENTER);
        return root;
    }

    private JPanel bookingSearchBar() {
        JPanel search = cardPanel();
        search.setLayout(new GridBagLayout());
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(15, 18, 15, 18)));
        GridBagConstraints c = constraints();
        c.insets = new Insets(3, 5, 3, 9);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        JSpinner checkIn = dateSpinner(LocalDate.now().plusDays(1));
        JSpinner checkOut = dateSpinner(LocalDate.now().plusDays(2));
        JSpinner guests = new JSpinner(new SpinnerNumberModel(2, 1, 12, 1));
        search.add(searchField("CHECK-IN", checkIn), gridCell(c, 0, 0, 1));
        search.add(searchField("CHECK-OUT", checkOut), gridCell(c, 1, 0, 1));
        search.add(searchField("JUMLAH TAMU", guests), gridCell(c, 2, 0, 1));
        JButton find = primaryButton("Cari kamar");
        find.setPreferredSize(new Dimension(145, 44));
        c.gridx = 3;
        c.gridy = 0;
        c.weightx = 0;
        c.insets = new Insets(3, 6, 3, 0);
        search.add(find, c);
        find.addActionListener(event -> runAction(() -> {
            if (bookingCheckIn == null || bookingCheckOut == null || bookingGuests == null
                    || bookingRoomChoice == null) {
                throw new IllegalStateException("Form pemesanan belum siap. Silakan coba kembali.");
            }
            bookingCheckIn.setValue(checkIn.getValue());
            bookingCheckOut.setValue(checkOut.getValue());
            bookingGuests.setValue(guests.getValue());
            guestTabs.setSelectedIndex(1);
            searchAvailableRooms(bookingCheckIn, bookingCheckOut, bookingGuests, bookingRoomChoice);
        }));
        return search;
    }

    private JPanel searchField(String caption, Component input) {
        JPanel field = new JPanel(new BorderLayout(0, 6));
        field.setOpaque(false);
        field.add(label(caption, 10, MUTED, true), BorderLayout.NORTH);
        input.setPreferredSize(new Dimension(160, 38));
        field.add(input, BorderLayout.CENTER);
        return field;
    }

    private GridBagConstraints gridCell(GridBagConstraints template, int x, int y, double weight) {
        GridBagConstraints cell = (GridBagConstraints) template.clone();
        cell.gridx = x;
        cell.gridy = y;
        cell.weightx = weight;
        return cell;
    }

    private void showGuestHomeTab(int index) {
        if (guestTabs != null) guestTabs.setSelectedIndex(index);
    }

    private JPanel bookingPanel() {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(sectionHeading("Pesan Kamar", "Pilih tanggal menginap dan kamar yang tersedia."), BorderLayout.WEST);
        panel.add(top, BorderLayout.NORTH);

        JPanel form = cardPanel();
        form.setLayout(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(22, 24, 22, 24)));
        GridBagConstraints c = constraints();
        c.insets = new Insets(9, 8, 9, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        JSpinner checkIn = dateSpinner(LocalDate.now().plusDays(1));
        JSpinner checkOut = dateSpinner(LocalDate.now().plusDays(2));
        JSpinner guests = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        JComboBox<Room> rooms = new JComboBox<>();
        bookingCheckIn = checkIn;
        bookingCheckOut = checkOut;
        bookingGuests = guests;
        bookingRoomChoice = rooms;
        addFormRow(form, c, 0, "Check-in", checkIn);
        addFormRow(form, c, 1, "Check-out", checkOut);
        addFormRow(form, c, 2, "Jumlah tamu", guests);
        addFormRow(form, c, 3, "Kamar tersedia", rooms);

        JButton search = secondaryButton("Cari ketersediaan");
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        form.add(search, c);
        search.addActionListener(event -> runAction(() ->
                searchAvailableRooms(checkIn, checkOut, guests, rooms)));

        JTextArea notes = textArea(3);
        addFormRow(form, c, 5, "Permintaan khusus", new JScrollPane(notes));
        JComboBox<String> payment = new JComboBox<>(new String[]{
                "Transfer Bank Virtual Account", "QRIS", "Kartu Kredit", "Bayar di Front Desk"
        });
        addFormRow(form, c, 6, "Metode pembayaran", payment);
        JButton book = primaryButton("Konfirmasi reservasi");
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        form.add(book, c);
        book.addActionListener(event -> runAction(() -> {
            Room selected = (Room) rooms.getSelectedItem();
            if (selected == null) throw new IllegalArgumentException("Cari dan pilih kamar yang tersedia terlebih dahulu.");
            GuestAccount guest = (GuestAccount) currentAccount;
            Reservation reservation = service.createReservation(guest.getUsername(), guest.getFullName(),
                    guest.getPhone(), selected.getNumber(), dateValue(checkIn), dateValue(checkOut),
                    (Integer) guests.getValue(), (String) payment.getSelectedItem(), notes.getText());
            showInfo("Reservasi berhasil dibuat.\nID: " + reservation.getId()
                    + "\nStatus: " + reservation.getStatus()
                    + "\nEstimasi total: " + CURRENCY.format(reservation.calculateTotal()));
            showGuestHome();
        }));
        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private void searchAvailableRooms(JSpinner checkIn, JSpinner checkOut,
                                      JSpinner guests, JComboBox<Room> rooms) throws Exception {
        List<Room> found = service.findAvailableRooms(dateValue(checkIn), dateValue(checkOut),
                (Integer) guests.getValue());
        rooms.setModel(new DefaultComboBoxModel<>(found.toArray(Room[]::new)));
        if (found.isEmpty()) {
            showInfo("Belum ada kamar yang tersedia pada tanggal tersebut. Silakan pilih tanggal lain.");
        }
    }

    private JPanel guestReservationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        panel.add(sectionHeading("Reservasi Saya", "Lihat status dan rincian pemesanan Anda."), BorderLayout.NORTH);
        JTable table = table(new String[]{"ID Booking", "Kamar", "Check-in", "Check-out", "Jumlah", "Total", "Status"});
        fillGuestReservations(table);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton cancel = dangerButton("Batalkan reservasi terpilih");
        cancel.addActionListener(event -> {
            int row = table.getSelectedRow();
            if (row < 0) { showError("Pilih reservasi yang ingin dibatalkan."); return; }
            String id = table.getValueAt(row, 0).toString();
            if (confirm("Batalkan reservasi " + id + "?")) runAction(() -> {
                service.cancelReservation(id, currentAccount.getUsername(), false);
                fillGuestReservations(table);
            });
        });
        panel.add(cancel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel publicRoomsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(sectionHeading("Kamar & Suites", "Bandingkan fasilitas dan pilih kamar yang paling sesuai."), BorderLayout.WEST);
        JComboBox<String> filter = new JComboBox<>(new String[]{
                "Semua tipe", "Deluxe Heritage Room", "Grand Executive Suite",
                "Presidential Royal Penthouse"
        });
        filter.setPreferredSize(new Dimension(240, 38));
        header.add(filter, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);
        JPanel gallery = new JPanel(new BorderLayout());
        gallery.setOpaque(false);
        Runnable applyFilter = () -> {
            String category = (String) filter.getSelectedItem();
            List<Room> rooms = service.getRooms().stream()
                    .filter(room -> "Semua tipe".equals(category) || room.getCategory().equals(category))
                    .toList();
            gallery.removeAll();
            gallery.add(roomCardGrid(rooms), BorderLayout.NORTH);
            gallery.revalidate();
            gallery.repaint();
        };
        filter.addActionListener(event -> applyFilter.run());
        applyFilter.run();
        JScrollPane scroll = new JScrollPane(gallery);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel roomCardGrid(List<Room> rooms) {
        JPanel grid = new JPanel(new GridLayout(0, 3, 14, 14));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(2, 1, 10, 1));
        for (Room room : rooms) {
            if (room.getStatus() == RoomStatus.MAINTENANCE) continue;
            grid.add(roomCard(room));
        }
        if (grid.getComponentCount() == 0) {
            grid.add(infoCard("Kamar belum tersedia", "Silakan periksa kembali nanti.", "Kami menantikan kunjungan Anda."));
        }
        return grid;
    }

    private JPanel roomCard(Room room) {
        JPanel card = cardPanel();
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 8, 10, 8)));
        RoomImagePanel image = new RoomImagePanel(new Dimension(320, 178));
        image.setImageData(room.getImageData());
        card.add(image, BorderLayout.NORTH);
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setBorder(BorderFactory.createEmptyBorder(14, 9, 2, 9));
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(label(room.getCategory().toUpperCase(Locale.ROOT), 11, BLUE_ACCENT, true));
        details.add(Box.createVerticalStrut(5));
        details.add(label("Kamar " + room.getNumber() + " · " + room.getFloor(), 17, TEXT, true));
        details.add(Box.createVerticalStrut(6));
        details.add(label(room.getBedType() + " · Maks. " + room.getCapacity() + " tamu", 12, MUTED, false));
        details.add(Box.createVerticalStrut(6));
        JLabel facilities = label(room.getAmenities(), 12, MUTED, false);
        facilities.setToolTipText(room.getAmenities());
        details.add(facilities);
        details.add(Box.createVerticalStrut(12));
        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.setOpaque(false);
        JPanel rate = new JPanel();
        rate.setOpaque(false);
        rate.setLayout(new BoxLayout(rate, BoxLayout.Y_AXIS));
        rate.add(label(CURRENCY.format(room.getNightlyRate()), 16, TEXT, true));
        rate.add(label("per malam", 11, MUTED, false));
        bottom.add(rate, BorderLayout.CENTER);
        JButton choose = primaryButton("Pilih kamar");
        choose.addActionListener(event -> prepareRoomBooking(room));
        bottom.add(choose, BorderLayout.EAST);
        details.add(bottom);
        card.add(details, BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BLUE_ACCENT),
                        BorderFactory.createEmptyBorder(8, 8, 10, 8)));
            }
            @Override public void mouseExited(MouseEvent event) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(8, 8, 10, 8)));
            }
        });
        return card;
    }

    private void prepareRoomBooking(Room room) {
        showGuestHomeTab(1);
        if (bookingCheckIn == null || bookingRoomChoice == null) return;
        runAction(() -> {
            List<Room> available = service.findAvailableRooms(dateValue(bookingCheckIn),
                    dateValue(bookingCheckOut), (Integer) bookingGuests.getValue());
            if (available.stream().noneMatch(candidate -> candidate.getNumber().equals(room.getNumber()))) {
                bookingRoomChoice.setModel(new DefaultComboBoxModel<>(available.toArray(Room[]::new)));
                showInfo("Kamar ini belum tersedia pada tanggal yang dipilih. Silakan cari tanggal lainnya.");
                return;
            }
            bookingRoomChoice.setModel(new DefaultComboBoxModel<>(available.toArray(Room[]::new)));
            for (int i = 0; i < bookingRoomChoice.getItemCount(); i++) {
                if (bookingRoomChoice.getItemAt(i).getNumber().equals(room.getNumber())) {
                    bookingRoomChoice.setSelectedIndex(i);
                    break;
                }
            }
        });
    }

    private void showAdminHome() {
        screens.removeAll();
        AdminWorkspace workspace = new AdminWorkspace(currentAccount.getFullName(),
                this::showUnavailableAdminModule, this::logout);
        workspace.addPage("dashboard", "Dashboard",
                new DashboardPanel(service, currentAccount.getFullName(), workspace::showPage));
        workspace.addPage("rooms", "Data Kamar", adminRoomsPanel());
        workspace.addPage("reservations", "Reservasi", adminReservationsPanel());
        workspace.addPage("home", "Pengaturan Beranda", adminHomeContentPanel());
        screens.add(workspace, "admin");
        workspace.showPage("dashboard");
        fadeTo("admin");
        frame.setTitle("StayEase | Dashboard");
        screens.revalidate();
        screens.repaint();
    }

    private void showUnavailableAdminModule(String key) {
        String title = switch (key) {
            case "guests" -> "Data Tamu";
            case "payments" -> "Pembayaran";
            case "reports" -> "Laporan";
            default -> key;
        };
        showInfo("Modul " + title + " belum tersedia. Tidak ada data contoh yang ditampilkan.");
    }

    private JPanel adminRoomsPanel() {
        JPanel panel = adminContentPanel();
        JPanel top = new JPanel(new BorderLayout(12, 12));
        top.setOpaque(false);
        top.add(sectionHeading("Manajemen Kamar", "Kelola foto, tipe, tarif, fasilitas, dan ketersediaan kamar."), BorderLayout.NORTH);
        JPanel actionRows = new JPanel();
        actionRows.setOpaque(false);
        actionRows.setLayout(new BoxLayout(actionRows, BoxLayout.Y_AXIS));
        JPanel filterRow = new JPanel(new BorderLayout(10, 0));
        filterRow.setOpaque(false);
        roomSearch = new JTextField();
        roomSearch.setToolTipText("Cari nomor, tipe, atau fasilitas kamar");
        JPanel dropdowns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        dropdowns.setOpaque(false);
        roomCategoryFilter = new JComboBox<>(new String[]{
                "Semua tipe", "Deluxe Heritage Room", "Grand Executive Suite",
                "Presidential Royal Penthouse"
        });
        roomStatusFilter = new JComboBox<>(new String[]{"Semua status", "Tersedia", "Pemeliharaan"});
        roomCategoryFilter.setPreferredSize(new Dimension(190, 38));
        roomStatusFilter.setPreferredSize(new Dimension(150, 38));
        dropdowns.add(roomCategoryFilter);
        dropdowns.add(roomStatusFilter);
        filterRow.add(roomSearch, BorderLayout.CENTER);
        filterRow.add(dropdowns, BorderLayout.EAST);
        actionRows.add(filterRow);
        actionRows.add(Box.createVerticalStrut(9));
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton add = primaryButton("Tambah Kamar");
        JButton edit = secondaryButton("Ubah");
        JButton photo = secondaryButton("Kelola Foto");
        JButton delete = dangerButton("Hapus");
        buttons.add(add);
        buttons.add(edit);
        buttons.add(photo);
        buttons.add(delete);
        actionRows.add(buttons);
        top.add(actionRows, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);
        roomsTable = table(new String[]{"FOTO", "NO. KAMAR", "TIPE KAMAR", "LANTAI", "RANJANG",
                "Kapasitas", "Tarif / Malam", "Fasilitas", "Status"});
        roomsTable.setRowHeight(68);
        roomsTable.getColumnModel().getColumn(0).setPreferredWidth(104);
        roomsTable.getColumnModel().getColumn(0).setMaxWidth(120);
        roomsTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean selected, boolean focused,
                                                           int row, int column) {
                JLabel photoCell = (JLabel) super.getTableCellRendererComponent(
                        table, "", selected, focused, row, column);
                photoCell.setHorizontalAlignment(SwingConstants.CENTER);
                photoCell.setIcon(value instanceof ImageIcon image ? image : null);
                photoCell.setText(value instanceof ImageIcon ? "" : "—");
                return photoCell;
            }
        });
        roomsTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean selected, boolean focused,
                                                           int row, int column) {
                JLabel badge = (JLabel) super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                badge.setOpaque(true);
                if (!selected && "Tersedia".equals(String.valueOf(value))) {
                    badge.setForeground(new Color(21, 128, 61));
                    badge.setBackground(new Color(220, 252, 231));
                } else if (!selected) {
                    badge.setForeground(new Color(180, 83, 9));
                    badge.setBackground(new Color(255, 247, 237));
                }
                return badge;
            }
        });
        panel.add(new JScrollPane(roomsTable), BorderLayout.CENTER);
        fillRooms(roomsTable, service.getRooms());

        roomSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { filterRooms(); }
            @Override public void removeUpdate(DocumentEvent event) { filterRooms(); }
            @Override public void changedUpdate(DocumentEvent event) { filterRooms(); }
        });
        roomCategoryFilter.addActionListener(event -> filterRooms());
        roomStatusFilter.addActionListener(event -> filterRooms());
        add.addActionListener(event -> showRoomDialog(null));
        edit.addActionListener(event -> {
            Room selected = selectedRoom();
            if (selected == null) { showError("Pilih kamar yang ingin diubah."); return; }
            showRoomDialog(selected);
        });
        photo.addActionListener(event -> {
            Room selected = selectedRoom();
            if (selected == null) { showError("Pilih kamar yang ingin diunggah fotonya."); return; }
            chooseRoomImage(selected);
        });
        delete.addActionListener(event -> {
            Room selected = selectedRoom();
            if (selected == null) { showError("Pilih kamar yang ingin dihapus."); return; }
            if (confirm("Hapus kamar " + selected.getNumber() + "?")) runAction(() -> {
                service.deleteRoom(selected.getNumber());
                refreshRooms();
            });
        });
        return panel;
    }

    private JPanel adminReservationsPanel() {
        JPanel panel = adminContentPanel();
        JPanel top = new JPanel(new BorderLayout(12, 10));
        top.setOpaque(false);
        top.add(sectionHeading("Manajemen Reservasi & Tamu", "Buat, ubah status, dan kelola data reservasi."), BorderLayout.NORTH);
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton add = primaryButton("Buat Reservasi Manual");
        JButton edit = secondaryButton("Ubah");
        JButton delete = dangerButton("Hapus");
        JButton status = secondaryButton("Ubah Status");
        buttons.add(add);
        buttons.add(edit);
        buttons.add(status);
        buttons.add(delete);
        top.add(buttons, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);
        reservationsTable = table(new String[]{"ID Booking", "ID & Tamu", "Kamar & Tipe",
                "Check-in", "Check-out", "Total Biaya", "Pembayaran", "Status"});
        panel.add(new JScrollPane(reservationsTable), BorderLayout.CENTER);
        fillAdminReservations(reservationsTable);
        add.addActionListener(event -> showReservationDialog(null));
        edit.addActionListener(event -> {
            Reservation selected = selectedReservation();
            if (selected == null) { showError("Pilih reservasi yang ingin diubah."); return; }
            showReservationDialog(selected);
        });
        status.addActionListener(event -> changeReservationStatus());
        delete.addActionListener(event -> {
            Reservation selected = selectedReservation();
            if (selected == null) { showError("Pilih reservasi yang ingin dihapus."); return; }
            if (confirm("Hapus reservasi " + selected.getId() + "?")) runAction(() -> {
                service.deleteReservation(selected.getId());
                refreshReservations();
            });
        });
        return panel;
    }

    private JPanel adminHomeContentPanel() {
        JPanel panel = adminContentPanel();
        panel.add(sectionHeading("Pengaturan Beranda", "Pilih informasi yang terlihat oleh pengguna portal tamu."), BorderLayout.NORTH);
        HomeContent current = service.getHomeContent();
        JPanel form = cardPanel();
        form.setLayout(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)));
        GridBagConstraints c = constraints();
        c.insets = new Insets(8, 4, 8, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        JTextField title = new JTextField(current.getHeroTitle());
        JTextArea welcome = textArea(3);
        welcome.setText(current.getWelcomeMessage());
        JTextArea notice = textArea(4);
        notice.setText(current.getBookingNotice());
        JCheckBox showRooms = new JCheckBox("Tampilkan katalog kamar");
        JCheckBox showAmenities = new JCheckBox("Tampilkan informasi fasilitas hotel");
        JCheckBox showNotice = new JCheckBox("Tampilkan informasi reservasi");
        for (JCheckBox box : List.of(showRooms, showAmenities, showNotice)) {
            box.setOpaque(false);
            box.setForeground(TEXT);
        }
        showRooms.setSelected(current.isShowRoomCatalog());
        showAmenities.setSelected(current.isShowAmenities());
        showNotice.setSelected(current.isShowBookingNotice());
        addFormRow(form, c, 0, "Judul utama", title);
        addFormRow(form, c, 1, "Deskripsi hotel", new JScrollPane(welcome));
        addFormRow(form, c, 2, "Informasi reservasi", new JScrollPane(notice));
        c.gridx = 1;
        c.gridy = 3;
        form.add(showRooms, c);
        c.gridy = 4;
        form.add(showAmenities, c);
        c.gridy = 5;
        form.add(showNotice, c);
        JButton save = primaryButton("Simpan pengaturan beranda");
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 2;
        form.add(save, c);
        save.addActionListener(event -> runAction(() -> {
            service.updateHomeContent(title.getText(), welcome.getText(), notice.getText(),
                    showRooms.isSelected(), showAmenities.isSelected(), showNotice.isSelected());
            showInfo("Pengaturan beranda berhasil disimpan.");
        }));
        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private void showRoomDialog(Room existing) {
        boolean editing = existing != null;
        JComboBox<String> category = new JComboBox<>(new String[]{
                "Deluxe Heritage Room", "Grand Executive Suite", "Presidential Royal Penthouse"
        });
        JTextField number = new JTextField(editing ? existing.getNumber() : "");
        JComboBox<String> floor = new JComboBox<>(new String[]{
                "Lantai 1", "Lantai 2", "Lantai 3", "Lantai 4 (Penthouse)"
        });
        JComboBox<String> bed = new JComboBox<>(new String[]{
                "King Size Royal", "Twin Hollywood", "Super King Imperial"
        });
        JSpinner capacity = new JSpinner(new SpinnerNumberModel(editing ? existing.getCapacity() : 2, 1, 12, 1));
        JTextField rate = new JTextField(editing ? existing.getBaseRate().toPlainString() : "2400000");
        JTextField amenities = new JTextField(editing ? existing.getAmenities() : "Wi-Fi, sarapan, city view");
        JComboBox<RoomStatus> status = new JComboBox<>(RoomStatus.values());
        byte[][] selectedImage = {editing ? existing.getImageData() : null};
        RoomImagePanel imagePreview = new RoomImagePanel(new Dimension(560, 155));
        imagePreview.setImageData(selectedImage[0]);
        if (editing) {
            category.setSelectedItem(existing.getCategory());
            floor.setSelectedItem(existing.getFloor());
            bed.setSelectedItem(existing.getBedType());
            status.setSelectedItem(existing.getStatus());
            number.setEditable(false);
        }
        JCheckBox balcony = new JCheckBox("Private Balcony");
        JCheckBox tub = new JCheckBox("Deep Soaking Tub");
        JCheckBox butler = new JCheckBox("24/7 Butler Service");
        JCheckBox cellar = new JCheckBox("Wine Cellar");
        for (JCheckBox box : List.of(balcony, tub, butler, cellar)) {
            box.setOpaque(false);
            box.setForeground(TEXT);
        }
        if (editing) {
            String current = existing.getAmenities().toLowerCase(Locale.ROOT);
            balcony.setSelected(current.contains("balcony"));
            tub.setSelected(current.contains("tub") || current.contains("bathtub"));
            butler.setSelected(current.contains("butler"));
            cellar.setSelected(current.contains("cellar"));
        }
        JPanel form = dialogForm();
        GridBagConstraints c = constraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        addFormRow(form, c, 0, "Tipe kamar", category);
        addFormRow(form, c, 1, "Nomor kamar", number);
        addFormRow(form, c, 2, "Lantai", floor);
        addFormRow(form, c, 3, "Tipe ranjang", bed);
        addFormRow(form, c, 4, "Maksimum tamu", capacity);
        addFormRow(form, c, 5, "Tarif dasar / malam (IDR)", rate);
        addFormRow(form, c, 6, "Fasilitas utama", amenities);
        addFormRow(form, c, 7, "Status", status);
        JPanel facilities = new JPanel();
        facilities.setOpaque(false);
        for (JCheckBox box : List.of(balcony, tub, butler, cellar)) facilities.add(box);
        addFormRow(form, c, 8, "Fasilitas unggulan", facilities);
        JPanel imageControls = new JPanel();
        imageControls.setOpaque(false);
        JButton upload = secondaryButton("Unggah foto kamar");
        JButton removeImage = dangerButton("Hapus foto");
        imageControls.add(upload);
        imageControls.add(removeImage);
        upload.addActionListener(event -> chooseImageData(selectedImage, imagePreview));
        removeImage.addActionListener(event -> {
            selectedImage[0] = null;
            imagePreview.setImageData(null);
        });
        addFormRow(form, c, 9, "Foto untuk card kamar", imagePreview);
        addFormRow(form, c, 10, "", imageControls);
        if (JOptionPane.showConfirmDialog(frame, new JScrollPane(form),
                editing ? "Ubah Kamar" : "Tambah Kamar Baru",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        runAction(() -> {
            String selectedCategory = (String) category.getSelectedItem();
            StringBuilder selectedAmenities = new StringBuilder(amenities.getText().trim());
            for (JCheckBox box : List.of(balcony, tub, butler, cellar)) {
                if (box.isSelected() && !selectedAmenities.toString().toLowerCase(Locale.ROOT)
                        .contains(box.getText().toLowerCase(Locale.ROOT))) {
                    selectedAmenities.append(", ").append(box.getText());
                }
            }
            BigDecimal baseRate = new BigDecimal(rate.getText().trim());
            if (editing) {
                service.updateRoom(existing.getNumber(), selectedCategory, number.getText(),
                        (String) floor.getSelectedItem(), (String) bed.getSelectedItem(),
                        (Integer) capacity.getValue(), baseRate, selectedAmenities.toString(),
                        (RoomStatus) status.getSelectedItem());
                service.updateRoomImage(existing.getNumber(), selectedImage[0]);
            } else {
                Room room = createRoom(selectedCategory, number.getText(), (String) floor.getSelectedItem(),
                        (String) bed.getSelectedItem(), (Integer) capacity.getValue(), baseRate,
                        selectedAmenities.toString(), (RoomStatus) status.getSelectedItem());
                room.setImageData(selectedImage[0]);
                service.addRoom(room);
            }
            refreshRooms();
        });
    }

    private void chooseRoomImage(Room room) {
        byte[][] selectedImage = {room.getImageData()};
        RoomImagePanel preview = new RoomImagePanel(new Dimension(560, 210));
        preview.setImageData(selectedImage[0]);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBackground(CARD);
        content.add(preview, BorderLayout.CENTER);
        JPanel actions = new JPanel();
        actions.setOpaque(false);
        JButton upload = secondaryButton("Pilih foto");
        JButton remove = dangerButton("Hapus foto");
        actions.add(upload);
        actions.add(remove);
        content.add(actions, BorderLayout.SOUTH);
        upload.addActionListener(event -> chooseImageData(selectedImage, preview));
        remove.addActionListener(event -> {
            selectedImage[0] = null;
            preview.setImageData(null);
        });
        int answer = JOptionPane.showConfirmDialog(frame, content,
                "Foto kamar " + room.getNumber(), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) runAction(() -> {
            service.updateRoomImage(room.getNumber(), selectedImage[0]);
            refreshRooms();
            showInfo("Foto kamar tersimpan di database dan akan tampil pada card kamar.");
        });
    }

    private void chooseImageData(byte[][] imageData, RoomImagePanel preview) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih foto kamar");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Gambar kamar (JPG, JPEG, PNG)", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            byte[] bytes = Files.readAllBytes(chooser.getSelectedFile().toPath());
            if (bytes.length > 8 * 1024 * 1024) {
                throw new IOException("Ukuran gambar maksimal 8 MB.");
            }
            decodeRoomImage(bytes);
            imageData[0] = bytes;
            preview.setImageData(bytes);
        } catch (IOException | IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void showReservationDialog(Reservation existing) {
        boolean editing = existing != null;
        JTextField name = new JTextField(editing ? existing.getGuestName() : "");
        JTextField phone = new JTextField(editing ? existing.getPhone() : "");
        JComboBox<Room> room = new JComboBox<>(service.getRooms().toArray(Room[]::new));
        JSpinner checkIn = dateSpinner(editing ? existing.getCheckIn() : LocalDate.now().plusDays(1));
        JSpinner checkOut = dateSpinner(editing ? existing.getCheckOut() : LocalDate.now().plusDays(2));
        JSpinner guests = new JSpinner(new SpinnerNumberModel(editing ? existing.getGuests() : 1, 1, 12, 1));
        JComboBox<String> payment = new JComboBox<>(new String[]{
                "Transfer Bank Perusahaan (Invoice)", "Cash / EDC Front Desk",
                "Kartu Kredit Virtual", "QRIS"
        });
        JTextArea notes = textArea(3);
        JComboBox<ReservationStatus> status = new JComboBox<>(ReservationStatus.values());
        if (editing) {
            for (int i = 0; i < room.getItemCount(); i++) {
                if (room.getItemAt(i).getNumber().equals(existing.getRoomNumber())) {
                    room.setSelectedIndex(i);
                    break;
                }
            }
            payment.setSelectedItem(existing.getPaymentMethod());
            notes.setText(existing.getNotes());
            status.setSelectedItem(existing.getStatus());
        }
        JPanel form = dialogForm();
        GridBagConstraints c = constraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        addFormRow(form, c, 0, "Nama lengkap tamu", name);
        addFormRow(form, c, 1, "Nomor telepon / WhatsApp", phone);
        addFormRow(form, c, 2, "Pilihan kamar", room);
        addFormRow(form, c, 3, "Tanggal check-in", checkIn);
        addFormRow(form, c, 4, "Tanggal check-out", checkOut);
        addFormRow(form, c, 5, "Jumlah tamu", guests);
        addFormRow(form, c, 6, "Metode pembayaran", payment);
        addFormRow(form, c, 7, "Status reservasi", status);
        addFormRow(form, c, 8, "Permintaan khusus", new JScrollPane(notes));
        if (JOptionPane.showConfirmDialog(frame, new JScrollPane(form),
                editing ? "Ubah Reservasi" : "Buat Reservasi Manual",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        runAction(() -> {
            Room selectedRoom = (Room) room.getSelectedItem();
            if (selectedRoom == null) throw new IllegalArgumentException("Pilih kamar terlebih dahulu.");
            if (editing) {
                service.updateReservation(existing.getId(), name.getText(), phone.getText(),
                        selectedRoom.getNumber(), dateValue(checkIn), dateValue(checkOut),
                        (Integer) guests.getValue(), (String) payment.getSelectedItem(), notes.getText(),
                        (ReservationStatus) status.getSelectedItem());
            } else {
                service.createManualReservation(name.getText(), phone.getText(), selectedRoom.getNumber(),
                        dateValue(checkIn), dateValue(checkOut), (Integer) guests.getValue(),
                        (String) payment.getSelectedItem(), notes.getText());
            }
            refreshReservations();
        });
    }

    private void changeReservationStatus() {
        Reservation selected = selectedReservation();
        if (selected == null) { showError("Pilih reservasi yang ingin diperbarui."); return; }
        ReservationStatus status = (ReservationStatus) JOptionPane.showInputDialog(frame,
                "Status kedatangan tamu", "Perbarui Status",
                JOptionPane.PLAIN_MESSAGE, null, ReservationStatus.values(), selected.getStatus());
        if (status != null) runAction(() -> {
            service.updateReservationStatus(selected.getId(), status);
            refreshReservations();
        });
    }

    private void fillRooms(JTable table, List<Room> rooms) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (Room room : rooms) {
            if (table == roomsTable) {
                if (roomSearch != null && !roomSearch.getText().isBlank()) {
                    String query = roomSearch.getText().trim().toLowerCase(Locale.ROOT);
                    String searchable = (room.getNumber() + room.getCategory() + room.getAmenities()
                            + room.getFloor()).toLowerCase(Locale.ROOT);
                    if (!searchable.contains(query)) continue;
                }
                if (roomCategoryFilter != null
                        && !"Semua tipe".equals(roomCategoryFilter.getSelectedItem())
                        && !room.getCategory().equals(roomCategoryFilter.getSelectedItem())) continue;
                if (roomStatusFilter != null
                        && !"Semua status".equals(roomStatusFilter.getSelectedItem())
                        && !room.getStatus().toString().equals(roomStatusFilter.getSelectedItem())) continue;
            }
            model.addRow(new Object[]{roomThumbnail(room), room.getNumber(), room.getCategory(),
                    room.getFloor(), room.getBedType(), room.getCapacity(), CURRENCY.format(room.getNightlyRate()),
                    room.getAmenities(), room.getStatus()});
        }
    }

    private ImageIcon roomThumbnail(Room room) {
        byte[] imageData = room.getImageData();
        if (imageData == null) return null;
        try {
            BufferedImage image = decodeRoomImage(imageData);
            return new ImageIcon(image.getScaledInstance(84, 54, Image.SCALE_SMOOTH));
        } catch (IOException exception) {
            throw new IllegalStateException("Foto kamar " + room.getNumber() + " tidak dapat ditampilkan.", exception);
        }
    }

    private void fillAdminReservations(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (Reservation reservation : service.getReservations()) {
            String category;
            try {
                category = service.findRoom(reservation.getRoomNumber()).getCategory();
            } catch (hotel.model.HotelException exception) {
                category = "Kamar dihapus";
            }
            model.addRow(new Object[]{reservation.getId(),
                    reservation.getGuestName() + " · " + reservation.getPhone(),
                    reservation.getRoomNumber() + " · " + category,
                    reservation.getCheckIn(), reservation.getCheckOut(),
                    CURRENCY.format(reservation.calculateTotal()),
                    reservation.getPaymentMethod(), reservation.getStatus()});
        }
    }

    private void fillGuestReservations(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (Reservation reservation : service.getReservationsFor(currentAccount.getUsername())) {
            model.addRow(new Object[]{reservation.getId(), reservation.getRoomNumber(),
                    reservation.getCheckIn(), reservation.getCheckOut(), reservation.getGuests(),
                    CURRENCY.format(reservation.calculateTotal()), reservation.getStatus()});
        }
    }

    private void filterRooms() {
        if (roomsTable != null) fillRooms(roomsTable, service.getRooms());
    }

    private void refreshRooms() {
        if (roomsTable != null) fillRooms(roomsTable, service.getRooms());
    }

    private void refreshReservations() {
        if (reservationsTable != null) fillAdminReservations(reservationsTable);
    }

    private Room selectedRoom() {
        int row = roomsTable == null ? -1 : roomsTable.getSelectedRow();
        if (row < 0) return null;
        int modelRow = roomsTable.convertRowIndexToModel(row);
        String number = roomsTable.getModel().getValueAt(modelRow, 1).toString();
        return service.getRooms().stream().filter(room -> room.getNumber().equals(number)).findFirst().orElse(null);
    }

    private Reservation selectedReservation() {
        int row = reservationsTable == null ? -1 : reservationsTable.getSelectedRow();
        if (row < 0) return null;
        int modelRow = reservationsTable.convertRowIndexToModel(row);
        String id = reservationsTable.getModel().getValueAt(modelRow, 0).toString();
        return service.getReservations().stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
    }

    private void logout() {
        currentAccount = null;
        service.logout();
        showLogin();
    }

    private JPanel adminContentPanel() {
        JPanel panel = new JPanel(new BorderLayout(18, 18));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        return panel;
    }

    private JPanel brandHeader(String subtitle) {
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel name = label("StayEase", 24, BLUE_ACCENT, true);
        name.setFont(new Font("SansSerif", Font.BOLD, 23));
        brand.add(name);
        brand.add(label(subtitle, 10, MUTED, true));
        return brand;
    }

    private JPanel infoCard(String title, String body, String footer) {
        JPanel card = cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.add(label(title, 12, BLUE_ACCENT, true));
        card.add(Box.createVerticalStrut(12));
        JLabel text = label(body, 14, TEXT, false);
        text.setMaximumSize(new Dimension(320, 70));
        card.add(text);
        card.add(Box.createVerticalStrut(10));
        card.add(label(footer, 11, MUTED, false));
        return card;
    }

    private JPanel sectionHeading(String title, String subtitle) {
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(label(title, 25, TEXT, true));
        heading.add(Box.createVerticalStrut(5));
        heading.add(label(subtitle, 13, MUTED, false));
        return heading;
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createLineBorder(BORDER));
        panel.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) {
                panel.setBorder(BorderFactory.createLineBorder(new Color(174, 202, 241)));
            }
            @Override public void mouseExited(MouseEvent event) {
                panel.setBorder(BorderFactory.createLineBorder(BORDER));
            }
        });
        return panel;
    }

    private JPanel dialogForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        return panel;
    }

    private JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(24, 105, 220));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        return styleButton(button, new Color(24, 105, 220), new Color(17, 83, 179));
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(TEXT);
        return styleButton(button, Color.WHITE, new Color(242, 246, 251));
    }

    private JButton dangerButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(255, 244, 243));
        button.setForeground(new Color(175, 55, 48));
        return styleButton(button, new Color(255, 244, 243), new Color(255, 231, 229));
    }

    private JButton styleButton(JButton button, Color normal, Color hover) {
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normal.equals(Color.WHITE) ? BORDER : new Color(255, 255, 255, 35)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) { button.setBackground(hover); }
            @Override public void mouseExited(MouseEvent event) { button.setBackground(normal); }
        });
        return button;
    }

    private JTable table(String[] columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        table.getTableHeader().setOpaque(true);
        return table;
    }

    private JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 24);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(Color.WHITE);
        area.setForeground(TEXT);
        area.setCaretColor(BLUE_ACCENT);
        area.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return area;
    }

    private JSpinner dateSpinner(LocalDate date) {
        Date value = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner spinner = new JSpinner(new SpinnerDateModel(value, null, null, java.util.Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        return spinner;
    }

    private LocalDate dateValue(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void addFormRow(JPanel form, GridBagConstraints constraints, int row, String title, Component input) {
        GridBagConstraints c = (GridBagConstraints) constraints.clone();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.weightx = 0.35;
        c.anchor = GridBagConstraints.LINE_START;
        JLabel field = label(title, 13, TEXT, true);
        form.add(field, c);
        c.gridx = 1;
        c.weightx = 0.65;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(input, c);
    }

    private GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.NORTHWEST;
        return c;
    }

    private Room createRoom(String category, String number, String floor, String bed, int capacity,
                            BigDecimal rate, String amenities, RoomStatus status) {
        return switch (category) {
            case "Deluxe Heritage Room" -> new StandardRoom(number, floor, bed, capacity, rate, amenities, status);
            case "Grand Executive Suite" -> new DeluxeRoom(number, floor, bed, capacity, rate, amenities, status);
            case "Presidential Royal Penthouse" -> new SuiteRoom(number, floor, bed, capacity, rate, amenities, status);
            default -> throw new IllegalArgumentException("Tipe kamar tidak dikenal.");
        };
    }

    private void runAction(CheckedAction action) {
        try {
            action.run();
        } catch (Exception exception) {
            showError(exception.getMessage() == null ? "Operasi tidak dapat diselesaikan." : exception.getMessage());
        }
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(frame, message, "StayEase", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Periksa kembali", JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(frame, message, "Konfirmasi",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void fadeTo(String screen) {
        if (screenTransition != null) screenTransition.stop();
        screenLayout.show(screens, screen);
        screens.setAlpha(0f);
        screenTransition = new Timer(16, null);
        screenTransition.addActionListener(event -> {
            float next = screens.getAlpha() + 0.1f;
            if (next >= 1f) {
                screens.setAlpha(1f);
                screenTransition.stop();
            } else {
                screens.setAlpha(next);
            }
        });
        screenTransition.start();
    }

    private static BufferedImage decodeRoomImage(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new IOException("Gambar tidak dapat dibaca.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Gunakan file gambar PNG atau JPEG yang valid.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > 24_000_000L) throw new IOException("Resolusi gambar maksimal 24 megapiksel.");
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    private static final class GradientPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private GradientPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(700, 160));
            setMinimumSize(new Dimension(500, 140));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            copy.setPaint(new GradientPaint(0, 0, new Color(35, 119, 231),
                    width, height, new Color(13, 61, 133)));
            copy.fillRoundRect(0, 0, width, height, 24, 24);
            copy.dispose();
        }
    }

    private static final class FadingPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private float alpha = 1f;

        private FadingPanel(CardLayout layout) {
            super(layout);
        }

        private float getAlpha() { return alpha; }

        private void setAlpha(float value) {
            alpha = Math.max(0f, Math.min(1f, value));
            repaint();
        }

        @Override
        protected void paintChildren(Graphics graphics) {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setComposite(java.awt.AlphaComposite.SrcOver.derive(alpha));
            super.paintChildren(copy);
            copy.dispose();
        }
    }

    private static final class RoomImagePanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private transient BufferedImage image;

        private RoomImagePanel(Dimension preferredSize) {
            setPreferredSize(preferredSize);
            setMinimumSize(new Dimension(180, 125));
            setOpaque(false);
        }

        private void setImageData(byte[] data) {
            try {
                image = data == null ? null : decodeRoomImage(data);
            } catch (IOException exception) {
                throw new IllegalArgumentException(exception.getMessage(), exception);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            copy.clip(new RoundRectangle2D.Float(0, 0, width, height, 16, 16));
            copy.setPaint(new GradientPaint(0, 0, new Color(213, 230, 253),
                    width, height, new Color(232, 241, 252)));
            copy.fillRoundRect(0, 0, width, height, 16, 16);
            if (image != null) {
                double scale = Math.max((double) width / image.getWidth(),
                        (double) height / image.getHeight());
                int imageWidth = (int) Math.ceil(image.getWidth() * scale);
                int imageHeight = (int) Math.ceil(image.getHeight() * scale);
                copy.drawImage(image, (width - imageWidth) / 2,
                        (height - imageHeight) / 2, imageWidth, imageHeight, null);
            } else {
                copy.setColor(BLUE_ACCENT);
                copy.setFont(new Font("SansSerif", Font.BOLD, 34));
                String initial = "K";
                int textWidth = copy.getFontMetrics().stringWidth(initial);
                copy.drawString(initial, (width - textWidth) / 2, height / 2 - 4);
                copy.setFont(new Font("SansSerif", Font.BOLD, 10));
                String title = "STAYEASE  ·  HOTEL MANAGEMENT";
                textWidth = copy.getFontMetrics().stringWidth(title);
                copy.setColor(TEXT);
                copy.drawString(title, (width - textWidth) / 2, height / 2 + 20);
                copy.setFont(new Font("SansSerif", Font.PLAIN, 10));
                String caption = "Unggah foto kamar dari portal admin";
                textWidth = copy.getFontMetrics().stringWidth(caption);
                copy.setColor(MUTED);
                copy.drawString(caption, (width - textWidth) / 2, height / 2 + 39);
            }
            copy.dispose();
        }
    }

    private String html(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
