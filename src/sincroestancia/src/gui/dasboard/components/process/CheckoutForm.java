package sincroestancia.src.gui.dasboard.components.process;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.gui.components.ButtonUtils;
import sincroestancia.src.models.ReservationInfo;

/**
 * Formulario de Checkout (Salida de Huésped)
 */
public class CheckoutForm extends JPanel {

    // UI Components
    private JTextField txtCheckoutDate;
    private JTextField txtCheckoutTime;
    private JCheckBox chkKeysReturned;
    private JCheckBox chkDamagesDetected;
    private JTextArea txtDamagesDescription;
    private JButton btnCancel;
    private JButton btnFinish;

    // Data
    private int currentReservationId = -1;
    private ReservationInfo reservationInfo;

    // Callbacks
    private Runnable onCancel;
    private Runnable onFinish;

    public CheckoutForm() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        headerPanel.setPreferredSize(new Dimension(0, 70));

        JPanel headerInner = new JPanel(new GridLayout(2, 1));
        headerInner.setBackground(Color.WHITE);
        headerInner.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel titleLabel = new JLabel("Checkout - Salida de Huésped");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel subtitleLabel = new JLabel("Complete los datos de salida");
        subtitleLabel.setForeground(Color.GRAY);

        headerInner.add(titleLabel);
        headerInner.add(subtitleLabel);
        headerPanel.add(headerInner, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // BODY
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new javax.swing.BoxLayout(bodyPanel, javax.swing.BoxLayout.Y_AXIS));
        bodyPanel.setBackground(Color.WHITE);
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 1. Fecha y Hora de Salida
        bodyPanel.add(createSectionLabel("¿A qué hora exacta ha quedado libre la vivienda?"));
        bodyPanel.add(createVerticalSpace(10));

        JPanel dateTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        dateTimePanel.setBackground(Color.WHITE);
        dateTimePanel.setAlignmentX(LEFT_ALIGNMENT);
        dateTimePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lblDate = new JLabel("Fecha:");
        lblDate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtCheckoutDate = new JTextField(12);
        txtCheckoutDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel lblTime = new JLabel("Hora:");
        lblTime.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtCheckoutTime = new JTextField(8);
        txtCheckoutTime.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtCheckoutTime.setText("12:00");

        dateTimePanel.add(lblDate);
        dateTimePanel.add(txtCheckoutDate);
        dateTimePanel.add(lblTime);
        dateTimePanel.add(txtCheckoutTime);

        bodyPanel.add(dateTimePanel);
        bodyPanel.add(createVerticalSpace(20));

        // 2. Entrega de Llaves
        bodyPanel.add(createSectionLabel("¿Entrega todos los juegos de llaves facilitados?"));
        bodyPanel.add(createVerticalSpace(10));

        chkKeysReturned = new JCheckBox("Sí, todas las llaves han sido devueltas");
        chkKeysReturned.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkKeysReturned.setBackground(Color.WHITE);
        chkKeysReturned.setAlignmentX(LEFT_ALIGNMENT);

        bodyPanel.add(chkKeysReturned);
        bodyPanel.add(createVerticalSpace(20));

        // 3. Averías o Desperfectos
        bodyPanel.add(createSectionLabel("¿Ha detectado alguna avería o desperfecto durante su estancia?"));
        bodyPanel.add(createVerticalSpace(5));

        JLabel lblNote = new JLabel("(Fundamental preguntarlo antes de devolver fianzas)");
        lblNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblNote.setForeground(new Color(100, 100, 100));
        lblNote.setAlignmentX(LEFT_ALIGNMENT);
        bodyPanel.add(lblNote);
        bodyPanel.add(createVerticalSpace(10));

        chkDamagesDetected = new JCheckBox("Sí, se han detectado averías o desperfectos");
        chkDamagesDetected.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkDamagesDetected.setBackground(Color.WHITE);
        chkDamagesDetected.setAlignmentX(LEFT_ALIGNMENT);

        chkDamagesDetected.addActionListener(e -> {
            txtDamagesDescription.setEnabled(chkDamagesDetected.isSelected());
            if (!chkDamagesDetected.isSelected()) {
                txtDamagesDescription.setText("");
            }
        });

        bodyPanel.add(chkDamagesDetected);
        bodyPanel.add(createVerticalSpace(10));

        JLabel lblDescription = new JLabel("Descripción de averías/desperfectos:");
        lblDescription.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDescription.setAlignmentX(LEFT_ALIGNMENT);
        bodyPanel.add(lblDescription);
        bodyPanel.add(createVerticalSpace(5));

