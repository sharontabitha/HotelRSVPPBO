package hotel.ui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

public final class SurfaceCardPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public SurfaceCardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth() - 1;
        int height = getHeight() - 4;
        copy.setColor(new Color(31, 41, 55, 12));
        copy.fillRoundRect(1, 3, width - 1, height + 1, 16, 16);
        copy.setColor(StayEaseTheme.SURFACE);
        copy.fillRoundRect(0, 0, width - 1, height, 16, 16);
        copy.setColor(StayEaseTheme.BORDER);
        copy.drawRoundRect(0, 0, width - 1, height, 16, 16);
        copy.dispose();
    }
}
