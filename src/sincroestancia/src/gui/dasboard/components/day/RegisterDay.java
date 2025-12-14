package sincroestancia.src.gui.dasboard.components.day;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseWheelListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.text.MaskFormatter;

import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.gui.components.ButtonUtils;
import sincroestancia.src.models.ReservationInfo;

/**
 * Formulario lateral deslizante (Drawer Content) para la gestión de Reservas.
 * * Permite realizar operaciones CRUD (Crear, Leer/Cargar, Actualizar, Borrar) sobre reservas.
 * * Características técnicas destacadas:
 * - Implementación de scroll personalizado (`ScrollablePanel`) para evitar problemas de
 * redimensionado en layouts verticales.
 * - Gestión manual de placeholders (texto fantasma) en los campos de texto.
 * - Validación de formatos de fecha y campos obligatorios antes del envío.
 * * @author Carlos Padilla Labella
 */
public class RegisterDay extends javax.swing.JPanel {

    private JLabel title;

    private JPanel name_panel, dni_panel, email_panel, phone_panel, entry_panel, exit_panel, pax_panel, paid_panel;
    private JTextField name_input, dni_input, email_input, phone_input;
    private JFormattedTextField entry_input, exit_input;
    private JLabel name_label, dni_label, email_label, phone_label, entry_label, exit_label, pax_label, paid_label;

    private JSpinner pax_spinner;
    private JCheckBox paid_checkbox;

    private JButton submit_button;
    private JButton delete_button;

    private final String name_placeholder = "Nombre y apellidos";
    private final String dni_placeholder = "DNI / NIE / Pasaporte";
    private final String email_placeholder = "ejemplo@correo.com";
    private final String phone_placeholder = "+34 600 000 000";
    private final String date_placeholder = "__/__/____";

    private DatabaseService db_service;
    private int current_vut_id = -1;
    private int editing_reservation_id = -1;
    private Runnable onReservationSuccess;

    /**
     * Constructor.
     * * Inicializa el servicio de base de datos, construye la interfaz y configura
     * la lógica de los placeholders.
     */
    public RegisterDay() {
        this.db_service = new DatabaseService();
        initComponents();
        setupPlaceholderLogic();
    }

