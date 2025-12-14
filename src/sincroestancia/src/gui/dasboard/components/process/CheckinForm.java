package sincroestancia.src.gui.dasboard.components.process;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Component;
import java.util.List; // Import añadido
import java.util.Map; // Import añadido
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.gui.components.ButtonUtils;
import sincroestancia.src.models.ReservationInfo;
import sincroestancia.src.models.GuestData;

/**
 * Panel de formulario para realizar el proceso de Check-in (Entrada de huéspedes).
 * * Gestiona el flujo completo de registro:
 * - Visualización de datos de la reserva.
 * - Listado y adición de huéspedes (Parte de Entrada).
 * - Firma digital (simulada) y aceptación de normas/GDPR.
 * - Registro de datos financieros (Tarjeta, método de pago).
 * * Interactúa con un componente 'Modal' externo para los sub-formularios.
 * * @author Carlos Padilla Labella
 */
public class CheckinForm extends JPanel {

    private JLabel stepIndicatorLabel;
    private JPanel mainContentCardPanel;
    private CardLayout cardLayout;
    private JPanel guestsListContainer;
    private JButton btnBack, btnNext, btnFinish;

    private int currentStep = 1;
    private final int TOTAL_STEPS = 3;

    private ReservationInfo reservationInfo; 
    private java.util.Map<String, String> firstGuestAddressCache = null; 

    private int currentReservationId = -1;
    private int currentCheckinId = -1; 

    private Runnable onCancel, onFinish;
    private Consumer<Component> onOpenModalRequest;
    private Runnable onCloseModalRequest;

