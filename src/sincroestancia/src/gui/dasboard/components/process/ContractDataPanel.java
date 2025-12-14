package sincroestancia.src.gui.dasboard.components.process;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panel para la Fase 3: Datos Contractuales y Consentimientos.
 */
public class ContractDataPanel extends JPanel {

    private JCheckBox chkDates;
    private JCheckBox chkPrice;
    private JCheckBox chkRules;
    private JCheckBox chkGdpr;

    public ContractDataPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        addSectionTitle("Confirmación y Consentimientos");

        // 1. Fechas
        chkDates = createCheckbox("¿Confirma las fechas exactas de entrada y salida (Check-in / Check-out)?");
        add(chkDates);
        add(Box.createRigidArea(new Dimension(0, 15)));

        // 2. Precio
        chkPrice = createCheckbox("¿Acepta el precio final con todos los servicios e impuestos incluidos?");
        add(chkPrice);
        add(Box.createRigidArea(new Dimension(0, 15)));

        // 3. Normas (Crítico)
        chkRules = createCheckbox(
                "¿Ha leído y acepta expresamente las normas de convivencia y el reglamento de régimen interno?");
        // Añadir nota explicativa pequeña
        JLabel noteRules = new JLabel(
                "<html><i style='color:gray; font-size:10px'>* Imprescindible para ejercer el derecho de admisión/expulsión en caso de incumplimiento grave.</i></html>");
        noteRules.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0)); // Indentar
        noteRules.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(chkRules);
        add(noteRules);
        add(Box.createRigidArea(new Dimension(0, 15)));

        // 4. RGPD
        chkGdpr = createCheckbox(
                "¿Autoriza el tratamiento de sus datos personales para el registro de viajeros (RGPD)?");
        add(chkGdpr);
    }

    private JCheckBox createCheckbox(String text) {
        JCheckBox chk = new JCheckBox(text);
        chk.setBackground(Color.WHITE);
        chk.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chk.setFocusPainted(false);
        chk.setAlignmentX(Component.LEFT_ALIGNMENT);
        return chk;
    }

    private void addSectionTitle(String title) {
        JLabel l = new JLabel(title);
        l.setFont(new Font("Segoe UI", Font.BOLD, 16));
        l.setForeground(new Color(0, 153, 255));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(l);
        add(Box.createRigidArea(new Dimension(0, 20)));
    }

    /**
     * Valida que todos los consentimientos estén marcados.
     */
    public boolean isValidData() {
        if (!chkDates.isSelected()) {
            showError("Debe confirmar las fechas de estancia.");
            return false;
        }
        if (!chkPrice.isSelected()) {
            showError("Debe aceptar el precio final.");
            return false;
        }
        if (!chkRules.isSelected()) {
            showError("Es obligatorio aceptar las normas de convivencia.");
            return false;
        }
        if (!chkGdpr.isSelected()) {
            showError("Debe autorizar el tratamiento de datos (RGPD).");
            return false;
        }
        return true;
    }

    private void showError(String msg) {
        javax.swing.JOptionPane.showMessageDialog(this, msg, "Consentimiento Requerido",
                javax.swing.JOptionPane.WARNING_MESSAGE);
    }
}
