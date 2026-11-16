package hotel.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

public final class AdminTopbar extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel title = new JLabel();
    private final JLabel breadcrumb = new JLabel();

    public AdminTopbar(String adminName) {
        super(new BorderLayout(16, 0));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, StayEaseTheme.BORDER),
                BorderFactory.createEmptyBorder(15, 26, 15, 26)));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new javax.swing.BoxLayout(heading, javax.swing.BoxLayout.Y_AXIS));
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        title.setForeground(StayEaseTheme.TEXT);
        breadcrumb.setFont(new Font("SansSerif", Font.PLAIN, 11));
        breadcrumb.setForeground(StayEaseTheme.MUTED);
        heading.add(title);
        heading.add(javax.swing.Box.createVerticalStrut(4));
        heading.add(breadcrumb);
        add(heading, BorderLayout.WEST);

        JPanel profile = new JPanel(new BorderLayout(10, 0));
        profile.setOpaque(false);
        JLabel avatar = new JLabel(initials(adminName), JLabel.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(StayEaseTheme.PALE_BLUE);
        avatar.setForeground(StayEaseTheme.NAVY);
        avatar.setFont(new Font("SansSerif", Font.BOLD, 13));
        avatar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        profile.add(avatar, BorderLayout.WEST);
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new javax.swing.BoxLayout(details, javax.swing.BoxLayout.Y_AXIS));
        JLabel name = new JLabel(adminName);
        name.setFont(new Font("SansSerif", Font.BOLD, 12));
        name.setForeground(StayEaseTheme.TEXT);
        JLabel role = new JLabel("Administrator");
        role.setFont(new Font("SansSerif", Font.PLAIN, 11));
        role.setForeground(StayEaseTheme.MUTED);
        details.add(name);
        details.add(role);
        profile.add(details, BorderLayout.CENTER);
        add(profile, BorderLayout.EAST);
        showPage("Dashboard", "StayEase  /  Dashboard");
    }

    public void showPage(String pageTitle, String path) {
        title.setText(pageTitle);
        breadcrumb.setText(path);
    }

    private static String initials(String value) {
        if (value == null || value.isBlank()) return "SE";
        String[] parts = value.trim().split("\\s+");
        return parts.length > 1
                ? (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase()
                : parts[0].substring(0, 1).toUpperCase();
    }
}
