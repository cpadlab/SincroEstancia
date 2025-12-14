package sincroestancia.src.gui.dasboard.components.process;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.models.GuestData;

/**
 * Panel para la Fase 2: Datos Financieros (RD 933/2021).
 */
public class FinancialDataPanel extends JPanel {

    private JComboBox<String> comboPaymentMethod;
    private JTextField txtPaymentId; // Últimos dígitos o IBAN
    private JComboBox<GuardianItem> comboPayer; // Titular del medio de pago
    private JFormattedTextField txtCardExpiry; // MM/YY
    private JFormattedTextField txtPaymentDate; // dd/MM/yyyy

    public FinancialDataPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding reducido

        addSectionTitle("Datos de la Transacción (RD 933/2021)");

        // 1. Medio de Pago
        comboPaymentMethod = new JComboBox<>(new String[] {
                "Tarjeta de Crédito/Débito",
                "Transferencia Bancaria",
                "Efectivo",
                "Plataforma Online (Airbnb, Booking, etc.)"
        });
        add(createInput("Medio de Pago", comboPaymentMethod));
        add(Box.createRigidArea(new Dimension(0, 10))); // Espaciado reducido

        // 2. Identificación del Medio de Pago
        txtPaymentId = new JTextField();
        add(createInput("Identificación del Medio de Pago (Últimos 4 dígitos o IBAN)", txtPaymentId));
        add(Box.createRigidArea(new Dimension(0, 10))); // Espaciado reducido

        // 3. Titular del Pago
        comboPayer = new JComboBox<>();
        add(createInput("Titular del Medio de Pago", comboPayer));
        add(Box.createRigidArea(new Dimension(0, 10))); // Espaciado reducido

        // 4. Fechas (Caducidad y Pago)
        JPanel datesPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        datesPanel.setBackground(Color.WHITE);
        datesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        datesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        txtCardExpiry = new JFormattedTextField(createMask("##/##"));
        datesPanel.add(createInput("Fecha Caducidad Tarjeta (MM/YY)", txtCardExpiry));

        txtPaymentDate = new JFormattedTextField(createMask("##/##/####"));
        txtPaymentDate.setText(new SimpleDateFormat("dd/MM/yyyy").format(new Date())); // Fecha de hoy por defecto
        datesPanel.add(createInput("Fecha de Realización del Pago", txtPaymentDate));

        add(datesPanel);
    }

    /**
     * Valida los datos del formulario.
     * 
     * @return true si los datos son válidos, false en caso contrario (y muestra
     *         alerta).
     */
    public boolean isValidData() {
        // 1. Validar campos obligatorios básicos
        if (txtPaymentId.getText().trim().isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Debe indicar la identificación del medio de pago (ej. últimos 4 dígitos).");
            return false;
        }

        if (getPayerId() == -1) {
            javax.swing.JOptionPane.showMessageDialog(this, "Debe seleccionar el titular del medio de pago.");
            return false;
        }

        // 2. Validar fecha de pago
        String pDate = txtPaymentDate.getText().trim();
        if (pDate.equals("__/__/____") || pDate.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "La fecha de pago es obligatoria.");
            return false;
        }

        // 3. Validar caducidad tarjeta (si aplica)
        String method = (String) comboPaymentMethod.getSelectedItem();
        if (method != null && method.contains("Tarjeta")) {
            String expiry = txtCardExpiry.getText().trim();
            if (expiry.equals("__/__") || expiry.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Para pagos con tarjeta, la fecha de caducidad es obligatoria.");
                return false;
            }

            // Validar que no esté caducada
            try {
                String[] parts = expiry.split("/");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt("20" + parts[1]); // Asumimos siglo 21

                java.time.YearMonth expiryDate = java.time.YearMonth.of(year, month);
                java.time.YearMonth now = java.time.YearMonth.now();

                if (expiryDate.isBefore(now)) {
                    javax.swing.JOptionPane.showMessageDialog(this, "La tarjeta indicada está caducada.");
                    return false;
                }

                if (month < 1 || month > 12) {
                    javax.swing.JOptionPane.showMessageDialog(this, "Mes de caducidad inválido (01-12).");
                    return false;
                }

            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(this, "Formato de fecha de caducidad inválido.");
                return false;
            }
        }

        return true;
    }

    /**
     * Rellena el selector de pagadores con los huéspedes adultos registrados.
     */
    public void setPotentialPayers(List<GuestData> adults) {
        comboPayer.removeAllItems();
        comboPayer.addItem(new GuardianItem(-1, "Seleccione el titular..."));

        if (adults != null) {
            for (GuestData adult : adults) {
                String label = adult.fullname() + " (" + adult.docNumber() + ")";
                comboPayer.addItem(new GuardianItem(adult.id(), label));
            }
        }
        // Opción para "Otro / No registrado" si fuera necesario, aunque la normativa
        // suele pedir identificarlo.
    }

    // --- Helpers UI ---

    private JPanel createInput(String labelText, Component field) {
        JPanel p = new JPanel(new java.awt.BorderLayout(0, 5));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60)); // Limitar altura máxima

        JLabel l = new JLabel(labelText);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(100, 100, 100));

        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        p.add(l, java.awt.BorderLayout.NORTH);
        p.add(field, java.awt.BorderLayout.CENTER);
        return p;
    }

    private void addSectionTitle(String title) {
        JLabel l = new JLabel(title);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        l.setForeground(new Color(0, 153, 255));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(l);
        add(Box.createRigidArea(new Dimension(0, 20)));
    }

    private MaskFormatter createMask(String s) {
        try {
            MaskFormatter mask = new MaskFormatter(s);
            mask.setPlaceholderCharacter('_');
            return mask;
        } catch (ParseException e) {
            return new MaskFormatter();
        }
    }

    // Clase auxiliar para el combo (reutilizada conceptualmente)
    public static class GuardianItem {
        private final int id;
        private final String name;

        public GuardianItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // --- Getters de datos ---
    public String getPaymentMethod() {
        return (String) comboPaymentMethod.getSelectedItem();
    }

    public String getPaymentId() {
        return txtPaymentId.getText().trim();
    }

    public int getPayerId() {
        GuardianItem selected = (GuardianItem) comboPayer.getSelectedItem();
        return (selected != null) ? selected.getId() : -1;
    }

    public String getCardExpiry() {
        return txtCardExpiry.getText().trim();
    }

    public String getPaymentDate() {
        return txtPaymentDate.getText().trim();
    }
}
