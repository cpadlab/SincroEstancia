package sincroestancia.src.gui.dasboard.components;

import sincroestancia.src.gui.dasboard.components.day.Day;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import sincroestancia.src.utils.ImageUtils;
import sincroestancia.src.services.DatabaseService;
import java.util.Map;
import java.util.HashMap;
import sincroestancia.src.models.DayInfo;
import sincroestancia.src.gui.components.ButtonUtils;

/**
 * Componente gráfico personalizado que representa el calendario mensual interactivo.
 * * Responsabilidades principales:
 * - Renderizar una rejilla de días (matriz 6x7) respetando el calendario gregoriano.
 * - Gestionar la navegación entre meses y años.
 * - Visualizar el estado de cada día mediante códigos de colores (Temporadas) e indicadores (Puntos de reserva).
 * - Gestionar la selección de días y comunicar el cambio al panel de detalle (Day).
 * * @author Carlos Padilla Labella
 */
public class Calendar extends javax.swing.JPanel {

    private int current_year;
    private int current_month;
    private final Locale spanishLocale = new Locale("es", "ES");

    private final String icon_path = "/sincroestancia/assets/images/";
    private final int icon_size = 18;
    private int current_vut_id;

    private final Color border_color = new Color(245, 245, 245);
    private final Color hover_color = new Color(250, 250, 250);
    private final Color default_hover_day_btn_color = new Color(229, 229, 229);
    private final Color white_color = Color.WHITE;
    private final Color day_btn_default_color = white_color;
    private final Color low_day_btn_color = new Color(240, 253, 244);
    private final Color average_day_btn_color = new Color(255, 247, 237);
    private final Color high_day_btn_color = new Color(254, 242, 242);
    private final Color hover_low_day_btn_color = new Color(187, 247, 208);
    private final Color hover_average_day_btn_color = new Color(254, 215, 170);
    private final Color hover_high_day_btn_color = new Color(254, 202, 202);

    private final Day day_panel_ref;
    private final DatabaseService db_service;

    private int selected_day;
    private int selected_month;
    private int selected_year;
    private JButton selected_day_button = null;

    /**
     * Constructor del Calendario.
     * * Inicializa la fecha al día de hoy, configura los listeners de navegación
     * y realiza el primer renderizado.
     * * @param day_panel Referencia al panel lateral de detalles para notificar cambios.
     * @param vut_id ID de la vivienda actual para cargar sus datos.
     */
    public Calendar(Day day_panel, int vut_id) {

        this.current_vut_id = vut_id;
        this.day_panel_ref = day_panel;

        initComponents();

        this.db_service = new DatabaseService();

        GregorianCalendar cal = new GregorianCalendar();
        this.current_year = cal.get(GregorianCalendar.YEAR);
        this.current_month = cal.get(GregorianCalendar.MONTH);

        this.selected_year = this.current_year;
        this.selected_month = this.current_month;
        this.selected_day = cal.get(GregorianCalendar.DAY_OF_MONTH);

        add_navigation_listeners();
        update_calendar();

        this.day_panel_ref.updateDayInfo(this.selected_day, this.selected_month, this.selected_year,
                this.current_vut_id);
    }

    /**
     * Cambia la vivienda activa y refresca el calendario visualmente.
     */
    public void setVutId(int new_vut_id) {
        this.current_vut_id = new_vut_id;
        update_calendar();
    }

