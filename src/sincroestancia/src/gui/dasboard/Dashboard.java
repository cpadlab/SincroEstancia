package sincroestancia.src.gui.dasboard;

import sincroestancia.src.gui.dasboard.components.Calendar;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import sincroestancia.src.gui.components.Drawer;
import sincroestancia.src.gui.dasboard.components.day.Day;
import sincroestancia.src.gui.dasboard.components.day.RegisterDay;
import sincroestancia.src.gui.dasboard.components.process.CheckinForm;
import sincroestancia.src.gui.dasboard.components.process.CheckoutForm;
import sincroestancia.src.gui.components.Modal;
import sincroestancia.src.models.ReservationInfo;

/**
 * Panel principal (Dashboard) que coordina la vista general de la aplicación.
 * * Estructura visual:
 * - Panel Izquierdo: Calendario mensual (Calendar).
 * - Panel Derecho: Detalles del día seleccionado (Day).
 * * Funcionalidad Clave:
 * - Actúa como orquestador de eventos entre los sub-componentes.
 * - Gestiona los contenedores deslizantes (Drawers) para formularios de Registro, Check-in y Check-out.
 * - Gestiona el Modal de bloqueo para sub-tareas críticas (ej. añadir huéspedes).
 * * @author Carlos Padilla Labella
 */
public class Dashboard extends javax.swing.JPanel {

    private final Day day_panel;
    private Calendar calendar_panel;
    private RegisterDay register_day_form_panel;
    private Drawer register_day_drawer;
    private CheckinForm checkin_form_panel;
    private Drawer checkin_drawer;
    private CheckoutForm checkout_form_panel;
    private Drawer checkout_drawer;
    private Modal modal;

    private int vut_id = -1;

    /**
     * Constructor del Dashboard.
     * * Inicializa los componentes y configura el "cableado" (wiring) de los eventos.
     * Define qué sucede cuando un componente hijo solicita una acción (ej. terminar un check-in).
     */
    public Dashboard() {
        initComponents();

        left_panel.setLayout(new BorderLayout());
        right_panel.setLayout(new BorderLayout());

        day_panel = new Day();

        day_panel.setOnDataChanged(() -> {
            System.out.println("[Dashboard] Recibido aviso de 'Day'. Refrescando calendario...");
            if (calendar_panel != null) {
                calendar_panel.update_calendar();
            }
        });

        day_panel.setOnEditReservation((resInfo) -> {
            System.out.println("[Dashboard] Recibida petición de Edición. Abriendo Drawer...");
            openDrawerForEdit(resInfo);
        });

        modal = new Modal();
        checkin_form_panel = new CheckinForm();

        checkin_drawer = new Drawer(checkin_form_panel);

        checkin_form_panel.setOnOpenModalRequest((component) -> {
            modal.show(component);
        });

        checkin_form_panel.setOnCloseModalRequest(() -> {
            modal.close();
        });

        checkin_form_panel.setOnCancel(() -> {
            checkin_drawer.close();
        });

        checkin_form_panel.setOnFinish(() -> {
            System.out.println("[Dashboard] Check-in finalizado. Recargando...");
            checkin_drawer.close();
            if (calendar_panel != null) {
                calendar_panel.update_calendar();
            }
            day_panel.refresh_day_data();
        });

        checkout_form_panel = new CheckoutForm();
        checkout_drawer = new Drawer(checkout_form_panel);

        checkout_form_panel.setOnCancel(() -> {
            checkout_drawer.close();
        });

        checkout_form_panel.setOnFinish(() -> {
            System.out.println("[Dashboard] Checkout finalizado. Recargando...");
            checkout_drawer.close();
            if (calendar_panel != null) {
                calendar_panel.update_calendar();
            }
            day_panel.refresh_day_data();
        });

        day_panel.setOnCheckinRequest((reservationId) -> {
            System.out.println("[Dashboard] Abriendo formulario Check-in para reserva: " + reservationId);
            checkin_form_panel.loadReservationData(reservationId);
            checkin_drawer.open();
        });

        day_panel.setOnCheckoutRequest((reservationId) -> {
            System.out.println("[Dashboard] Abriendo formulario Checkout para reserva: " + reservationId);
            checkout_form_panel.loadReservationData(reservationId);
            checkout_drawer.open();
        });

        calendar_panel = new Calendar(day_panel, vut_id);
        register_day_form_panel = new RegisterDay();

        javax.swing.JScrollPane register_day_form_scroll_area = new javax.swing.JScrollPane(register_day_form_panel);
        register_day_form_scroll_area.setBorder(null);
        register_day_form_scroll_area.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        register_day_form_scroll_area.getVerticalScrollBar().setUnitIncrement(16);

        register_day_drawer = new Drawer(register_day_form_scroll_area);

        day_panel.setOnReserveAction((date) -> {
            register_day_form_panel.setStartDate(date);
            register_day_drawer.open();
        });

        register_day_form_panel.setOnReservationSuccess(() -> {
            System.out.println("[Dashboard] Recibida señal de éxito. Refrescando calendario...");
            calendar_panel.update_calendar();
            register_day_drawer.close();
        });

        left_panel.add(calendar_panel, BorderLayout.CENTER);
        right_panel.add(this.day_panel, BorderLayout.CENTER);

    }

