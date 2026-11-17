package hotel.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Komponen input penanggalan berbasis Swing dengan kalender pop-up interaktif.
 */
public final class DatePicker extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FULL_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"));
    private static final String[] MONTH_NAMES = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };
    private static final String[] DAY_NAMES = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};

    private LocalDate selectedDate;
    private LocalDate minDate;
    private YearMonth displayMonth;
    private final JTextField textField;
    private final JButton calendarButton;
    private final JPopupMenu popup;
    private final JPanel calendarPanel;
    private final transient List<Consumer<LocalDate>> listeners = new ArrayList<>();

    public DatePicker(LocalDate initialDate) {
        super(new BorderLayout(0, 0));
        this.selectedDate = initialDate != null ? initialDate : LocalDate.now();
        this.displayMonth = YearMonth.from(this.selectedDate);
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(StayEaseTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        textField = new JTextField(selectedDate.format(DATE_FORMAT));
        textField.setEditable(false);
        textField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        textField.setForeground(StayEaseTheme.TEXT);
        textField.setBackground(Color.WHITE);
        textField.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 4));
        textField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        textField.setToolTipText(selectedDate.format(FULL_FORMAT));

        calendarButton = new JButton("📅");
        calendarButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        calendarButton.setFocusable(false);
        calendarButton.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 10));
        calendarButton.setBackground(Color.WHITE);
        calendarButton.setContentAreaFilled(false);
        calendarButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calendarButton.setToolTipText("Buka kalender untuk memilih tanggal");

        popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(StayEaseTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        popup.setBackground(Color.WHITE);

        calendarPanel = new JPanel(new BorderLayout(0, 8));
        calendarPanel.setOpaque(false);
        popup.add(calendarPanel);

        MouseAdapter opener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isEnabled()) togglePopup();
            }
        };
        textField.addMouseListener(opener);
        calendarButton.addActionListener(e -> {
            if (isEnabled()) togglePopup();
        });

        add(textField, BorderLayout.CENTER);
        add(calendarButton, BorderLayout.EAST);
        setPreferredSize(new Dimension(160, 38));
    }

    public void togglePopup() {
        if (popup.isVisible()) {
            popup.setVisible(false);
        } else {
            if (!isShowing()) return;
            displayMonth = YearMonth.from(selectedDate);
            rebuildCalendar();
            popup.show(this, 0, getHeight() + 2);
        }
    }

    private void rebuildCalendar() {
        calendarPanel.removeAll();

        // Header: Tombol bulan sebelumnya, judul bulan & tahun, tombol bulan berikutnya
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);

        JButton prevBtn = new JButton("◄");
        prevBtn.setFont(new Font("SansSerif", Font.BOLD, 10));
        prevBtn.setMargin(new Insets(2, 6, 2, 6));
        prevBtn.setFocusable(false);
        prevBtn.addActionListener(e -> {
            displayMonth = displayMonth.minusMonths(1);
            rebuildCalendar();
            popup.pack();
        });

        JButton nextBtn = new JButton("►");
        nextBtn.setFont(new Font("SansSerif", Font.BOLD, 10));
        nextBtn.setMargin(new Insets(2, 6, 2, 6));
        nextBtn.setFocusable(false);
        nextBtn.addActionListener(e -> {
            displayMonth = displayMonth.plusMonths(1);
            rebuildCalendar();
            popup.pack();
        });

        JLabel title = new JLabel(MONTH_NAMES[displayMonth.getMonthValue() - 1] + " " + displayMonth.getYear(), SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(StayEaseTheme.TEXT);

        header.add(prevBtn, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(nextBtn, BorderLayout.EAST);
        calendarPanel.add(header, BorderLayout.NORTH);

        // Grid Container: Header nama hari + Grid 42 sel hari
        JPanel gridContainer = new JPanel(new BorderLayout(0, 4));
        gridContainer.setOpaque(false);

        JPanel dayNamesGrid = new JPanel(new GridLayout(1, 7, 2, 0));
        dayNamesGrid.setOpaque(false);
        for (int i = 0; i < 7; i++) {
            JLabel dayLabel = new JLabel(DAY_NAMES[i], SwingConstants.CENTER);
            dayLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            dayLabel.setForeground(i == 0 ? new Color(220, 38, 38) : StayEaseTheme.MUTED);
            dayNamesGrid.add(dayLabel);
        }
        gridContainer.add(dayNamesGrid, BorderLayout.NORTH);

        JPanel daysGrid = new JPanel(new GridLayout(6, 7, 2, 2));
        daysGrid.setOpaque(false);

        LocalDate firstDayOfMonth = displayMonth.atDay(1);
        int startCol = firstDayOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0
        int daysInMonth = displayMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 42; i++) {
            int dayNum = i - startCol + 1;
            if (dayNum < 1 || dayNum > daysInMonth) {
                JLabel empty = new JLabel("");
                daysGrid.add(empty);
            } else {
                LocalDate cellDate = displayMonth.atDay(dayNum);
                JButton dayButton = new JButton(String.valueOf(dayNum));
                dayButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
                dayButton.setMargin(new Insets(3, 3, 3, 3));
                dayButton.setFocusable(false);
                dayButton.setPreferredSize(new Dimension(34, 28));

                boolean isSelected = cellDate.equals(selectedDate);
                boolean isToday = cellDate.equals(today);
                boolean isBeforeMin = minDate != null && cellDate.isBefore(minDate);

                if (isBeforeMin) {
                    dayButton.setEnabled(false);
                    dayButton.setForeground(new Color(200, 205, 215));
                } else if (isSelected) {
                    dayButton.setBackground(StayEaseTheme.BLUE);
                    dayButton.setForeground(Color.WHITE);
                    dayButton.setFont(new Font("SansSerif", Font.BOLD, 12));
                } else if (isToday) {
                    dayButton.setBackground(StayEaseTheme.PALE_BLUE);
                    dayButton.setForeground(StayEaseTheme.BLUE);
                    dayButton.setFont(new Font("SansSerif", Font.BOLD, 12));
                } else {
                    dayButton.setBackground(Color.WHITE);
                    dayButton.setForeground(i % 7 == 0 ? new Color(220, 38, 38) : StayEaseTheme.TEXT);
                }

                dayButton.addActionListener(e -> {
                    setDate(cellDate);
                    popup.setVisible(false);
                });
                daysGrid.add(dayButton);
            }
        }
        gridContainer.add(daysGrid, BorderLayout.CENTER);
        calendarPanel.add(gridContainer, BorderLayout.CENTER);

        // Footer: Hari Ini & Tutup
        JPanel footer = new JPanel(new BorderLayout(6, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JButton todayBtn = new JButton("Hari Ini");
        todayBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        todayBtn.setFocusable(false);
        todayBtn.setEnabled(minDate == null || !today.isBefore(minDate));
        todayBtn.addActionListener(e -> {
            setDate(today);
            popup.setVisible(false);
        });

        JButton closeBtn = new JButton("Tutup");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        closeBtn.setFocusable(false);
        closeBtn.addActionListener(e -> popup.setVisible(false));

        footer.add(todayBtn, BorderLayout.WEST);
        footer.add(closeBtn, BorderLayout.EAST);
        calendarPanel.add(footer, BorderLayout.SOUTH);
    }

    public LocalDate getDate() {
        return selectedDate;
    }

    public void setDate(LocalDate date) {
        if (date == null) return;
        this.selectedDate = date;
        this.displayMonth = YearMonth.from(date);
        textField.setText(date.format(DATE_FORMAT));
        textField.setToolTipText(date.format(FULL_FORMAT));
        for (Consumer<LocalDate> listener : listeners) {
            listener.accept(date);
        }
    }

    public void setMinDate(LocalDate minDate) {
        this.minDate = minDate;
        if (selectedDate != null && minDate != null && selectedDate.isBefore(minDate)) {
            setDate(minDate);
        }
    }

    public LocalDate getMinDate() {
        return minDate;
    }

    public void addDateChangeListener(Consumer<LocalDate> listener) {
        listeners.add(listener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        calendarButton.setEnabled(enabled);
    }
}