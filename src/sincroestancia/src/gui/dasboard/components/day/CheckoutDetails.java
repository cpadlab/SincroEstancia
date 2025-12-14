package sincroestancia.src.gui.dasboard.components.day;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import sincroestancia.src.gui.components.ButtonUtils;
import sincroestancia.src.models.ReservationInfo;

/**
 * Panel informativo específico para gestionar la salida (Check-out) de un huésped.
 * * Este componente se muestra en el panel lateral cuando se detecta que una reserva
 * finaliza en la fecha seleccionada.
 * * Características:
 * - Muestra datos resumen de la reserva.
 * - Estilo visual diferenciado con bordes en tono Ámbar/Naranja.
 * - Botón de acción inteligente que valida si es el momento adecuado para el checkout.
 * * @author Carlos Padilla Labella
 */
public class CheckoutDetails extends JPanel {

    private JLabel guest_name_label;
    private JLabel guest_dates_label;
    private JLabel guest_pax_label;
    private JButton edit_button;
    private JButton checkout_button;

    private ReservationInfo currentReservation;
    private Consumer<ReservationInfo> onEditAction;
    private Consumer<Integer> onCheckoutAction;

    public CheckoutDetails() {
        initComponents();
    }

    /**
     * Inicialización de la interfaz gráfica.
     * * Configura un panel con borde titulado en color naranja para distinguirlo
     * visualmente de las entradas (Check-ins).
     */
    private void initComponents() {
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new java.awt.Color(250, 250, 250));

        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(245, 158, 11)),
                "Salida de Huésped (Check-out)",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12),
                new Color(180, 83, 9));

        card.setBorder(BorderFactory.createCompoundBorder(
                titledBorder,
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        java.awt.Font labelFont = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12);

        guest_name_label = new JLabel("Huésped: ");
        guest_name_label.setFont(labelFont);
        guest_name_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        guest_dates_label = new JLabel("Estancia: ");
        guest_dates_label.setFont(labelFont);
        guest_dates_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        guest_pax_label = new JLabel("Personas: ");
        guest_pax_label.setFont(labelFont);
        guest_pax_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(guest_name_label);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(guest_dates_label);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(guest_pax_label);
        card.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel btnContainer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        btnContainer.setBackground(card.getBackground());
        btnContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        edit_button = new JButton("Editar Reserva");
        edit_button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        edit_button.setFocusable(false);
        edit_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        edit_button.setMargin(new java.awt.Insets(8, 25, 8, 25));
        ButtonUtils.styleSecondary(edit_button);

        edit_button.addActionListener(e -> {
            if (this.currentReservation != null && this.onEditAction != null) {
                onEditAction.accept(this.currentReservation);
            }
        });

        checkout_button = new JButton("Hacer Checkout");
        checkout_button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        checkout_button.setFocusable(false);
        checkout_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        checkout_button.setMargin(new java.awt.Insets(8, 25, 8, 25));
        checkout_button.setEnabled(false);
        ButtonUtils.stylePrimary(checkout_button);

        checkout_button.addActionListener(e -> {
            if (this.currentReservation != null && this.onCheckoutAction != null) {
                onCheckoutAction.accept(this.currentReservation.id());
            }
        });

        btnContainer.add(edit_button);
        btnContainer.add(Box.createRigidArea(new Dimension(10, 0)));
        btnContainer.add(checkout_button);
        
        card.add(btnContainer);

        add(card);

    }

    /**
     * Define la acción a ejecutar cuando se pulsa "Editar Reserva".
     */
    public void setOnEditAction(Consumer<ReservationInfo> action) {
        this.onEditAction = action;
    }

    /**
     * Define la acción a ejecutar cuando se pulsa "Hacer Checkout".
     */
    public void setOnCheckoutAction(Consumer<Integer> action) {
        this.onCheckoutAction = action;
    }

    /**
     * Actualiza la información visual y lógica del panel basándose en la reserva seleccionada.
     * * Implementa una regla de negocio importante:
     * - Si el checkout ya está hecho -> Muestra botón verde deshabilitado ("Completado").
     * - Si no está hecho -> Verifica la fecha. Solo habilita el botón si la fecha de salida
     * es hoy, fue en el pasado, o es dentro de los próximos 3 días.
     * * @param resInfo Objeto con los datos de la reserva.
     * @param locale Configuración regional para formateo de fechas.
     */
    public void updateDetails(ReservationInfo resInfo, Locale locale) {
        
        this.currentReservation = resInfo;

        if (resInfo != null) {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", locale);

            String checkInDisplay = resInfo.checkIn();
        
            String checkOutDisplay = resInfo.checkOut();
            try {
                checkInDisplay = formatter.format(parser.parse(checkInDisplay));
                checkOutDisplay = formatter.format(parser.parse(checkOutDisplay));
            } catch (Exception e) { }

            guest_name_label.setText("Huésped: " + resInfo.guestName());
            guest_dates_label.setText("Estancia: " + checkInDisplay + " al " + checkOutDisplay);
            guest_pax_label.setText("Personas: " + resInfo.pax());

            if (resInfo.hasCheckout()) {
                checkout_button.setText("Checkout Completado");
                checkout_button.setBackground(new Color(230, 255, 230));
                checkout_button.setForeground(new Color(0, 100, 0));
                checkout_button.setEnabled(false);
                
            } else {
                
                checkout_button.setText("Hacer Checkout");
                checkout_button.setForeground(Color.WHITE);
                ButtonUtils.stylePrimary(checkout_button);
                
                try {
                    
                    LocalDate checkoutDate = LocalDate.parse(resInfo.checkOut(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    LocalDate today = LocalDate.now();
                    long daysUntilCheckout = ChronoUnit.DAYS.between(today, checkoutDate);

                    boolean canCheckout = daysUntilCheckout <= 3; 
                    checkout_button.setEnabled(canCheckout);
                    
                    if (!canCheckout) {
                        checkout_button.setToolTipText("Disponible 3 días antes de la salida");
                    } else {
                        checkout_button.setToolTipText(null);
                    }
                    
                } catch (Exception e) {
                    checkout_button.setEnabled(true);
                }

            }
        }
    }
}