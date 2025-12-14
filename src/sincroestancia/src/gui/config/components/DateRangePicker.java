package sincroestancia.src.gui.config.components;

import java.awt.*;
import java.awt.event.ActionListener;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import sincroestancia.src.models.DayInfo;
import sincroestancia.src.services.DatabaseService;

/**
 * Componente gráfico personalizado que muestra un calendario mensual interactivo.
 * * Diseñado para la selección de rangos de fechas (fecha inicio - fecha fin) en el módulo de configuración.
 * * Características principales:
 * - Navegación entre meses.
 * - Visualización del estado de los días (Pasado, Reservado, Disponible).
 * - Código de colores para mostrar la temporada configurada (Baja, Media, Alta).
 * - Bloqueo visual de días no disponibles para evitar selecciones inválidas.
 * * @author Carlos Padilla Labella
 */
public class DateRangePicker extends JPanel {

    private JLabel lblMonthYear;
    private JPanel daysPanel;
    private JButton btnPrev;
    private JButton btnNext;

    private DatabaseService dbService;
    private int currentVutId = -1;
    private YearMonth currentYearMonth;
    private LocalDate selectedStartDate;
    private LocalDate selectedEndDate;
    private final Locale locale = Locale.ENGLISH; 
    
    private Map<Integer, DayInfo> currentMonthData = new HashMap<>();

    private final Color COLOR_SELECTED = new Color(0, 120, 215);
    private final Color COLOR_RANGE = new Color(200, 230, 255);
    private final Color COLOR_RESERVED = new Color(200, 200, 200);
    private final Color COLOR_PAST = new Color(240, 240, 240);
    private final Color SEASON_LOW = new Color(220, 255, 220);
    private final Color SEASON_AVG = new Color(255, 245, 210);
    private final Color SEASON_HIGH = new Color(255, 220, 220);
    private final Color SEASON_NONE = Color.WHITE;
    private final Color COLOR_TODAY_TEXT = new Color(34, 139, 34);

    /**
     * Constructor del selector de rango.
     * * Inicializa el calendario en el mes actual y conecta con el servicio de base de datos.
     */
    public DateRangePicker() {
        this.dbService = new DatabaseService();
        this.currentYearMonth = YearMonth.now();
        initComponents();
        refreshCalendar();
    }
    
    /**
     * Asigna la vivienda activa y refresca la visualización con sus datos específicos.
     * * @param vutId Identificador de la vivienda.
     */
    public void setVutId(int vutId) {
        this.currentVutId = vutId;
        refreshCalendar();
    }
    
    /**
     * Fuerza una recarga de los datos desde la base de datos y repinta el calendario.
     * * Útil para reflejar cambios tras guardar una configuración de precios.
     */
    public void reloadData() {
        refreshCalendar();
    }

    /**
     * Configura la estructura del panel (Cabecera de navegación + Rejilla de días).
     */
    private void initComponents() {
        
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        btnPrev = createNavButton("<");
        btnNext = createNavButton(">");
        lblMonthYear = new JLabel("", SwingConstants.CENTER);
        lblMonthYear.setFont(new Font("Segoe UI", Font.BOLD, 16));

        btnPrev.addActionListener(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            refreshCalendar();
        });

