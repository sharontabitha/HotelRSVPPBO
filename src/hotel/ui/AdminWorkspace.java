package hotel.ui;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class AdminWorkspace extends JPanel {
    private static final long serialVersionUID = 1L;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final transient Map<String, PageInfo> pageInfo = new LinkedHashMap<>();
    private final AdminTopbar topbar;
    private final AdminSidebar sidebar;
    private final transient Consumer<String> onNavigate;

    public AdminWorkspace(String adminName, Consumer<String> onNavigate, Runnable onLogout) {
        super(new BorderLayout());
        this.onNavigate = onNavigate;
        topbar = new AdminTopbar(adminName);
        sidebar = new AdminSidebar(this::navigate, onLogout);
        add(sidebar, BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(topbar, BorderLayout.NORTH);
        right.add(pages, BorderLayout.CENTER);
        add(right, BorderLayout.CENTER);
    }

    public void addPage(String key, String title, Component component) {
        if (pageInfo.containsKey(key)) throw new IllegalArgumentException("Halaman sudah terdaftar: " + key);
        pageInfo.put(key, new PageInfo(title));
        pages.add(component, key);
    }

    public void showPage(String key) {
        PageInfo info = pageInfo.get(key);
        if (info == null) throw new IllegalArgumentException("Halaman belum terdaftar: " + key);
        pageLayout.show(pages, key);
        sidebar.setActive(key);
        topbar.showPage(info.title(), "StayEase  /  " + info.title());
    }

    private void navigate(String key) {
        if (pageInfo.containsKey(key)) {
            showPage(key);
            return;
        }
        onNavigate.accept(key);
    }

    private record PageInfo(String title) {}
}