        txtDamagesDescription = new JTextArea(5, 30);
        txtDamagesDescription.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDamagesDescription.setLineWrap(true);
        txtDamagesDescription.setWrapStyleWord(true);
        txtDamagesDescription.setEnabled(false);
        txtDamagesDescription.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        JScrollPane scrollPane = new JScrollPane(txtDamagesDescription);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        bodyPanel.add(scrollPane);

        JScrollPane bodyScroll = new JScrollPane(bodyPanel);
        bodyScroll.setBorder(null);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(bodyScroll, BorderLayout.CENTER);

        // FOOTER
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setPreferredSize(new Dimension(0, 80));
        footerPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));

        JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
        leftFooter.setOpaque(false);

        btnCancel = new JButton("Cancelar");
        btnCancel.setMargin(new java.awt.Insets(8, 25, 8, 25));
        styleButtonSecondary(btnCancel);
        ButtonUtils.styleDanger(btnCancel);
        btnCancel.addActionListener(e -> {
            if (onCancel != null)
                onCancel.run();
        });

        leftFooter.add(btnCancel);

        JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 20));
        rightFooter.setOpaque(false);

        btnFinish = new JButton("Finalizar Checkout");
        btnFinish.setMargin(new java.awt.Insets(8, 25, 8, 25));
        styleButtonPrimary(btnFinish);
        btnFinish.setBackground(new Color(34, 197, 94)); // Verde
        ButtonUtils.stylePrimary(btnFinish);
        btnFinish.addActionListener(e -> finishCheckout());

        rightFooter.add(btnFinish);

        footerPanel.add(leftFooter, BorderLayout.WEST);
        footerPanel.add(rightFooter, BorderLayout.EAST);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createVerticalSpace(int height) {
        JPanel space = new JPanel();
        space.setBackground(Color.WHITE);
        space.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        space.setPreferredSize(new Dimension(0, height));
        return space;
    }

    private void styleButtonPrimary(JButton btn) {
        btn.setBackground(new Color(0, 153, 255));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private void styleButtonSecondary(JButton btn) {
        btn.setBackground(new Color(245, 245, 245));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    public void loadReservationData(int reservationId) {
        this.currentReservationId = reservationId;

        DatabaseService db = new DatabaseService();
        this.reservationInfo = db.get_reservation_by_id(reservationId);

        if (reservationInfo != null) {
            // Establecer fecha de checkout por defecto
            txtCheckoutDate.setText(reservationInfo.checkOut());
        }

        // Resetear campos
        txtCheckoutTime.setText("12:00");
        chkKeysReturned.setSelected(false);
        chkDamagesDetected.setSelected(false);
        txtDamagesDescription.setText("");
        txtDamagesDescription.setEnabled(false);
    }

    private void finishCheckout() {
        // 1. Recopilar datos
        String checkoutDate = txtCheckoutDate.getText().trim();
        String checkoutTime = txtCheckoutTime.getText().trim();
        String fullExitTime = checkoutDate + " " + checkoutTime; // Formato para BBDD
        
        boolean keysReturned = chkKeysReturned.isSelected();
        boolean damagesDetected = chkDamagesDetected.isSelected();
        String damagesDescription = txtDamagesDescription.getText().trim();

        // 2. Validaciones básicas
        if (checkoutDate.isEmpty() || checkoutTime.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "La fecha y hora son obligatorias.", "Error", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!keysReturned) {
            int confirm = javax.swing.JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de que NO se han devuelto las llaves?\nEsto quedará registrado.", 
                "Advertencia de Seguridad", javax.swing.JOptionPane.YES_NO_OPTION);
            if (confirm != javax.swing.JOptionPane.YES_OPTION) return;
        }

        // 3. Guardar en Base de Datos
        DatabaseService db = new DatabaseService();
        boolean success = db.register_checkout(
            this.currentReservationId,
            fullExitTime,
            keysReturned,
            damagesDetected,
            damagesDescription
        );

        if (success) {
            javax.swing.JOptionPane.showMessageDialog(this, "Checkout registrado correctamente.", "Éxito", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            if (onFinish != null) {
                onFinish.run(); // Esto cerrará el drawer y refrescará el dashboard
            }
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Error al guardar en la base de datos.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setOnCancel(Runnable action) {
        this.onCancel = action;
    }

    public void setOnFinish(Runnable action) {
        this.onFinish = action;
    }
}
