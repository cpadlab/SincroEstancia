package sincroestancia.src.gui.dasboard.components.day;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.gui.components.ButtonUtils;
import javax.swing.JButton;

import java.util.function.Consumer;
import sincroestancia.src.models.ReservationInfo;

/**
 * Un panel dedicado a mostrar los detalles de una reserva existente.
 * 
 * @author Carlos Padilla Labella
 */
public class ReservationDetails extends JPanel {

    private DatabaseService db_service;
    private javax.swing.JCheckBox paid_checkbox;

    private int current_reservation_id = -1;
    private int current_vut_id = -1;
    private String check_in_date_sql;
    private String check_out_date_sql;

    private JButton edit_button;
    private JButton checkin_button;

    private Consumer<ReservationInfo> onEditAction;
    private ReservationInfo currentReservation;
    private Consumer<Integer> onCheckinAction;

    private JLabel guest_name_label;
    private JLabel guest_dni_label;
    private JLabel guest_contact_label;
    private JLabel guest_dates_label;
    private JLabel guest_pax_label;

    private Runnable onPaymentChanged;

    public ReservationDetails() {
        initComponents();
        this.db_service = new DatabaseService();
    }

    private void initComponents() {
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new java.awt.Color(250, 250, 250));
        setAlignmentX(Component.CENTER_ALIGNMENT);

        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                "Detalles de la Reserva",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12),
                new Color(51, 51, 51));

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(15, 0, 0, 0),
                BorderFactory.createCompoundBorder(
                        titledBorder,
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                )));

        java.awt.Font labelFont = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12);
        guest_name_label = new JLabel("Huésped: ");
        guest_name_label.setFont(labelFont);
        guest_name_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        guest_dni_label = new JLabel("DNI: ");
        guest_dni_label.setFont(labelFont);
        guest_dni_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        guest_contact_label = new JLabel("Contacto: ");
        guest_contact_label.setFont(labelFont);
        guest_contact_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        guest_dates_label = new JLabel("Estancia: ");
        guest_dates_label.setFont(labelFont);
        guest_dates_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        guest_pax_label = new JLabel("Personas: ");
        guest_pax_label.setFont(labelFont);
        guest_pax_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(guest_name_label);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(guest_dni_label);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(guest_contact_label);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(guest_dates_label);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(guest_pax_label);
        add(Box.createRigidArea(new Dimension(0, 10)));

        paid_checkbox = new javax.swing.JCheckBox("Reserva Pagada");
        paid_checkbox.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        paid_checkbox.setBackground(getBackground());
        paid_checkbox.setFocusable(false);
        paid_checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        paid_checkbox.addActionListener(e -> handlePaymentChange());

        add(paid_checkbox);

        add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnContainer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        btnContainer.setBackground(getBackground());
        btnContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        checkin_button = new JButton("Hacer Check-in");
        checkin_button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        checkin_button.setFocusable(false);
        checkin_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        checkin_button.setMargin(new java.awt.Insets(8, 25, 8, 25));
        checkin_button.setEnabled(false);
        ButtonUtils.stylePrimary(checkin_button);

        checkin_button.addActionListener(e -> {
            if (this.current_reservation_id != -1 && this.onCheckinAction != null) {
                System.out.println("[ReservationDetails] Solicitando Check-in para ID: " + this.current_reservation_id);
                onCheckinAction.accept(this.current_reservation_id);
            }
        });

        btnContainer.add(checkin_button);
        btnContainer.add(Box.createRigidArea(new Dimension(10, 0)));

        edit_button = new JButton("Editar Reserva");
        edit_button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        edit_button.setAlignmentX(Component.CENTER_ALIGNMENT);
        edit_button.setFocusable(false);
        edit_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        edit_button.setMargin(new java.awt.Insets(8, 25, 8, 25));

        ButtonUtils.styleSecondary(edit_button);
        edit_button.addActionListener(e -> {
            if (this.currentReservation != null && this.onEditAction != null) {
                onEditAction.accept(this.currentReservation);
            }
        });

        btnContainer.add(edit_button);
        add(btnContainer);
    }

    public void setOnPaymentChanged(Runnable action) {
        this.onPaymentChanged = action;
    }

    public void setOnEditAction(Consumer<ReservationInfo> action) {
        this.onEditAction = action;
    }

    public void setOnCheckinAction(Consumer<Integer> action) {
        this.onCheckinAction = action;
    }

    private void handlePaymentChange() {
        if (this.current_reservation_id == -1)
            return;

        boolean isNowPaid = paid_checkbox.isSelected();

        boolean success = db_service.update_reservation_payment_status(
                this.current_reservation_id,
                isNowPaid,
                this.current_vut_id,
                this.check_in_date_sql,
                this.check_out_date_sql);

        if (success) {
            System.out.println("[DetailsPanel] Pago actualizado. Avisando al panel 'Day'...");
            if (onPaymentChanged != null) {
                onPaymentChanged.run();
            }
        } else {
            System.err.println("[DetailsPanel] Error al actualizar el pago. Revirtiendo checkbox.");
            paid_checkbox.setSelected(!isNowPaid);
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al actualizar el estado del pago.",
                    "Error de Base de Datos",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateDetails(ReservationInfo resInfo, Locale spanishLocale, int vutId) {
        this.current_reservation_id = -1;
        this.currentReservation = null;

        if (resInfo != null) {

            this.current_reservation_id = resInfo.id();
            this.current_vut_id = vutId;
            this.currentReservation = resInfo;
            this.check_in_date_sql = resInfo.checkIn();
            this.check_out_date_sql = resInfo.checkOut();

            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", spanishLocale);
            String checkInDisplay = resInfo.checkIn();
            String checkOutDisplay = resInfo.checkOut();

            try {
                checkInDisplay = formatter.format(parser.parse(checkInDisplay));
                checkOutDisplay = formatter.format(parser.parse(checkOutDisplay));
            } catch (java.text.ParseException e) {
                System.err.println("[ReservationDetails] Error al parsear fechas de reserva.");
            }

            guest_name_label.setText("Huésped: " + resInfo.guestName());
            guest_dni_label.setText("DNI: " + resInfo.guestDni());
            guest_contact_label
                    .setText("<html>Contacto: " + resInfo.guestEmail() + "<br> | " + resInfo.guestPhone() + "</html>");
            guest_dates_label.setText("Estancia: " + checkInDisplay + " al " + checkOutDisplay);

            guest_pax_label.setText("Personas: " + resInfo.pax());

            paid_checkbox.setSelected(resInfo.isPaid());

            try {
                
                java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
                java.time.LocalDate today = java.time.LocalDate.now();

                if (resInfo.hasCheckin()) {
                    checkin_button.setEnabled(false);
                    checkin_button.setText("Check-in Realizado");
                    checkin_button.setToolTipText("El registro de viajeros ya se ha completado.");
                } else {
                    checkin_button.setText("Hacer Check-in");

                    java.time.LocalDate checkInDate = java.time.LocalDate.parse(this.check_in_date_sql, dtf);

                    long daysUntilCheckin = java.time.temporal.ChronoUnit.DAYS.between(today, checkInDate);

                    if (daysUntilCheckin >= 3) {
                        checkin_button.setEnabled(false);
                        checkin_button.setToolTipText("Disponible 3 días antes de la llegada");
                    } else {
                        checkin_button.setEnabled(true);
                        checkin_button.setToolTipText(null);
                    }
                }

                java.time.LocalDate checkOutDate = java.time.LocalDate.parse(this.check_out_date_sql, dtf);

                if (checkOutDate.isBefore(today)) {
                    edit_button.setEnabled(false);
                    edit_button.setToolTipText("No se pueden editar reservas finalizadas.");
                } else {
                    edit_button.setEnabled(true);
                    edit_button.setToolTipText("Modificar datos de la reserva");
                }

            } catch (Exception e) {
                System.err.println("[ReservationDetails] Error calculando fechas: " + e.getMessage());
                checkin_button.setEnabled(true);
                edit_button.setEnabled(true);
            }

        } else {
            guest_name_label.setText("Error: No se encontraron los datos de la reserva asociada.");
            guest_dni_label.setText("");
            guest_contact_label.setText("");
            guest_dates_label.setText("");
            guest_pax_label.setText("");
            paid_checkbox.setSelected(false);
            checkin_button.setEnabled(false);
            edit_button.setEnabled(false);
        }
    }
}