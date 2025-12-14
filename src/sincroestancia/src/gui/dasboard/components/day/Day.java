package sincroestancia.src.gui.dasboard.components.day;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.models.FullDayInfo;
import java.util.function.Consumer;
import sincroestancia.src.gui.components.ButtonUtils;
import sincroestancia.src.models.ReservationInfo;

/**
 * Panel de detalle lateral que muestra la información completa del día seleccionado.
 * * Actúa como un contenedor dinámico que cambia su contenido según el estado del día:
 * - Si está libre (futuro): Muestra botón para crear reserva.
 * - Si está reservado/pagado: Muestra el componente 'ReservationDetails' con datos del huésped.
 * - Si hay una salida (Check-out): Muestra el componente 'CheckoutDetails'.
 * - Si es pasado: Muestra advertencia de bloqueo.
 * * Utiliza el patrón Observer (vía Callbacks) para notificar al Dashboard de las acciones del usuario.
 * * @author Carlos Padilla Labella
 */
public class Day extends javax.swing.JPanel {

    private final DatabaseService db_service;
    private final Locale spanishLocale = new Locale("es", "ES");

    private javax.swing.JLabel price_label;
    private javax.swing.JLabel status_label;

    private javax.swing.JButton reserve_button;
    private Consumer<Date> onReserveAction;
    private ReservationDetails reservation_details_panel;
    private CheckoutDetails checkout_details_panel;
    private Runnable onDataChanged;
    private javax.swing.JLabel past_day_warning_label;
    private Consumer<ReservationInfo> onEditReservation;
    private int currentDay, currentMonth, currentYear, currentVutId;
    private Consumer<Integer> onCheckinRequest;
    private Consumer<Integer> onCheckoutRequest;