    /**
     * Método CORE: Renderiza la rejilla del calendario.
     * * Flujo lógico:
     * 1. Limpia la rejilla actual.
     * 2. Obtiene los datos de precios/estados de la BD para el mes actual.
     * 3. Calcula el "offset" (qué día de la semana cae el día 1).
     * 4. Rellena los huecos previos con los días finales del mes anterior (en gris).
     * 5. Genera los botones para los días del mes actual:
     * - Aplica color según temporada (Low/Avg/High).
     * - Añade un punto (•) si hay reserva.
     * - Resalta en azul si es el día seleccionado.
     * - Configura eventos de clic y hover.
     * 6. Rellena los huecos finales con los días del mes siguiente (en gris).
     */
    public void update_calendar() {

        this.selected_day_button = null;

        Map<Integer, DayInfo> monthData = new HashMap<>();
        if (this.current_vut_id > 0) {
            monthData = db_service.get_month_data(this.current_vut_id, this.current_year, this.current_month);
        }

        GregorianCalendar cal = new GregorianCalendar(this.current_year, this.current_month, 1);

        SimpleDateFormat month_year_format = new SimpleDateFormat("MMMM yyyy", spanishLocale);
        month_label.setText(month_year_format.format(cal.getTime()).toUpperCase());

        days_grid_panel.removeAll();
        days_grid_panel.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));

        int firstDayOfWeek = cal.get(GregorianCalendar.DAY_OF_WEEK);
        int startOffset = (firstDayOfWeek - GregorianCalendar.MONDAY + 7) % 7;

        GregorianCalendar prevMonthCal = (GregorianCalendar) cal.clone();
        prevMonthCal.add(GregorianCalendar.MONTH, -1);
        int daysInPrevMonth = prevMonthCal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);

        for (int i = 0; i < startOffset; i++) {
            int dayNumber = daysInPrevMonth - startOffset + 1 + i;

            JLabel dayLabel = new JLabel(String.valueOf(dayNumber), SwingConstants.CENTER);
            dayLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
            dayLabel.setForeground(new Color(180, 180, 180));
            dayLabel.setOpaque(true);
            dayLabel.setBackground(Color.WHITE);

            days_grid_panel.add(dayLabel);
        }

        int days_in_month = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);

        for (int i = 1; i <= days_in_month; i++) {

            final int day = i;
            JButton day_button = new JButton(String.valueOf(i));

            day_button.setMargin(new java.awt.Insets(1, 1, 1, 1));
            day_button.setFocusable(false);
            day_button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            day_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            ButtonUtils.applyRoundedStyle(day_button);

            Color day_btn_color = day_btn_default_color;
            Color hover_btn_color = default_hover_day_btn_color;

            GregorianCalendar today = new GregorianCalendar();
            if (this.current_year == today.get(GregorianCalendar.YEAR) &&
                    this.current_month == today.get(GregorianCalendar.MONTH) &&
                    i == today.get(GregorianCalendar.DAY_OF_MONTH)) {
                day_button.setForeground(Color.BLUE);
                day_button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
            }

            if (this.current_vut_id > 0) {
                DayInfo dayInfo = monthData.get(day);

                if (dayInfo != null) {
                    String season = dayInfo.season();
                    String status = dayInfo.status();

                    if (status != null && (status.equals("reserved") || status.equals("paid"))) {
                        day_button.setText("<html><center>" + day + "<br><font size='-2'>•</font></center></html>");
                        day_button.setMargin(new java.awt.Insets(0, 1, 0, 1));
                    }

                    switch (season) {
                        case "low" -> {
                            day_btn_color = low_day_btn_color;
                            hover_btn_color = hover_low_day_btn_color;
                        }
                        case "average" -> {
                            day_btn_color = average_day_btn_color;
                            hover_btn_color = hover_average_day_btn_color;
                        }
                        case "high" -> {
                            day_btn_color = high_day_btn_color;
                            hover_btn_color = hover_high_day_btn_color;
                        }
                    }
                }
            }

            day_button.setBackground(day_btn_color);
            day_button.putClientProperty("baseColor", day_btn_color);
            day_button.putClientProperty("hoverColor", hover_btn_color);

            day_button.putClientProperty("originalBaseColor", day_btn_color);
            day_button.putClientProperty("originalHoverColor", hover_btn_color);

            boolean isSelected = this.current_year == this.selected_year &&
                    this.current_month == this.selected_month &&
                    day == this.selected_day;

            if (isSelected) {
                day_button.setBackground(new Color(200, 230, 255)); // Azul claro
                day_button.putClientProperty("baseColor", new Color(200, 230, 255));
                day_button.putClientProperty("hoverColor", new Color(150, 200, 255));
                this.selected_day_button = day_button;
            }

            day_button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    Color baseColor = (Color) day_button.getClientProperty("hoverColor");
                    day_button.setBackground(baseColor != null ? baseColor : default_hover_day_btn_color);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Color baseColor = (Color) day_button.getClientProperty("baseColor");
                    day_button.setBackground(baseColor != null ? baseColor : day_btn_default_color);
                }
            });

            day_button.addActionListener((ActionEvent e) -> {
                if (this.selected_day_button != null) {
                    Color oldBaseColor = (Color) this.selected_day_button.getClientProperty("originalBaseColor");
                    Color oldHoverColor = (Color) this.selected_day_button.getClientProperty("originalHoverColor");
                    this.selected_day_button.setBackground(oldBaseColor);
                    this.selected_day_button.putClientProperty("baseColor", oldBaseColor);
                    this.selected_day_button.putClientProperty("hoverColor", oldHoverColor);
                }

                day_button.putClientProperty("originalBaseColor", day_button.getClientProperty("baseColor"));
                day_button.putClientProperty("originalHoverColor", day_button.getClientProperty("hoverColor"));

                this.selected_day = day;
                this.selected_month = this.current_month;
                this.selected_year = this.current_year;

                day_button.setBackground(new Color(200, 230, 255));
                day_button.putClientProperty("baseColor", new Color(200, 230, 255));
                day_button.putClientProperty("hoverColor", new Color(150, 200, 255));
                this.selected_day_button = day_button;

                day_panel_ref.updateDayInfo(day, this.current_month, this.current_year, this.current_vut_id);
            });

            days_grid_panel.add(day_button);
        }

        int totalCells = startOffset + days_in_month;
        int nextMonthDay = 1;

        for (int i = totalCells; i < 42; i++) {

            JLabel dayLabel = new JLabel(String.valueOf(nextMonthDay), SwingConstants.CENTER);
            dayLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
            dayLabel.setForeground(new Color(180, 180, 180));
            dayLabel.setOpaque(true);
            dayLabel.setBackground(Color.WHITE);

            days_grid_panel.add(dayLabel);
            nextMonthDay++;
        }

        days_grid_panel.revalidate();
        days_grid_panel.repaint();

        day_panel_ref.updateDayInfo(this.selected_day, this.selected_month, this.selected_year, this.current_vut_id);
    }

    /**
     * Configura los listeners para los botones de navegación (Anterior, Siguiente, Hoy).
     */
    private void add_navigation_listeners() {

        btn_prev_month.addActionListener((ActionEvent e) -> {
            this.current_month--;
            if (this.current_month < 0) {
                this.current_month = 11;
                this.current_year--;
            }
            update_calendar();
        });

        btn_next_month.addActionListener((ActionEvent e) -> {
            this.current_month++;
            if (this.current_month > 11) {
                this.current_month = 0;
                this.current_year++;
            }
            update_calendar();
        });

        btn_today.addActionListener((ActionEvent e) -> {
            GregorianCalendar cal = new GregorianCalendar();
            this.current_year = cal.get(GregorianCalendar.YEAR);
            this.current_month = cal.get(GregorianCalendar.MONTH);

            if (this.selected_day_button != null) {
                this.selected_day_button.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }

            this.selected_year = this.current_year;
            this.selected_month = this.current_month;
            this.selected_day = cal.get(GregorianCalendar.DAY_OF_MONTH);

            update_calendar();

            day_panel_ref.updateDayInfo(this.selected_day, this.selected_month, this.selected_year,
                    this.current_vut_id);
        });
    }

    /**
     * Inicialización de componentes gráficos (Layout, Iconos, Colores).
     */
    private void initComponents() {

        JPanel navigation_panel = new JPanel(new BorderLayout(10, 10));
        JPanel calendar_panel = new JPanel(new BorderLayout());
        JPanel week_days_panel = new JPanel(new GridLayout(1, 7));
        days_grid_panel = new JPanel(new GridLayout(6, 7, 1, 1));

        btn_prev_month = new JButton();
        btn_next_month = new JButton();
        btn_today = new JButton();

        ImageIcon prev_month_icon = ImageUtils.get_scaled_icon(icon_path + "angle-small-left.png", icon_size,
                icon_size);
        ImageIcon next_month_icon = ImageUtils.get_scaled_icon(icon_path + "angle-small-right.png", icon_size,
                icon_size);
        ImageIcon calendar_icon = ImageUtils.get_scaled_icon(icon_path + "calendar-day.png", icon_size, icon_size);

        btn_prev_month.setIcon(prev_month_icon);
        if (prev_month_icon == null)
            btn_prev_month.setText("<");

        btn_next_month.setIcon(next_month_icon);
        if (next_month_icon == null)
            btn_next_month.setText(">");

        btn_today.setIcon(calendar_icon);
        btn_today.setText("Hoy");

        JButton[] nav_buttons = { btn_prev_month, btn_next_month, btn_today };
        Dimension icon_button_size = new Dimension(30, 30);

        MouseAdapter hover_effect = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                JButton button = (JButton) e.getSource();
                button.setContentAreaFilled(true);
                button.setBackground(hover_color);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JButton button = (JButton) e.getSource();
                button.setContentAreaFilled(false);
                button.setBackground(white_color);
            }
        };

        for (JButton btn : nav_buttons) {
            btn.setBorder(null);
            btn.setContentAreaFilled(false);
            btn.setFocusable(false);
            btn.addMouseListener(hover_effect);
        }

        btn_prev_month.setPreferredSize(icon_button_size);
        btn_prev_month.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        btn_next_month.setPreferredSize(icon_button_size);
        btn_next_month.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        btn_today.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn_today.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ButtonUtils.applyRoundedStyle(btn_today);

        month_label = new JLabel("MES AÑO", SwingConstants.CENTER);
        month_label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.CENTER_BASELINE, 14));

        JPanel left_nav_panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
        left_nav_panel.add(btn_prev_month);
        left_nav_panel.add(month_label);
        left_nav_panel.add(btn_next_month);

        navigation_panel.add(left_nav_panel, BorderLayout.CENTER);
        navigation_panel.add(btn_today, BorderLayout.EAST);
        navigation_panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] weekDays = { "L", "M", "X", "J", "V", "S", "D" };
        for (String day : weekDays) {
            JLabel day_label = new JLabel(day, SwingConstants.CENTER);
            day_label.setFont(new java.awt.Font("Inter", java.awt.Font.PLAIN, 14));
            week_days_panel.add(day_label);
            day_label.setOpaque(true);
            day_label.setBackground(new Color(229, 229, 229));
            day_label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }
        week_days_panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));

        calendar_panel.add(week_days_panel, BorderLayout.NORTH);
        calendar_panel.add(days_grid_panel, BorderLayout.CENTER);
        calendar_panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 10, 10));

        setLayout(new BorderLayout());
        add(navigation_panel, BorderLayout.NORTH);
        add(calendar_panel, BorderLayout.CENTER);

        setBackground(white_color);
        navigation_panel.setBackground(white_color);
        calendar_panel.setBackground(white_color);
        week_days_panel.setBackground(new Color(229, 229, 229));
        days_grid_panel.setBackground(border_color);
        left_nav_panel.setBackground(white_color);

    }

    private javax.swing.JPanel days_grid_panel;
    private javax.swing.JLabel month_label;
    private javax.swing.JButton btn_prev_month;
    private javax.swing.JButton btn_next_month;
    private javax.swing.JButton btn_today;

}