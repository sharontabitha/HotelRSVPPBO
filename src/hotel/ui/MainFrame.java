package hotel.ui;

import javax.swing.JFrame;
import java.awt.Dimension;

public final class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public MainFrame() {
        super("StayEase | Hotel Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setSize(1360, 860);
        setLocationRelativeTo(null);
    }
}
