package hotel.ui;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public final class StayEaseTheme {
    public static final Color NAVY = new Color(30, 58, 95);
    public static final Color BLUE = new Color(59, 130, 246);
    public static final Color BACKGROUND = new Color(245, 247, 250);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = new Color(31, 41, 55);
    public static final Color MUTED = new Color(107, 114, 128);
    public static final Color BORDER = new Color(229, 233, 239);
    public static final Color SUCCESS = new Color(34, 197, 94);
    public static final Color WARNING = new Color(245, 158, 11);
    public static final Color DANGER = new Color(239, 68, 68);
    public static final Color PALE_BLUE = new Color(239, 246, 255);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);

    private StayEaseTheme() {}

    public static void install() {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Exception exception) {
            throw new IllegalStateException("FlatLaf tidak tersedia. Jalankan aplikasi melalui Maven.", exception);
        }
        UIManager.put("defaultFont", BODY);
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 9);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", BODY);
        UIManager.put("Button.font", BODY);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", BLUE);
        UIManager.put("TextField.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        UIManager.put("PasswordField.background", SURFACE);
        UIManager.put("PasswordField.foreground", TEXT);
        UIManager.put("PasswordField.caretForeground", BLUE);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("TextArea.background", SURFACE);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.caretForeground", BLUE);
        UIManager.put("Table.font", BODY);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.selectionBackground", PALE_BLUE);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", new Color(248, 249, 251));
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 12));
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.foreground", TEXT);
    }
}