        btnNext.addActionListener(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            refreshCalendar();
        });

        headerPanel.add(btnPrev, BorderLayout.WEST);
        headerPanel.add(lblMonthYear, BorderLayout.CENTER);
        headerPanel.add(btnNext, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        daysPanel = new JPanel(new GridLayout(0, 7, 3, 3)); 
        daysPanel.setBackground(Color.WHITE);
        
        add(daysPanel, BorderLayout.CENTER);
    }

    private JButton createNavButton(String text) {
        
        JButton btn = new JButton(text);
        
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(200,200,200)));
        btn.setPreferredSize(new Dimension(40, 30));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return btn;

    }

    /**
     * Lógica principal de renderizado del calendario.
     * * Pasos de implementación:
     * - Limpia el panel de días.
     * - Consulta a la base de datos el estado (Reservado/Libre) y temporada de cada día del mes.
     * - Calcula los huecos vacíos al inicio de la semana para alinear los días correctamente.
     * - Itera día a día creando botones.
     * - Aplica lógica de colores con la siguiente prioridad:
     * 1. Pasado / Reservado (Gris, deshabilitado).
     * 2. Seleccionado por el usuario (Azul intenso).
     * 3. Temporada configurada (Verde/Amarillo/Rojo).
     * 4. Por defecto (Blanco).
     */
    private void refreshCalendar() {
        
        daysPanel.removeAll();

        if (currentVutId != -1) {
            currentMonthData = dbService.get_month_data(currentVutId, currentYearMonth.getYear(), currentYearMonth.getMonthValue() - 1);
        } else {
            currentMonthData.clear();
        }

        lblMonthYear.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, locale) + " " + currentYearMonth.getYear());

        DayOfWeek[] daysOfWeek = DayOfWeek.values(); 
        for (DayOfWeek d : daysOfWeek) {
            JLabel lbl = new JLabel(d.getDisplayName(TextStyle.SHORT, locale), SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(Color.GRAY);
            daysPanel.add(lbl);
        }

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int emptySlots = firstOfMonth.getDayOfWeek().getValue() - 1; 

        for (int i = 0; i < emptySlots; i++) {
            daysPanel.add(new JLabel(""));
        }

        int daysInMonth = currentYearMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        for (int day = 1; day <= daysInMonth; day++) {
            
            LocalDate date = currentYearMonth.atDay(day);
            JButton btnDay = new JButton(String.valueOf(day));

            btnDay.setFocusPainted(false);
            btnDay.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
            btnDay.setOpaque(true);
            
            boolean isPast = !date.isAfter(today);
            boolean isReserved = false;
            Color dayColor = SEASON_NONE;
            String tooltip = "No configurado";

            if (currentMonthData.containsKey(day)) {
                DayInfo info = currentMonthData.get(day);
                if (info != null) {

                    if ("reserved".equals(info.status()) || "paid".equals(info.status())) {
                        isReserved = true;
                    } 
                    
                    else {
                        if (info.season() != null) {
                            switch (info.season()) {
                                case "low": dayColor = SEASON_LOW; break;
                                case "average": dayColor = SEASON_AVG; break;
                                case "high": dayColor = SEASON_HIGH; break;
                            }
                            tooltip = "Configurado: " + info.season().toUpperCase();
                        }
                    }
                }
            }

            if (isPast) {
                btnDay.setEnabled(false);
                btnDay.setBackground(COLOR_PAST);
                btnDay.setForeground(Color.GRAY);
            } 
            else if (isReserved) {
                btnDay.setEnabled(false); 
                btnDay.setBackground(COLOR_RESERVED);
                btnDay.setForeground(Color.DARK_GRAY);
                btnDay.setToolTipText("Ocupado / Reservado");
                btnDay.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 2));
            } 
            else {
                btnDay.setCursor(new Cursor(Cursor.HAND_CURSOR));
                btnDay.addActionListener(createDayActionListener(date));
                btnDay.setBackground(dayColor);
                btnDay.setToolTipText(tooltip);

                styleButtonForSelection(btnDay, date);
            }
            
            if (date.equals(today)) {
                btnDay.setForeground(COLOR_TODAY_TEXT);
                btnDay.setFont(new Font("Segoe UI", Font.BOLD, 14));
            }

            daysPanel.add(btnDay);
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }

    /**
     * Aplica el estilo visual de "Selección" (Azul) sobre un botón específico.
     * * Si el día es el inicio o el fin del rango, se pinta azul oscuro.
     * * Si está dentro del rango, se pinta azul claro.
     */
    private void styleButtonForSelection(JButton btn, LocalDate date) {
        
        boolean isStart = selectedStartDate != null && date.equals(selectedStartDate);
        boolean isEnd = selectedEndDate != null && date.equals(selectedEndDate);

        if (isStart || isEnd) {
            btn.setBackground(COLOR_SELECTED);
            btn.setForeground(Color.WHITE);
            return;
        }

        if (selectedStartDate != null && selectedEndDate != null) {
            if (date.isAfter(selectedStartDate) && date.isBefore(selectedEndDate)) {
                btn.setBackground(COLOR_RANGE);
                btn.setForeground(Color.BLACK); 
            }
        }
    }

    private ActionListener createDayActionListener(LocalDate date) {
        return e -> {
            handleDateSelection(date);
            refreshCalendar(); 
        };
    }

    /**
     * Gestiona la lógica de clic para definir el rango.
     * * 1er Clic: Define fecha de inicio.
     * * 2do Clic: Define fecha de fin (si es posterior al inicio).
     * * 3er Clic: Reinicia la selección y establece nueva fecha de inicio.
     */
    private void handleDateSelection(LocalDate date) {
        if (selectedStartDate == null) {
            selectedStartDate = date;
            selectedEndDate = null; 
        } 
        else if (selectedEndDate == null) {
            if (date.isBefore(selectedStartDate)) {
                selectedStartDate = date;
            } else {
                selectedEndDate = date;
            }
        } 
        else {
            selectedStartDate = date;
            selectedEndDate = null;
        }
    }

    /**
     * @return La fecha de inicio del rango seleccionado.
     */
    public LocalDate getStartDate() {
        return selectedStartDate;
    }

    /**
     * @return La fecha de fin del rango. Si solo se ha seleccionado un día, devuelve la fecha de inicio.
     */
    public LocalDate getEndDate() {
        return (selectedEndDate == null) ? selectedStartDate : selectedEndDate;
    }
    
    /**
     * Limpia la selección actual del usuario.
     */
    public void resetSelection() {
        selectedStartDate = null;
        selectedEndDate = null;
        refreshCalendar();
    }
}