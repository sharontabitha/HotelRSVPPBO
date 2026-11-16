package hotel;

import hotel.controller.HotelController;
import hotel.data.HotelRepository;
import hotel.model.Account;
import hotel.service.AuthenticationService;
import hotel.service.HotelService;
import hotel.ui.AdminWorkspace;
import hotel.ui.DashboardPanel;
import hotel.ui.StayEaseTheme;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class StayEaseUiSmokeTest {
    private StayEaseUiSmokeTest() {}

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("stayease.admin.initialPassword", "StayEaseUiTest-Admin-2026");
        StayEaseTheme.install();
        Path database = Files.createTempDirectory("stayease-ui-test-").resolve("stayease.sqlite");
        HotelService hotelService = new HotelService(new HotelRepository(database));
        HotelController controller = new HotelController(hotelService);
        Account admin = controller.login(AuthenticationService.DEMO_ADMIN_USERNAME,
                "StayEaseUiTest-Admin-2026".toCharArray());

        SwingUtilities.invokeAndWait(() -> {
            AdminWorkspace workspace = new AdminWorkspace(admin.getFullName(), key -> {
                throw new AssertionError("Disabled navigation unexpectedly activated: " + key);
            }, () -> {});
            workspace.addPage("dashboard", "Dashboard",
                    new DashboardPanel(controller, admin.getFullName(), workspace::showPage));
            workspace.addPage("rooms", "Data Kamar", new javax.swing.JPanel());
            workspace.addPage("reservations", "Reservasi", new javax.swing.JPanel());
            workspace.showPage("dashboard");
            Set<String> labels = new HashSet<>();
            collectLabels(workspace, labels);
            require(containsText(labels, "Dashboard"), "Dashboard navigation item");
            require(containsText(labels, "Data Kamar"), "Room navigation item");
            require(containsText(labels, "Data Tamu"), "Guest navigation item remains visible");
            require(containsText(labels, "Pembayaran"), "Payment navigation item remains visible");
            require(containsText(labels, "Laporan"), "Report navigation item remains visible");
            require(labels.contains("TOTAL KAMAR"), "Dashboard total room card");
            require(labels.contains("KAMAR TERSEDIA"), "Dashboard availability card");
            require(labels.contains("KAMAR TERISI"), "Dashboard occupancy card");
            require(labels.contains("TOTAL RESERVASI"), "Dashboard reservation card");
            require(labels.contains("Reservasi terbaru"), "Dashboard recent reservation table");
            workspace.showPage("rooms");
            workspace.showPage("reservations");
        });
        System.out.println("PASS: FlatLaf theme, responsive admin workspace navigation, real-data dashboard cards, and recent-reservations table.");
    }

    private static void collectLabels(Container parent, Set<String> labels) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JLabel label) labels.add(label.getText());
            if (component instanceof AbstractButton button) labels.add(button.getText());
            if (component instanceof Container container) collectLabels(container, labels);
        }
    }

    private static void require(boolean condition, String assertion) {
        if (!condition) throw new AssertionError(assertion);
    }

    private static boolean containsText(Set<String> labels, String text) {
        return labels.stream().anyMatch(label -> label != null && label.contains(text));
    }
}