    /**
     * Construcción de la interfaz gráfica.
     * * Utiliza un diseño de encabezado fijo + cuerpo con scroll + pie de página fijo.
     * * Configura cada campo de entrada mediante métodos auxiliares para mantener el código limpio.
     */
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
        setBackground(Color.WHITE);

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 20, 15));

        title = new JLabel("Nueva Reserva");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(51, 51, 51));
        headerPanel.add(title);

        add(headerPanel, java.awt.BorderLayout.NORTH);

        ScrollablePanel contentPanel = new ScrollablePanel();
        contentPanel.setLayout(new javax.swing.BoxLayout(contentPanel, javax.swing.BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        configureInputPanel(name_panel = new JPanel(), name_label = new JLabel("Nombre completo"),
                name_input = new JTextField(name_placeholder));
        configureInputPanel(dni_panel = new JPanel(), dni_label = new JLabel("Documento de Identidad"),
                dni_input = new JTextField(dni_placeholder));
        configureInputPanel(email_panel = new JPanel(), email_label = new JLabel("Email"),
                email_input = new JTextField(email_placeholder));
        configureInputPanel(phone_panel = new JPanel(), phone_label = new JLabel("Teléfono"),
                phone_input = new JTextField(phone_placeholder));

        configureDatePanel(entry_panel = new JPanel(), entry_label = new JLabel("Fecha de Entrada"),
                entry_input = new JFormattedTextField(createDateMask()));
        configureDatePanel(exit_panel = new JPanel(), exit_label = new JLabel("Fecha de Salida"),
                exit_input = new JFormattedTextField(createDateMask()));

        pax_panel = new JPanel();
        pax_panel.setBackground(Color.WHITE);
        pax_panel.setLayout(new javax.swing.BoxLayout(pax_panel, javax.swing.BoxLayout.Y_AXIS));
        pax_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pax_panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        pax_label = new JLabel("Nº de Personas");
        pax_label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pax_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        pax_spinner = new JSpinner(new javax.swing.SpinnerNumberModel(1, 1, 20, 1));
        pax_spinner.setBorder(null);
        pax_spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor) pax_spinner.getEditor();
        spinnerEditor.getTextField().setBackground(Color.WHITE);
        spinnerEditor.getTextField().setHorizontalAlignment(SwingConstants.LEFT);

        javax.swing.JSeparator paxSep = new javax.swing.JSeparator();
        paxSep.setAlignmentX(Component.LEFT_ALIGNMENT);

        pax_panel.add(pax_label);
        pax_panel.add(Box.createRigidArea(new Dimension(0, 5)));
        pax_panel.add(pax_spinner);
        pax_panel.add(Box.createRigidArea(new Dimension(0, 5)));
        pax_panel.add(paxSep);

        // PAGO
        paid_panel = new JPanel();
        paid_panel.setBackground(Color.WHITE);
        paid_panel.setLayout(new javax.swing.BoxLayout(paid_panel, javax.swing.BoxLayout.Y_AXIS));
        paid_panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        paid_panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        paid_label = new JLabel("Estado del Pago");
        paid_label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        paid_label.setAlignmentX(Component.LEFT_ALIGNMENT);

        paid_checkbox = new JCheckBox("Marcar como \"Reserva Pagada\"");
        paid_checkbox.setBackground(Color.WHITE);
        paid_checkbox.setFocusPainted(false);
        paid_checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        javax.swing.JSeparator paidSep = new javax.swing.JSeparator();
        paidSep.setAlignmentX(Component.LEFT_ALIGNMENT);

        paid_panel.add(paid_label);
        paid_panel.add(paid_checkbox);
        paid_panel.add(Box.createRigidArea(new Dimension(0, 5)));
        paid_panel.add(paidSep);

        contentPanel.add(name_panel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(dni_panel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(email_panel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(phone_panel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(entry_panel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(exit_panel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(pax_panel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(paid_panel);

        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        enableScrollOnPanel(contentPanel, scrollPane);

        add(scrollPane, java.awt.BorderLayout.CENTER);

        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(new Color(250, 250, 250));
        footerPanel.setPreferredSize(new Dimension(0, 80));
        footerPanel.setLayout(new java.awt.GridLayout(1, 2, 15, 0));
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

        delete_button = new JButton("Eliminar");
        delete_button.setBackground(new Color(220, 53, 69));
        delete_button.setForeground(Color.WHITE);
        delete_button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        delete_button.setFocusPainted(false);
        delete_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ButtonUtils.styleDanger(delete_button);
        delete_button.setVisible(false);
        delete_button.addActionListener(e -> delete_buttonActionPerformed(e));

        submit_button = new JButton("Guardar Reserva");
        submit_button.setBackground(new Color(0, 123, 255));
        submit_button.setForeground(Color.WHITE);
        submit_button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        submit_button.setFocusPainted(false);
        submit_button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ButtonUtils.stylePrimary(submit_button);
        submit_button.addActionListener(e -> submit_buttonActionPerformed(e));

        footerPanel.add(delete_button);
        footerPanel.add(submit_button);

        add(footerPanel, java.awt.BorderLayout.SOUTH);
    }

    /**
     * Método auxiliar para configurar paneles de entrada de texto con estilo consistente.
     */
    private void configureInputPanel(JPanel panel, JLabel label, JTextField input) {
        panel.setBackground(Color.WHITE);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        input.setBorder(null);
        input.setBackground(Color.WHITE);
        input.setForeground(new Color(115, 115, 115));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);

        javax.swing.JSeparator sep = new javax.swing.JSeparator();
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(input);
        panel.add(sep);
    }

    private void configureDatePanel(JPanel panel, JLabel label, JFormattedTextField input) {
        configureInputPanel(panel, label, input);
    }

    /**
     * Crea la máscara de formato para fechas (##/##/####).
     */
    private MaskFormatter createDateMask() {
        try {
            MaskFormatter mask = new MaskFormatter("##/##/####");
            mask.setPlaceholderCharacter('_');
            return mask;
        } catch (ParseException e) {
            return new MaskFormatter();
        }
    }

    /**
     * Lógica de envío del formulario (Crear o Actualizar).
     * * Flujo:
     * - Valida campos obligatorios.
     * - Convierte fechas de visualización (dd/MM/yyyy) a SQL (yyyy-MM-dd).
     * - Comprueba que fecha salida > fecha entrada.
     * - Si `editing_reservation_id` es -1 -> INSERT.
     * - Si `editing_reservation_id` tiene valor -> UPDATE.
     * - Notifica éxito mediante el callback `onReservationSuccess`.
     */
    private void submit_buttonActionPerformed(java.awt.event.ActionEvent evt) {
        
        if (this.current_vut_id == -1) {
            JOptionPane.showMessageDialog(this, "Error: No se ha seleccionado ningún VUT.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = name_input.getText().trim();
        String dni = dni_input.getText().trim();
        String email = email_input.getText().trim();
        String phone = phone_input.getText().trim();
        String checkInStr = entry_input.getText();
        String checkOutStr = exit_input.getText();
        int pax = (Integer) pax_spinner.getValue();
        boolean isPaid = paid_checkbox.isSelected();

        if (name.isEmpty() || name.equals(name_placeholder) || dni.isEmpty() || dni.equals(dni_placeholder) || email.isEmpty() || email.equals(email_placeholder) || phone.isEmpty() || phone.equals(phone_placeholder) || checkInStr.equals(date_placeholder) || checkOutStr.equals(date_placeholder)) {
            JOptionPane.showMessageDialog(this, "Por favor, rellene todos los campos obligatorios.", "Campos Vacíos",
                    JOptionPane.WARNING_MESSAGE);
            return;
            
        }

        SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String checkInSQL, checkOutSQL;

        try {
            
            Date checkInDate = parser.parse(checkInStr);
            Date checkOutDate = parser.parse(checkOutStr);
            
            if (!checkOutDate.after(checkInDate)) {
                JOptionPane.showMessageDialog(this, "La fecha de salida debe ser posterior a la fecha de entrada.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            checkInSQL = formatter.format(checkInDate);
            checkOutSQL = formatter.format(checkOutDate);

        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this, "Formato de fecha inválido.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean success;
        if (this.editing_reservation_id == -1) {
            success = db_service.register_reservation(this.current_vut_id, name, dni, email, phone, checkInSQL,
                    checkOutSQL, pax, isPaid);
        } else {
            success = db_service.update_reservation_details(this.editing_reservation_id, this.current_vut_id, name, dni,
                    email, phone, pax, isPaid, checkInSQL, checkOutSQL);
        }

        if (success) {
            
            String msg = (this.editing_reservation_id == -1) ? "Reserva registrada exitosamente."
                    : "Reserva actualizada exitosamente.";
            
                    JOptionPane.showMessageDialog(this, msg, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            resetForm();
            
            if (this.onReservationSuccess != null) {
                this.onReservationSuccess.run();
            }

        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar. Revise conflictos de fechas.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Lógica de eliminación de reserva.
     * * Requiere confirmación del usuario.
     * * Libera los días en el calendario y borra el registro.
     */
    private void delete_buttonActionPerformed(java.awt.event.ActionEvent evt) {
        
        if (this.editing_reservation_id == -1)
            return;
        
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que quieres eliminar esta reserva?\nLos días volverán a quedar libres.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
                if (confirm == JOptionPane.YES_OPTION) {
            try {
                
                SimpleDateFormat parser = new SimpleDateFormat("dd/MM/yyyy");
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                
                String checkInSQL = formatter.format(parser.parse(entry_input.getText()));
                String checkOutSQL = formatter.format(parser.parse(exit_input.getText()));
                
                boolean success = db_service.delete_reservation(this.editing_reservation_id, this.current_vut_id,
                        checkInSQL, checkOutSQL);
                
                        if (success) {
                    JOptionPane.showMessageDialog(this, "Reserva eliminada.", "Eliminado",
                            JOptionPane.INFORMATION_MESSAGE);
                    resetForm();
                    if (this.onReservationSuccess != null)
                        this.onReservationSuccess.run();
                } else {
                    JOptionPane.showMessageDialog(this, "Error al eliminar la reserva.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (ParseException e) {}
        }
    }

    public void setVutId(int vut_id) {
        this.current_vut_id = vut_id;
    }

    public void setOnReservationSuccess(Runnable action) {
        this.onReservationSuccess = action;
    }

    /**
     * Carga el formulario con los datos de una reserva existente para edición.
     */
    public void loadForEdit(ReservationInfo resInfo) {
        
        if (resInfo == null)
            return;
        
        resetForm();
        
        this.editing_reservation_id = resInfo.id();
        Color defaultColor = new Color(51, 51, 51);
        name_input.setText(resInfo.guestName());
        name_input.setForeground(defaultColor);
        dni_input.setText(resInfo.guestDni());
        dni_input.setForeground(defaultColor);
        email_input.setText(resInfo.guestEmail());
        email_input.setForeground(defaultColor);
        phone_input.setText(resInfo.guestPhone());
        phone_input.setForeground(defaultColor);
        pax_spinner.setValue(resInfo.pax());
        paid_checkbox.setSelected(resInfo.isPaid());

        try {
        
            SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat viewFormat = new SimpleDateFormat("dd/MM/yyyy");
        
            entry_input.setText(viewFormat.format(sqlFormat.parse(resInfo.checkIn())));
            exit_input.setText(viewFormat.format(sqlFormat.parse(resInfo.checkOut())));
            entry_input.setForeground(defaultColor);
            exit_input.setForeground(defaultColor);

        } catch (ParseException e) {}

        submit_button.setText("Guardar Cambios");
        title.setText("Editar Reserva");
        delete_button.setVisible(true);
    }

    /**
     * Limpia el formulario y restaura los placeholders.
     */
    private void resetForm() {
        name_input.setText(name_placeholder);
        dni_input.setText(dni_placeholder);
        email_input.setText(email_placeholder);
        phone_input.setText(phone_placeholder);
        entry_input.setValue(null);
        exit_input.setValue(null);
        pax_spinner.setValue(1);
        paid_checkbox.setSelected(false);
        Color placeholderColor = new Color(115, 115, 115);
        name_input.setForeground(placeholderColor);
        dni_input.setForeground(placeholderColor);
        email_input.setForeground(placeholderColor);
        phone_input.setForeground(placeholderColor);
        this.editing_reservation_id = -1;
        submit_button.setText("Guardar Reserva");
        title.setText("Nueva Reserva");
        delete_button.setVisible(false);
    }

    /**
     * Establece la fecha de inicio al abrir el formulario desde el calendario.
     */
    public void setStartDate(Date date) {
        if (date == null)
            return;
        resetForm();
        try {
            SimpleDateFormat viewFormat = new SimpleDateFormat("dd/MM/yyyy");
            entry_input.setText(viewFormat.format(date));
            entry_input.setForeground(new Color(51, 51, 51));
        } catch (Exception e) {
        }
    }

    /**
     * Configura los Listeners para simular el efecto "Placeholder" en Swing.
     * * Cuando el campo gana foco, borra el texto gris.
     * * Cuando pierde foco, si está vacío, restaura el texto gris.
     */
    private void setupPlaceholderLogic() {

        java.util.Map<JTextField, String> placeholders = new java.util.HashMap<>();
        placeholders.put(name_input, name_placeholder);
        placeholders.put(dni_input, dni_placeholder);
        placeholders.put(email_input, email_placeholder);
        placeholders.put(phone_input, phone_placeholder);

        final Color placeholderColor = new Color(115, 115, 115);
        final Color defaultColor = new Color(51, 51, 51);

        for (java.util.Map.Entry<JTextField, String> entry : placeholders.entrySet()) {

            JTextField field = entry.getKey();
            String placeholder = entry.getValue();

            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (field.getText().equals(placeholder)) {
                        field.setText("");
                        field.setForeground(defaultColor);
                    }
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (field.getText().isEmpty()) {
                        field.setText(placeholder);
                        field.setForeground(placeholderColor);
                    }
                }
            });
        }
    }

    /**
     * Habilita el scroll con la rueda del ratón incluso cuando el cursor está sobre
     * componentes que normalmente no propagan el evento (como paneles internos).
     */
    private void enableScrollOnPanel(JPanel contentPanel, JScrollPane scrollPane) {
        MouseWheelListener scroller = e -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            int amount = e.getWheelRotation() * bar.getUnitIncrement() * 3;
            bar.setValue(bar.getValue() + amount);
        };

        contentPanel.addMouseWheelListener(scroller);

        for (Component c : contentPanel.getComponents()) {
            if (c instanceof JPanel) {
                c.addMouseWheelListener(scroller);
            }
        }
    }
    
    /**
     * Clase interna que implementa la interfaz Scrollable.
     * * Necesaria para que el JPanel dentro del JScrollPane se comporte correctamente:
     * - getScrollableTracksViewportWidth() -> true: Forza al panel a ajustarse al ancho del viewport (evita scroll horizontal).
     * - getScrollableTracksViewportHeight() -> false: Permite que el panel crezca verticalmente (activa scroll vertical).
     */
    private class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}