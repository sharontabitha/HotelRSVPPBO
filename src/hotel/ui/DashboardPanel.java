package hotel.ui;

import hotel.controller.HotelController;
import hotel.model.Reservation;
import hotel.model.Room;
import hotel.model.RoomStatus;
import hotel.model.HotelException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class DashboardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
    private final transient HotelController controller;
    private final transient Consumer<String> navigate;

    public DashboardPanel(HotelController controller, String adminName, Consumer<String> navigate) {
        super(new BorderLayout(0, 22));
        this.controller = controller;
        this.navigate = navigate;
        setBackground(StayEaseTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        add(greeting(adminName), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(0, 22));
        body.setOpaque(false);
        body.add(statistics(), BorderLayout.NORTH);
        body.add(recentReservations(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    private JPanel greeting(String adminName) {
        JPanel panel = new JPanel(new BorderLayout(12, 8));
        panel.setOpaque(false);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Selamat datang, " + adminName);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(StayEaseTheme.TEXT);
        JLabel subtitle = new JLabel("Berikut ringkasan operasional StayEase hari ini.");
        subtitle.setFont(StayEaseTheme.BODY);
        subtitle.setForeground(StayEaseTheme.MUTED);
        text.add(title);
        text.add(javax.swing.Box.createVerticalStrut(6));
        text.add(subtitle);
        panel.add(text, BorderLayout.WEST);
        JLabel date = new JLabel(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy",
                Locale.forLanguageTag("id-ID"))), SwingConstants.RIGHT);
        date.setForeground(StayEaseTheme.MUTED);
        date.setFont(StayEaseTheme.BODY);
        panel.add(date, BorderLayout.EAST);
        return panel;
    }

    private JPanel statistics() {
        List<Room> rooms = controller.getRooms();
        long maintenance = rooms.stream().filter(room -> room.getStatus() == RoomStatus.MAINTENANCE).count();
        int available;
        try {
            available = controller.findAvailableRooms(LocalDate.now(), LocalDate.now().plusDays(1), 1).size();
        } catch (HotelException exception) {
            throw new IllegalStateException("Tidak dapat menghitung kamar tersedia.", exception);
        }
        int occupied = Math.max(0, rooms.size() - available - (int) maintenance);
        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(statCard("TOTAL KAMAR", String.valueOf(rooms.size()), "Unit terdaftar",
                new Color(59, 130, 246), "▤"));
        cards.add(statCard("KAMAR TERSEDIA", String.valueOf(available), "Siap dipesan",
                StayEaseTheme.SUCCESS, "✓"));
        cards.add(statCard("KAMAR TERISI", String.valueOf(occupied), "Tamu menginap hari ini",
                StayEaseTheme.WARNING, "⌂"));
        cards.add(statCard("TOTAL RESERVASI", String.valueOf(controller.getReservations().size()),
                "Seluruh status", new Color(139, 92, 246), "▣"));
        return cards;
    }

    private JPanel statCard(String title, String value, String caption, Color accent, String icon) {
        JPanel card = surfaceCard(new BorderLayout(14, 8));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 22, 18));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new javax.swing.BoxLayout(left, javax.swing.BoxLayout.Y_AXIS));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 10));
        heading.setForeground(StayEaseTheme.MUTED);
        JLabel amount = new JLabel(value);
        amount.setFont(new Font("SansSerif", Font.BOLD, 27));
        amount.setForeground(StayEaseTheme.TEXT);
        JLabel foot = new JLabel(caption);
        foot.setFont(new Font("SansSerif", Font.PLAIN, 11));
        foot.setForeground(StayEaseTheme.MUTED);
        left.add(heading);
        left.add(javax.swing.Box.createVerticalStrut(9));
        left.add(amount);
        left.add(javax.swing.Box.createVerticalStrut(4));
        left.add(foot);
        JLabel mark = new JLabel(icon, SwingConstants.CENTER);
        mark.setOpaque(true);
        mark.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24));
        mark.setForeground(accent);
        mark.setFont(new Font("SansSerif", Font.BOLD, 18));
        mark.setPreferredSize(new Dimension(40, 40));
        card.add(left, BorderLayout.CENTER);
        card.add(mark, BorderLayout.EAST);
        return card;
    }

    private JPanel recentReservations() {
        JPanel card = surfaceCard(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 22, 20));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Reservasi terbaru");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(StayEaseTheme.TEXT);
        JLabel subtitle = new JLabel("Pantau pemesanan kamar yang tercatat di sistem.");
        subtitle.setForeground(StayEaseTheme.MUTED);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titles.add(title);
        titles.add(javax.swing.Box.createVerticalStrut(5));
        titles.add(subtitle);
        heading.add(titles, BorderLayout.WEST);
        JButton all = new JButton("Lihat reservasi");
        all.setForeground(StayEaseTheme.BLUE);
        all.addActionListener(event -> navigate.accept("reservations"));
        heading.add(all, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID RESERVASI", "NAMA TAMU", "KAMAR", "CHECK-IN", "CHECK-OUT", "TOTAL", "STATUS"}, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        controller.getReservations().stream()
                .sorted(Comparator.comparing(Reservation::getCheckIn).reversed())
                .limit(8)
                .forEach(reservation -> model.addRow(new Object[]{
                        reservation.getId(), reservation.getGuestName(),
                        roomName(reservation.getRoomNumber()), reservation.getCheckIn(),
                        reservation.getCheckOut(), CURRENCY.format(reservation.calculateTotal()),
                        reservation.getStatus()
                }));
        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(
                0, 0, 1, 0, StayEaseTheme.BORDER));
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable current, Object value,
                                                           boolean selected, boolean focused,
                                                           int row, int column) {
                JLabel badge = (JLabel) super.getTableCellRendererComponent(
                        current, value, selected, focused, row, column);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                badge.setOpaque(true);
                if (!selected) {
                    switch (String.valueOf(value)) {
                        case "Dikonfirmasi", "Checked-In" -> {
                            badge.setForeground(new Color(21, 128, 61));
                            badge.setBackground(new Color(220, 252, 231));
                        }
                        case "Dibatalkan" -> {
                            badge.setForeground(new Color(185, 28, 28));
                            badge.setBackground(new Color(254, 226, 226));
                        }
                        default -> {
                            badge.setForeground(new Color(180, 83, 9));
                            badge.setBackground(new Color(255, 247, 237));
                        }
                    }
                }
                return badge;
            }
        });
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private String roomName(String roomNumber) {
        return controller.getRooms().stream().filter(room -> room.getNumber().equalsIgnoreCase(roomNumber))
                .map(room -> room.getCategory() + " · " + roomNumber)
                .findFirst().orElse("Kamar " + roomNumber);
    }

    private JPanel surfaceCard(BorderLayout layout) {
        return new SurfaceCardPanel(layout);
    }
}
