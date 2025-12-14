package sincroestancia.src.gui.dasboard.components.process;

import java.awt.Color;
import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import sincroestancia.src.gui.components.ButtonUtils;
import java.awt.Cursor;

import sincroestancia.src.models.GuestData;

/**
 * Formulario detallado para los datos de un huésped (Adulto o Menor).
 */
public class GuestFormPanel extends JPanel {

    // --- UI Components ---
    private JTextField txtFullname, txtSurname1, txtSurname2;
    private JComboBox<String> comboSex;
    private JFormattedTextField txtBirthDate;
    private JTextField txtNationality;
    private JComboBox<String> comboDocType;
    private JTextField txtDocNumber, txtSupportNumber; // Fecha expedición o soporte

    // Dirección
    private JTextField txtAddress, txtCity, txtCountry;

    // Contacto
    private JTextField txtPhone, txtEmail;

    // Menores
    private JCheckBox chkIsMinor;
    private JComboBox<GuardianItem> comboGuardian;

    // Botones
    private Runnable onSave;
    private Runnable onCancel;

    // Paneles contenedores para ocultar/mostrar según si es menor
    private JPanel contactPanel, addressPanel, docPanel;

    public GuestFormPanel() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new java.awt.BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        // 1. HEADER
        JPanel header = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 20, 15));
        header.setBackground(new Color(245, 245, 245));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));

        JLabel title = new JLabel("Datos del Huésped");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.add(title);
        add(header, java.awt.BorderLayout.NORTH);

        // 2. BODY (Scrollable)
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // --- SECCIÓN 0: TIPO DE HUÉSPED ---
        chkIsMinor = new JCheckBox("Es menor de 14 años");
        chkIsMinor.setBackground(Color.WHITE);
        chkIsMinor.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chkIsMinor.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR A LA IZQUIERDA
        chkIsMinor.addActionListener(e -> toggleMinorFields(chkIsMinor.isSelected()));

        body.add(chkIsMinor);
        body.add(Box.createRigidArea(new Dimension(0, 15)));

        // Selector de Tutor (Solo visible si es menor)
        JPanel guardianPanel = new JPanel(new GridLayout(2, 1));
        guardianPanel.setBackground(Color.WHITE);
        guardianPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR
        guardianPanel.add(new JLabel("Tutor / Adulto Responsable:"));
        comboGuardian = new JComboBox<>();
        guardianPanel.add(comboGuardian);
        guardianPanel.setVisible(false);

        body.add(guardianPanel);
        body.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- SECCIÓN 1: IDENTIFICACIÓN BÁSICA ---
        addSectionTitle(body, "Identificación");

        JPanel nameGrid = new JPanel(new GridLayout(1, 3, 15, 0));
        nameGrid.setBackground(Color.WHITE);
        nameGrid.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR GRID
        nameGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        nameGrid.add(createInput("Nombre", txtFullname = new JTextField()));
        nameGrid.add(createInput("Primer Apellido", txtSurname1 = new JTextField()));
        nameGrid.add(createInput("Segundo Apellido", txtSurname2 = new JTextField()));
        body.add(nameGrid);
        body.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel bioGrid = new JPanel(new GridLayout(1, 3, 15, 0));
        bioGrid.setBackground(Color.WHITE);
        bioGrid.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR GRID
        bioGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        // Fecha nacimiento con máscara
        txtBirthDate = new javax.swing.JFormattedTextField(createDateMask());
        bioGrid.add(createInput("Fecha Nacimiento (dd/mm/aaaa)", txtBirthDate));

        comboSex = new JComboBox<>(new String[] { "Hombre", "Mujer" });
        JPanel sexPanel = new JPanel(new java.awt.BorderLayout());
        sexPanel.setBackground(Color.WHITE);
        sexPanel.add(new JLabel("Sexo"), java.awt.BorderLayout.NORTH);
        sexPanel.add(comboSex, java.awt.BorderLayout.CENTER);
        bioGrid.add(sexPanel);

        bioGrid.add(createInput("Nacionalidad", txtNationality = new JTextField()));
        body.add(bioGrid);
        body.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- SECCIÓN 2: DOCUMENTO ---
        docPanel = new JPanel();
        docPanel.setLayout(new BoxLayout(docPanel, BoxLayout.Y_AXIS));
        docPanel.setBackground(Color.WHITE);
        docPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR PANEL

        addSectionTitle(docPanel, "Documento de Identidad");
        JPanel docGrid = new JPanel(new GridLayout(1, 3, 15, 0));
        docGrid.setBackground(Color.WHITE);
        docGrid.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR GRID
        docGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        comboDocType = new JComboBox<>(new String[] { "DNI", "Pasaporte", "TIE/NIE" });
        JPanel typePanel = new JPanel(new java.awt.BorderLayout());
        typePanel.setBackground(Color.WHITE);
        typePanel.add(new JLabel("Tipo"), java.awt.BorderLayout.NORTH);
        typePanel.add(comboDocType, java.awt.BorderLayout.CENTER);
        docGrid.add(typePanel);

        docGrid.add(createInput("Número", txtDocNumber = new JTextField()));
        docGrid.add(createInput("Soporte / Expedición", txtSupportNumber = new JTextField()));

        docPanel.add(docGrid);
        docPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        body.add(docPanel);

        // --- SECCIÓN 3: DIRECCIÓN ---
        addressPanel = new JPanel();
        addressPanel.setLayout(new BoxLayout(addressPanel, BoxLayout.Y_AXIS));
        addressPanel.setBackground(Color.WHITE);
        addressPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR PANEL

        addSectionTitle(addressPanel, "Residencia Habitual");
        JPanel addressInputPanel = createInput("Dirección Completa", txtAddress = new JTextField());
        addressInputPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR INPUT
        addressPanel.add(addressInputPanel);
        addressPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel locGrid = new JPanel(new GridLayout(1, 2, 15, 0));
        locGrid.setBackground(Color.WHITE);
        locGrid.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR GRID
        locGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        locGrid.add(createInput("Municipio", txtCity = new JTextField()));
        locGrid.add(createInput("País", txtCountry = new JTextField()));
        addressPanel.add(locGrid);
        addressPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        body.add(addressPanel);

        // --- SECCIÓN 4: CONTACTO ---
        contactPanel = new JPanel();
        contactPanel.setLayout(new BoxLayout(contactPanel, BoxLayout.Y_AXIS));
        contactPanel.setBackground(Color.WHITE);
        contactPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR PANEL

        addSectionTitle(contactPanel, "Contacto (RD 933/2021)");
        JPanel contactGrid = new JPanel(new GridLayout(1, 2, 15, 0));
        contactGrid.setBackground(Color.WHITE);
        contactGrid.setAlignmentX(Component.LEFT_ALIGNMENT); // <--- ALINEAR GRID
        contactGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        contactGrid.add(createInput("Teléfono Móvil", txtPhone = new JTextField()));
        contactGrid.add(createInput("Correo Electrónico", txtEmail = new JTextField()));
        contactPanel.add(contactGrid);

        body.add(contactPanel);

        // SCROLL
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, java.awt.BorderLayout.CENTER);

        // 3. FOOTER (Botones)
        // 3. FOOTER (Botones)
        JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 15, 15));
        footer.setBackground(new Color(245, 245, 245));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        JButton btnClose = new JButton("Cancelar");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnClose.setBackground(new Color(200, 200, 200));
        btnClose.setForeground(Color.BLACK);
        btnClose.setFocusPainted(false);
        btnClose.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ButtonUtils.styleDanger(btnClose);
        btnClose.addActionListener(e -> {
            if (onCancel != null)
                onCancel.run();
        });

        JButton btnSave = new JButton("Guardar Huésped");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSave.setBackground(new Color(0, 153, 255));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        ButtonUtils.stylePrimary(btnSave);
        btnSave.addActionListener(e -> {
            if (onSave != null)
                onSave.run();
        });

        footer.add(btnClose);
        footer.add(btnSave);
        add(footer, java.awt.BorderLayout.SOUTH);
    }

    /**
     * Rellena el selector de tutores con la lista de adultos proporcionada.
     */
    public void setPotentialGuardians(java.util.List<GuestData> adults) {
        comboGuardian.removeAllItems();

        // Opción por defecto (null)
        comboGuardian.addItem(new GuardianItem(-1, "Seleccione un adulto..."));

        if (adults != null) {
            for (GuestData adult : adults) {
                String label = adult.fullname() + " (" + adult.docNumber() + ")";
                comboGuardian.addItem(new GuardianItem(adult.id(), label));
            }
        }
    }

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
     * Pre-rellena el formulario con datos sugeridos.
     * Pasar null en los campos que no se quieran rellenar.
     */
    public void setPreloadedData(String name, String dni, String email, String phone,
            String address, String city, String country) {

        if (name != null && !name.isEmpty()) {
            // Intentar separar nombre y apellidos si viene todo junto (lógica básica)
            String[] parts = name.split(" ", 2);
            txtFullname.setText(parts[0]);
            if (parts.length > 1)
                txtSurname1.setText(parts[1]);
        }

        if (dni != null)
            txtDocNumber.setText(dni);
        if (email != null)
            txtEmail.setText(email);
        if (phone != null)
            txtPhone.setText(phone);

        if (address != null)
            txtAddress.setText(address);
        if (city != null)
            txtCity.setText(city);
        if (country != null)
            txtCountry.setText(country);
    }

    /**
     * Limpia todos los campos para un nuevo registro.
     */
    public void clearForm() {
        txtFullname.setText("");
        txtSurname1.setText("");
        txtSurname2.setText("");
        txtBirthDate.setValue(null); // Limpia máscara
        txtNationality.setText("");
        txtDocNumber.setText("");
        txtSupportNumber.setText("");
        txtAddress.setText("");
        txtCity.setText("");
        txtCountry.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        chkIsMinor.setSelected(false);
        toggleMinorFields(false);

        // Resets combos
        if (comboSex.getItemCount() > 0)
            comboSex.setSelectedIndex(0);
        if (comboDocType.getItemCount() > 0)
            comboDocType.setSelectedIndex(0);
    }

    /**
     * Carga los datos de un huésped existente en el formulario.
     */
    public void loadGuestData(java.util.Map<String, Object> data) {
        if (data == null)
            return;

        txtFullname.setText((String) data.get("fullname"));
        txtSurname1.setText((String) data.get("surname1"));
        txtSurname2.setText((String) data.get("surname2"));
        comboSex.setSelectedItem(data.get("sex"));
        txtBirthDate.setText((String) data.get("birthDate"));
        txtNationality.setText((String) data.get("nationality"));
        comboDocType.setSelectedItem(data.get("docType"));
        txtDocNumber.setText((String) data.get("docNumber"));
        txtSupportNumber.setText((String) data.get("supportNumber"));
        txtAddress.setText((String) data.get("address"));
        txtCity.setText((String) data.get("city"));
        txtCountry.setText((String) data.get("country"));
        txtPhone.setText((String) data.get("phone"));
        txtEmail.setText((String) data.get("email"));

        boolean isMinor = (Boolean) data.get("isMinor");
        chkIsMinor.setSelected(isMinor);
        toggleMinorFields(isMinor);
    }

    // Clase auxiliar para guardar ID y Nombre en el JComboBox
    private static class GuardianItem {
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
            return name; // Esto es lo que se ve en el Combo
        }
    }

    /**
     * Valida los campos y devuelve un Mapa con los datos.
     * Retorna null si hay errores de validación.
     */
    public java.util.Map<String, Object> validateAndGetData() {
        String fullname = txtFullname.getText().trim();
        String surname1 = txtSurname1.getText().trim();
        String docNumber = txtDocNumber.getText().trim();
        String birthDate = txtBirthDate.getText().trim();
        if (birthDate.equals("__/__/____")) {
            birthDate = "";
        }

        // 1. Validación de campos obligatorios básicos
        if (fullname.isEmpty() || surname1.isEmpty() || docNumber.isEmpty() || birthDate.isEmpty() ||
                txtNationality.getText().trim().isEmpty()) {

            javax.swing.JOptionPane.showMessageDialog(this,
                    "Por favor, rellene los campos obligatorios de Identificación y Documento.",
                    "Datos incompletos", javax.swing.JOptionPane.WARNING_MESSAGE);
            return null;
        }

        // 2. Validación condicional (Si NO es menor, contacto obligatorio)
        Integer guardianId = null;
        if (chkIsMinor.isSelected()) {
            GuardianItem selected = (GuardianItem) comboGuardian.getSelectedItem();
            if (selected != null && selected.getId() != -1) {
                guardianId = selected.getId();
            } else {
                // Opcional: Obligar a seleccionar tutor si es menor
                // javax.swing.JOptionPane.showMessageDialog(this, "Debe seleccionar un tutor
                // para el menor.");
                // return null;
            }
        }

        // 3. Construir mapa de datos
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("fullname", fullname);
        data.put("surname1", surname1);
        data.put("surname2", txtSurname2.getText().trim());
        data.put("sex", comboSex.getSelectedItem().toString());
        data.put("birthDate", birthDate);
        data.put("nationality", txtNationality.getText().trim());
        data.put("docType", comboDocType.getSelectedItem().toString());
        data.put("docNumber", docNumber);
        data.put("supportNumber", txtSupportNumber.getText().trim());
        data.put("address", txtAddress.getText().trim());
        data.put("city", txtCity.getText().trim());
        data.put("country", txtCountry.getText().trim());
        data.put("phone", txtPhone.getText().trim());
        data.put("email", txtEmail.getText().trim());
        data.put("isMinor", chkIsMinor.isSelected());
        data.put("guardianId", guardianId); // Puede ser null

        return data;
    }

    // --- LOGICA UI ---

    private void toggleMinorFields(boolean isMinor) {
        // Si es menor, ocultamos contacto y quizás documento/dirección si asumimos las
        // del padre
        // Por normativa, el menor NECESITA documento si tiene > 14 o si viaja fuera.
        // Simplificamos: Si es menor, ocultamos contacto obligatorio y mostramos
        // selector de tutor.

        // Mostrar/Ocultar selector de tutor
        ((JPanel) comboGuardian.getParent()).setVisible(isMinor);

        // Opcional: Ocultar datos de contacto directo del niño
        contactPanel.setVisible(!isMinor);

        revalidate();
        repaint();
    }

    private JPanel createInput(String labelText, JTextField field) {
        JPanel p = new JPanel(new java.awt.BorderLayout(0, 5));
        p.setBackground(Color.WHITE);
        JLabel l = new JLabel(labelText);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(100, 100, 100));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p.add(l, java.awt.BorderLayout.NORTH);
        p.add(field, java.awt.BorderLayout.CENTER);
        return p;
    }

    private void addSectionTitle(JPanel container, String title) {
        JLabel l = new JLabel(title);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(new Color(0, 153, 255)); // Azul corporativo
        l.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));

        // --- ALINEACIÓN IZQUIERDA ---
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        // ----------------------------
        container.add(l);
        container.add(sep);
        container.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    public void setOnSave(Runnable action) {
        this.onSave = action;
    }

    public void setOnCancel(Runnable action) {
        this.onCancel = action;
    }
}