    /**
     * Constructor del formulario de Check-in.
     * * Inicializa los servicios y construye la interfaz gráfica base (Header, Listado, Footer).
     */
    public CheckinForm() {
        initComponents();
        updateUIState();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        headerPanel.setPreferredSize(new Dimension(0, 70));

        JPanel headerInner = new JPanel(new java.awt.GridLayout(2, 1));
        headerInner.setBackground(Color.WHITE);
        headerInner.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel titleLabel = new JLabel("Registro de Viajeros (Check-in)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        stepIndicatorLabel = new JLabel("Fase 1 de 3: Identificación Personal");
        stepIndicatorLabel.setForeground(Color.GRAY);

        headerInner.add(titleLabel);
        headerInner.add(stepIndicatorLabel);
        headerPanel.add(headerInner, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainContentCardPanel = new JPanel(cardLayout);
        mainContentCardPanel.setBackground(Color.WHITE);

        mainContentCardPanel.add(createStep1Panel(), "STEP_1");
        mainContentCardPanel.add(createStep2Panel(), "STEP_2");
        mainContentCardPanel.add(createStep3Panel(), "STEP_3");

        add(mainContentCardPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setPreferredSize(new Dimension(0, 80));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.setMargin(new java.awt.Insets(8, 25, 8, 25));
        styleButtonSecondary(btnCancel);
        ButtonUtils.styleSecondary(btnCancel);
        btnCancel.addActionListener(e -> {
            if (onCancel != null)
                onCancel.run();
        });

        JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 20));
        rightFooter.setOpaque(false);

        btnBack = new JButton("Atrás");
        btnBack.setMargin(new java.awt.Insets(8, 25, 8, 25));
        styleButtonSecondary(btnBack);
        ButtonUtils.styleSecondary(btnBack);
        btnBack.addActionListener(e -> navigateBack());

        btnNext = new JButton("Siguiente");
        btnNext.setMargin(new java.awt.Insets(8, 25, 8, 25));
        styleButtonPrimary(btnNext);
        ButtonUtils.stylePrimary(btnNext);
        btnNext.addActionListener(e -> navigateNext());

        btnFinish = new JButton("Finalizar");
        btnFinish.setMargin(new java.awt.Insets(8, 25, 8, 25));
        styleButtonPrimary(btnFinish);
        btnFinish.setBackground(new Color(0, 160, 0));
        ButtonUtils.styleSuccess(btnFinish);
        btnFinish.addActionListener(e -> {

            if (contractDataPanel != null && !contractDataPanel.isValidData()) {
                return;
            }

            if (financialDataPanel != null && contractDataPanel != null) {
                DatabaseService db = new DatabaseService();

                int payerId = financialDataPanel.getPayerId();
                String payerName = "Desconocido";
                if (payerId != -1) {
                    Map<String, Object> payerData = db.get_guest_details(payerId);
                    if (payerData != null) {
                        payerName = payerData.get("fullname") + " " + payerData.get("surname1") +
                                " (" + payerData.get("docNumber") + ")";
                    }
                }

                boolean success = db.finalize_checkin(
                        this.currentCheckinId,
                        financialDataPanel.getPaymentMethod(),
                        financialDataPanel.getPaymentId(),
                        payerName,
                        financialDataPanel.getCardExpiry(),
                        financialDataPanel.getPaymentDate(),
                        true, 
                        true 
                );

                if (success) {
                    JOptionPane.showMessageDialog(this, "Check-in finalizado y guardado correctamente.");
                    if (onFinish != null)
                        onFinish.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al guardar los datos finales.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        rightFooter.add(btnBack);
        rightFooter.add(btnNext);
        rightFooter.add(btnFinish);
        JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        leftFooter.setOpaque(false);
        leftFooter.add(btnCancel);

        footerPanel.add(leftFooter, BorderLayout.WEST);
        footerPanel.add(rightFooter, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private JButton btnAddGuest;

    private JPanel createStep1Panel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);

        guestsListContainer = new JPanel();
        guestsListContainer.setLayout(new BoxLayout(guestsListContainer, BoxLayout.Y_AXIS));
        guestsListContainer.setBackground(Color.WHITE);
        guestsListContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        guestsListContainer.add(new JLabel("No hay huéspedes registrados aún."));

        JScrollPane scroll = new JScrollPane(guestsListContainer);
        scroll.setBorder(null);
        p.add(scroll, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        actionPanel.setBackground(new Color(240, 249, 255));
        actionPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 230, 255)));

        btnAddGuest = new JButton("+ Añadir Huésped");
        btnAddGuest.setMargin(new java.awt.Insets(8, 25, 8, 25));
        styleButtonPrimary(btnAddGuest);
        ButtonUtils.styleSecondary(btnAddGuest);

        btnAddGuest.addActionListener(e -> openExternalGuestModal(-1));

        actionPanel.add(btnAddGuest);
        actionPanel.add(new JLabel("Añada a TODOS los viajeros."));
        p.add(actionPanel, BorderLayout.NORTH);

        return p;
    }

    private void openExternalGuestModal(int guestIdToEdit) {
        if (onOpenModalRequest == null)
            return;

        GuestFormPanel guestForm = new GuestFormPanel();
        DatabaseService db = new DatabaseService();

        List<GuestData> adults = db.get_adults_by_checkin(this.currentCheckinId);
        guestForm.setPotentialGuardians(adults);

        if (guestIdToEdit == -1) {
            guestForm.clearForm();

            List<GuestData> existingGuests = db.get_guests_by_checkin(this.currentCheckinId);

            if (existingGuests.isEmpty()) {
                
                if (this.reservationInfo != null) {
                    System.out.println("[CheckinForm] Auto-completando con datos del titular de la reserva.");
                    guestForm.setPreloadedData(
                            this.reservationInfo.guestName(),
                            this.reservationInfo.guestDni(),
                            this.reservationInfo.guestEmail(),
                            this.reservationInfo.guestPhone(),
                            null, null, null
                    );
                }
            } else {
                int firstGuestId = existingGuests.get(0).id();

                Map<String, Object> firstGuestFullData = db.get_guest_details(firstGuestId);

                if (firstGuestFullData != null) {
                    System.out.println(
                            "[CheckinForm] Auto-completando dirección del primer huésped (ID: " + firstGuestId + ")");
                    guestForm.setPreloadedData(
                            null, null, null, null,
                            (String) firstGuestFullData.get("address"),
                            (String) firstGuestFullData.get("city"),
                            (String) firstGuestFullData.get("country"));
                }
            }

        } else {
            System.out.println("Cargando datos para editar huésped ID: " + guestIdToEdit);
            Map<String, Object> data = db.get_guest_details(guestIdToEdit);
            guestForm.loadGuestData(data);
        }

        guestForm.setOnCancel(() -> {
            if (onCloseModalRequest != null)
                onCloseModalRequest.run();
        });

        guestForm.setOnSave(() -> {
            Map<String, Object> data = guestForm.validateAndGetData();

            if (data != null) {
                boolean success;

                if (guestIdToEdit == -1) {
                    success = db.add_guest(
                            this.currentCheckinId,
                            (String) data.get("fullname"), (String) data.get("surname1"), (String) data.get("surname2"),
                            (String) data.get("sex"), (String) data.get("birthDate"), (String) data.get("nationality"),
                            (String) data.get("docType"), (String) data.get("docNumber"),
                            (String) data.get("supportNumber"),
                            (String) data.get("address"), (String) data.get("city"), (String) data.get("country"),
                            (String) data.get("phone"), (String) data.get("email"), (Boolean) data.get("isMinor"),
                            (Integer) data.get("guardianId"));
                } else {
                    success = db.update_guest(
                            guestIdToEdit,
                            (String) data.get("fullname"), (String) data.get("surname1"), (String) data.get("surname2"),
                            (String) data.get("sex"), (String) data.get("birthDate"), (String) data.get("nationality"),
                            (String) data.get("docType"), (String) data.get("docNumber"),
                            (String) data.get("supportNumber"),
                            (String) data.get("address"), (String) data.get("city"), (String) data.get("country"),
                            (String) data.get("phone"), (String) data.get("email"), (Boolean) data.get("isMinor"),
                            (Integer) data.get("guardianId"));
                }

                if (success) {
                    refreshGuestList();
                    if (onCloseModalRequest != null)
                        onCloseModalRequest.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al guardar en base de datos.");
                }
            }
        });

        onOpenModalRequest.accept(guestForm);
    }

    /**
     * Define la acción a ejecutar cuando se solicita abrir una ventana modal (ej. Nuevo Huésped).
     * * @param action Consumer que recibe el componente a mostrar en el modal.
     */
    public void setOnOpenModalRequest(Consumer<Component> action) {
        this.onOpenModalRequest = action;
    }

    /**
     * Define la acción a ejecutar cuando se debe cerrar la ventana modal actual.
     * * @param action Runnable de cierre.
     */
    public void setOnCloseModalRequest(Runnable action) {
        this.onCloseModalRequest = action;
    }

    /**
     * Define la acción a ejecutar cuando el usuario cancela el proceso (botón Cancelar/Cerrar).
     * * @param action Runnable de cancelación.
     */
    public void setOnCancel(Runnable action) {
        this.onCancel = action;
    }

    /**
     * Define la acción a ejecutar cuando el check-in se completa exitosamente.
     * * @param action Runnable de finalización.
     */
    public void setOnFinish(Runnable action) {
        this.onFinish = action;
    }

    private void navigateNext() {
        if (currentStep == 1) {
            DatabaseService db = new DatabaseService();
            List<GuestData> guests = db.get_guests_by_checkin(this.currentCheckinId);

            if (this.reservationInfo != null && guests.size() < this.reservationInfo.pax()) {
                int missing = this.reservationInfo.pax() - guests.size();
                JOptionPane.showMessageDialog(this,
                        "Faltan registrar " + missing + " huésped(s).\nDebe registrar a todos los viajeros ("
                                + this.reservationInfo.pax() + ") antes de continuar.",
                        "Registro Incompleto",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (currentStep == 2) {
            if (financialDataPanel != null && !financialDataPanel.isValidData()) {
                return;
            }
        }

        if (currentStep < TOTAL_STEPS) {
            currentStep++;
            updateUIState();
        }
    }

    private void navigateBack() {
        if (currentStep > 1) {
            currentStep--;
            updateUIState();
        }
    }

    /**
     * Carga los datos de una reserva específica para iniciar o continuar el proceso de check-in.
     * * Pasos lógicos:
     * - Consulta la reserva en la BD.
     * - Obtiene o crea un ID de check-in asociado en la tabla 'checkins'.
     * - Carga la lista de huéspedes ya registrados.
     * - Resetea los campos del formulario.
     * * @param reservationId ID de la reserva a procesar.
     */
    public void loadReservationData(int reservationId) {
        this.currentReservationId = reservationId;
        this.firstGuestAddressCache = null;
        DatabaseService db = new DatabaseService();
        this.reservationInfo = db.get_reservation_by_id(reservationId);
        this.currentCheckinId = db.get_or_create_checkin_id(reservationId);

        System.out.println("[CheckinForm] Check-in ID activo: " + this.currentCheckinId);

        refreshGuestList();

        this.currentStep = 1;
        updateUIState();
    }

    private void refreshGuestList() {
        guestsListContainer.removeAll();

        List<GuestData> guests = new DatabaseService().get_guests_by_checkin(this.currentCheckinId);

        if (this.reservationInfo != null && btnAddGuest != null) {
            boolean isFull = guests.size() >= this.reservationInfo.pax();
            btnAddGuest.setEnabled(!isFull);
            if (isFull) {
                btnAddGuest.setText("Cupo Completo (" + guests.size() + "/" + this.reservationInfo.pax() + ")");
                btnAddGuest.setBackground(Color.GRAY);
            } else {
                btnAddGuest.setText("+ Añadir Huésped (" + guests.size() + "/" + this.reservationInfo.pax() + ")");
                styleButtonPrimary(btnAddGuest);
            }
        }

        if (guests.isEmpty()) {
            JLabel l = new JLabel("No hay huéspedes registrados aún.");
            l.setForeground(Color.GRAY);
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            guestsListContainer.add(l);

        } else {
            for (GuestData g : guests) {

                JPanel row = new JPanel(new java.awt.BorderLayout());
                row.setBackground(Color.WHITE);
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                        BorderFactory.createEmptyBorder(10, 5, 10, 5)));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);

                String icon = g.isMinor() ? " (Menor)" : "";
                JLabel lblName = new JLabel("• " + g.fullname() + icon + "  [" + g.docNumber() + "]");
                lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
                row.add(lblName, BorderLayout.CENTER);

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
                actions.setBackground(Color.WHITE);

                JButton btnEdit = new JButton("Editar");
                styleMiniButton(btnEdit, new Color(230, 240, 255), new Color(0, 100, 200));
                ButtonUtils.styleSecondary(btnEdit);
                btnEdit.addActionListener(e -> openExternalGuestModal(g.id()));

                JButton btnDel = new JButton("x");
                styleMiniButton(btnDel, new Color(255, 235, 235), Color.RED);
                ButtonUtils.styleSecondary(btnDel);
                btnDel.addActionListener(e -> deleteGuest(g.id()));

                actions.add(btnEdit);
                actions.add(btnDel);

                row.add(actions, BorderLayout.EAST);

                guestsListContainer.add(row);
            }
        }

        guestsListContainer.revalidate();
        guestsListContainer.repaint();
    }

    private void styleMiniButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        btn.setFocusPainted(false);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void deleteGuest(int guestId) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este huésped?", "Confirmar", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (new DatabaseService().delete_guest(guestId)) {
                refreshGuestList();
            }
        }
    }

