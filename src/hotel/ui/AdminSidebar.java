package hotel.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class AdminSidebar extends JPanel {
    private static final long serialVersionUID = 1L;
    private final transient Map<String, JButton> menuButtons = new LinkedHashMap<>();
    private final transient Consumer<String> onNavigate;

    public AdminSidebar(Consumer<String> onNavigate, Runnable onLogout) {
        this.onNavigate = onNavigate;
        setBackground(StayEaseTheme.NAVY);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(24, 16, 20, 16));
        setPreferredSize(new Dimension(244, 0));

        JLabel logo = new JLabel("StayEase");
        logo.setFont(new Font("SansSerif", Font.BOLD, 24));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(logo);
        JLabel tagline = new JLabel("HOTEL MANAGEMENT");
        tagline.setFont(new Font("SansSerif", Font.BOLD, 9));
        tagline.setForeground(new Color(174, 193, 218));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(tagline);
        add(Box.createVerticalStrut(34));
        addSectionTitle("MENU UTAMA");
        addItem("dashboard", "▦", "Dashboard", true);
        addItem("rooms", "▤", "Data Kamar", true);
        addItem("guests", "♙", "Data Tamu", false);
        addItem("reservations", "▣", "Reservasi", true);
        addItem("payments", "◉", "Pembayaran", false);
        addItem("reports", "▥", "Laporan", false);
        addItem("home", "⚙", "Pengaturan Beranda", true);
        add(Box.createVerticalGlue());
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hotelLabel = new JLabel("StayEase Hotel");
        hotelLabel.setForeground(Color.WHITE);
        hotelLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel roleLabel = new JLabel("Portal administrator");
        roleLabel.setForeground(new Color(174, 193, 218));
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        identity.add(hotelLabel);
        identity.add(Box.createVerticalStrut(4));
        identity.add(roleLabel);
        add(identity);
        add(Box.createVerticalStrut(14));
        JButton logout = new JButton("↪   Logout");
        style(logout, false);
        logout.addActionListener(event -> onLogout.run());
        add(logout);
    }

    public void setActive(String key) {
        menuButtons.forEach((name, button) -> style(button, name.equals(key)));
    }

    private void addSectionTitle(String title) {
        JLabel section = new JLabel(title);
        section.setForeground(new Color(174, 193, 218));
        section.setFont(new Font("SansSerif", Font.BOLD, 10));
        section.setBorder(BorderFactory.createEmptyBorder(0, 8, 10, 0));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(section);
    }

    private void addItem(String key, String icon, String title, boolean enabled) {
        JButton button = new JButton(icon + "   " + title);
        button.setToolTipText(enabled ? title : title + " — modul belum tersedia");
        button.setEnabled(enabled);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.addActionListener(event -> onNavigate.accept(key));
        style(button, false);
        menuButtons.put(key, button);
        add(button);
        add(Box.createVerticalStrut(5));
    }

    private void style(JButton button, boolean selected) {
        button.setFont(new Font("SansSerif", selected ? Font.BOLD : Font.PLAIN, 13));
        button.setForeground(selected ? Color.WHITE
                : button.isEnabled() ? new Color(221, 231, 245) : new Color(137, 155, 181));
        button.setBackground(selected ? StayEaseTheme.BLUE : StayEaseTheme.NAVY);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? new Color(91, 153, 249) : StayEaseTheme.NAVY),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));
        button.setCursor(button.isEnabled()
                ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                : java.awt.Cursor.getDefaultCursor());
    }
}