    /**
     * Método del ciclo de vida de Swing.
     * * Se ejecuta cuando este panel se añade a un componente padre.
     * * Es CRÍTICO para los Drawers y Modals: Aquí buscamos el JFrame principal (Main)
     * y "pegamos" (attach) los componentes flotantes a su JLayeredPane.
     * * Si no se hace esto aquí, los drawers no tendrían dónde dibujarse por encima del contenido.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (parentFrame != null) {
            register_day_drawer.attachToFrame(parentFrame);
            checkin_drawer.attachToFrame(parentFrame);
            checkout_drawer.attachToFrame(parentFrame);
            modal.attachToFrame(parentFrame);
        }
    }

    /**
     * Método auxiliar para preparar el formulario de registro en modo edición.
     */
    private void openDrawerForEdit(ReservationInfo resInfo) {
        if (register_day_form_panel != null) {
            register_day_form_panel.loadForEdit(resInfo);
            register_day_drawer.open();
        }
    }

    /**
     * Recibe el ID de la vivienda seleccionada desde la clase Main.
     * * Propaga este ID a todos los sub-componentes (Calendario, Formularios) para
     * que sepan qué datos cargar.
     * * @param new_vut_id El ID de la vivienda seleccionada.
     */
    public void updateVutData(int new_vut_id) {
        if (new_vut_id < 0) {
            System.out.println("[Dashboard] No VUT selected.");
        } else {
            this.vut_id = new_vut_id;
            if (calendar_panel != null) {
                calendar_panel.setVutId(new_vut_id);
            }
            if (register_day_form_panel != null) {
                register_day_form_panel.setVutId(new_vut_id);
            }
            System.out.println("[Dashboard] Updating with VUT ID: " + new_vut_id);
        }
    }

    private void initComponents() {

        left_panel = new javax.swing.JPanel();
        right_panel = new javax.swing.JPanel();

        setLayout(new java.awt.GridLayout(1, 2));

        left_panel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout left_panelLayout = new javax.swing.GroupLayout(left_panel);
        left_panel.setLayout(left_panelLayout);
        left_panelLayout.setHorizontalGroup(
                left_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 500, Short.MAX_VALUE));
        left_panelLayout.setVerticalGroup(
                left_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 500, Short.MAX_VALUE));

        add(left_panel);

        right_panel.setBackground(new java.awt.Color(250, 250, 250));
        right_panel.setLayout(new java.awt.BorderLayout());
        add(right_panel);
    }
    
    private javax.swing.JPanel left_panel;
    private javax.swing.JPanel right_panel;

}