    private void updateUIState() {
        cardLayout.show(mainContentCardPanel, "STEP_" + currentStep);
        switch (currentStep) {
            case 1 -> stepIndicatorLabel.setText("Fase 1 de 3: Identificación Personal");
            case 2 -> {
                stepIndicatorLabel.setText("Fase 2 de 3: Datos Financieros");
                if (financialDataPanel != null) {
                    DatabaseService db = new DatabaseService();
                    List<GuestData> adults = db.get_adults_by_checkin(this.currentCheckinId);
                    financialDataPanel.setPotentialPayers(adults);
                }
            }
            case 3 -> stepIndicatorLabel.setText("Fase 3 de 3: Datos Contractuales");
        }
        btnBack.setVisible(currentStep > 1);
        btnNext.setVisible(currentStep < TOTAL_STEPS);
        btnFinish.setVisible(currentStep == TOTAL_STEPS);
    }

    private FinancialDataPanel financialDataPanel;

    private JPanel createStep2Panel() {
        financialDataPanel = new FinancialDataPanel();
        return financialDataPanel;
    }

    private ContractDataPanel contractDataPanel;

    private JPanel createStep3Panel() {
        contractDataPanel = new ContractDataPanel();
        return contractDataPanel;
    }

    private void styleButtonPrimary(JButton b) {
        b.setBackground(new Color(0, 153, 255));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
    }

    private void styleButtonSecondary(JButton b) {
        b.setBackground(new Color(245, 245, 245));
    }
}