    /**
     * Constructor.
     * * Configura un Layout vertical (BoxLayout Y_AXIS) dentro de un ScrollPane
     * para asegurar que todo el contenido sea accesible incluso en pantallas pequeñas.
     * * Inicializa y oculta los sub-paneles (ReservationDetails y CheckoutDetails) hasta que sean necesarios.
     */
    public Day() {
        
        initComponents();

        this.db_service = new DatabaseService();

        this.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(getBackground());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(getBackground());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        reserve_button = new JButton("Registrar reserva");
        reserve_button.setAlignmentX(Component.CENTER_ALIGNMENT);
        reserve_button.setVisible(false);
        reserve_button.setMargin(new java.awt.Insets(8, 25, 8, 25));
        reserve_button.setFocusPainted(false);
        reserve_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ButtonUtils.stylePrimary(reserve_button);

        past_day_warning_label = new javax.swing.JLabel("No se pueden registrar reservas en fechas pasadas.");
        past_day_warning_label.setAlignmentX(Component.CENTER_ALIGNMENT);
        past_day_warning_label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.ITALIC, 12));
        past_day_warning_label.setForeground(Color.RED);
        past_day_warning_label.setVisible(false);

        selected_day_label.setAlignmentX(Component.CENTER_ALIGNMENT);
        price_label.setAlignmentX(Component.CENTER_ALIGNMENT);
        status_label.setAlignmentX(Component.CENTER_ALIGNMENT);

        selected_day_label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        price_label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        status_label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));

        contentPanel.add(selected_day_label);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(price_label);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(status_label);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(reserve_button);

        contentPanel.add(past_day_warning_label);

        reservation_details_panel = new ReservationDetails();
        reservation_details_panel.setVisible(false);
        contentPanel.add(reservation_details_panel);

        checkout_details_panel = new CheckoutDetails();
        checkout_details_panel.setVisible(false);

        checkout_details_panel.setOnEditAction((info) -> {
            if (this.onEditReservation != null) {
                this.onEditReservation.accept(info);
            }
        });

        checkout_details_panel.setOnCheckoutAction((reservationId) -> {
            if (this.onCheckoutRequest != null) {
                this.onCheckoutRequest.accept(reservationId);
            }
        });

        contentPanel.add(checkout_details_panel);

        scrollPane.setViewportView(contentPanel);
        this.add(scrollPane, BorderLayout.CENTER);

        reserve_button.addActionListener(e -> {
            if (onReserveAction != null) {
                GregorianCalendar cal = new GregorianCalendar(currentYear, currentMonth, currentDay);
                Date date = cal.getTime();
                onReserveAction.accept(date);
            }
        });

    }

    public void setOnReserveAction(Consumer<Date> action) {
        this.onReserveAction = action;
    }

    /**
     * Método principal de actualización llamado por el Calendario al seleccionar un día.
     * * Lógica de decisión (Máquina de estados):
     * 1. Limpia la UI (oculta todos los paneles opcionales).
     * 2. Consulta si hay un Checkout ese día -> Muestra `CheckoutDetails`.
     * 3. Consulta el estado del día (Reserved/Paid/Free):
     * - Reserved/Paid: Muestra `ReservationDetails`.
     * - Free: Comprueba si es fecha futura. Si sí, muestra botón "Registrar". Si no, muestra advertencia.
     * * @param day    Día del mes (1-31).
     * @param month  Mes (0-11).
     * @param year   Año.
     * @param vut_id ID de la vivienda seleccionada.
     */
    public void updateDayInfo(int day, int month, int year, int vut_id) {

        reserve_button.setVisible(false);
        past_day_warning_label.setVisible(false);
        reservation_details_panel.setVisible(false);
        checkout_details_panel.setVisible(false);

        this.currentDay = day;
        this.currentMonth = month;
        this.currentYear = year;
        this.currentVutId = vut_id;

        if (vut_id <= 0) {
            selected_day_label.setText("Seleccione un VUT");
            price_label.setText("");
            status_label.setText("");
            return;
        }

        GregorianCalendar cal = new GregorianCalendar(year, month, day);
        Date date_obj = cal.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("d 'de' MMMM 'de' yyyy", spanishLocale);
        String formattedDate = sdf.format(date_obj);
        selected_day_label.setText(formattedDate);

        FullDayInfo dayInfo = db_service.get_full_day_details(vut_id, date_obj);

        ReservationInfo checkoutInfo = db_service.get_reservation_by_checkout_date(vut_id, date_obj);

        if (checkoutInfo != null) {
            checkout_details_panel.updateDetails(checkoutInfo, spanishLocale);
            checkout_details_panel.setVisible(true);
        }

        if (dayInfo != null) {

            price_label.setText("(" + String.format(spanishLocale, "%.2f", dayInfo.price()) + " €)");

            String statusText = "Libre";
            Color statusColor = new Color(0, 140, 0);

            switch (dayInfo.status()) {
                case "reserved":
                    statusText = "Reservado";
                    statusColor = new Color(200, 0, 0);
                    loadAndShowReservationDetails(vut_id, date_obj);
                    break;
                case "paid":
                    statusText = "Pagado";
                    statusColor = new Color(0, 0, 200);
                    loadAndShowReservationDetails(vut_id, date_obj);
                    break;
                case "free":
                    GregorianCalendar todayCal = new GregorianCalendar();
                    todayCal.set(GregorianCalendar.HOUR_OF_DAY, 0);
                    todayCal.set(GregorianCalendar.MINUTE, 0);
                    todayCal.set(GregorianCalendar.SECOND, 0);
                    todayCal.set(GregorianCalendar.MILLISECOND, 0);
                    Date todayAtMidnight = todayCal.getTime();

                    if (!date_obj.before(todayAtMidnight)) {
                        reserve_button.setVisible(true);
                    } else {
                        past_day_warning_label.setVisible(true);
                    }

                    if (checkoutInfo != null) {
                        statusText = "Salida / Libre";
                        statusColor = new Color(217, 119, 6);
                    }

                    break;
                default:
                    break;
            }
            status_label.setText(statusText);
            status_label.setForeground(statusColor);

        } else {
            price_label.setText("(Sin precio)");
            status_label.setText("Sin datos");
            status_label.setForeground(new java.awt.Color(102, 102, 102));
        }
    }

    public void setOnDataChanged(Runnable action) {
        this.onDataChanged = action;
    }

    public void setOnEditReservation(Consumer<ReservationInfo> action) {
        this.onEditReservation = action;
    }

    public void setOnCheckinRequest(Consumer<Integer> action) {
        this.onCheckinRequest = action;
    }

    public void setOnCheckoutRequest(Consumer<Integer> action) {
        this.onCheckoutRequest = action;
    }

    public void refresh_day_data() {
        updateDayInfo(this.currentDay, this.currentMonth, this.currentYear, this.currentVutId);
    }

    /**
     * Carga los datos detallados de la reserva y configura los listeners del sub-panel.
     * * Encapsula la lógica de propagación de eventos: cuando ocurre algo en ReservationDetails
     * (ej. pago cambiado), se avisa a 'onDataChanged' para que el Dashboard refresque todo.
     */
    private void loadAndShowReservationDetails(int vut_id, Date date) {

        ReservationInfo resInfo = db_service.get_reservation_details_for_day(vut_id, date);
        reservation_details_panel.updateDetails(resInfo, spanishLocale, vut_id);

        reservation_details_panel.setOnPaymentChanged(() -> {
            System.out.println("[Day] Recibido aviso de 'ReservationDetails'.");

            if (onDataChanged != null) {
                System.out.println("[Day] Avisando a Dashboard para refrescar calendario...");
                onDataChanged.run();
            }

            System.out.println("[Day] Refrescando panel 'Day'...");
            updateDayInfo(this.currentDay, this.currentMonth, this.currentYear, this.currentVutId);
        });

        reservation_details_panel.setOnEditAction((infoToEdit) -> {
            System.out.println("[Day] Recibida petición de Edición. Avisando a Dashboard...");
            if (this.onEditReservation != null) {
                this.onEditReservation.accept(infoToEdit);
            }
        });

        reservation_details_panel.setOnCheckinAction((reservationId) -> {
            if (this.onCheckinRequest != null) {
                this.onCheckinRequest.accept(reservationId);
            }
        });
        
        reservation_details_panel.setVisible(true);
    }

    private void initComponents() {

        selected_day_label = new javax.swing.JLabel();
        price_label = new javax.swing.JLabel();
        status_label = new javax.swing.JLabel();

        setBackground(new java.awt.Color(250, 250, 250));

        selected_day_label.setForeground(new java.awt.Color(51, 51, 51));
        selected_day_label.setText("Seleccione un día");

        price_label.setForeground(new java.awt.Color(102, 102, 102));
        price_label.setText("(precio)");

        status_label.setForeground(new java.awt.Color(102, 102, 102));
        status_label.setText("estado");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE));
    }

    private javax.swing.JLabel selected_day_label;
    
}