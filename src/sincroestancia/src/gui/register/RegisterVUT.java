package sincroestancia.src.gui.register;

import java.awt.HeadlessException;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import sincroestancia.src.gui.Main;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.services.ImageStorageService;
import sincroestancia.src.gui.components.ButtonUtils;

/**
 * Panel de formulario para el registro de nuevas propiedades (VUTs).
 * * Esta clase gestiona la interfaz gráfica donde el usuario introduce los datos
 * de una nueva vivienda turística.
 * * Sus responsabilidades incluyen:
 * - Validación de campos de entrada (Nombre, URL, API Key).
 * - Selección y previsualización de la ruta de la imagen de portada.
 * - Coordinación con ImageStorageService para guardar la imagen físicamente.
 * - Coordinación con DatabaseService para registrar la entidad.
 * - Navegación post-registro hacia el Dashboard.
 * * @author Carlos Padilla Labella
 */
public class RegisterVUT extends javax.swing.JPanel {

    private File submit_image;
    private JPanel container_panel;

    private final String name_placeholder = "Introduzca el nombre de su VUT";
    private final String url_placeholder = "Introduzca el enlace de su VUT (airbnb, booking, etc)";
    private final String apikey_placeholder = "Introduzca la clave API de su alojamiento";

    private DatabaseService db_service;
    private ImageStorageService storage_service;

    private Main main_frame;

    /**
     * Constructor principal del panel de registro.
     * * Inicializa los componentes generados automáticamente y configura el layout manual
     * para los campos del formulario.
     * * Aplica los estilos visuales estandarizados (ButtonUtils) a los botones.
     * * @param main_container_panel Referencia al contenedor CardLayout principal.
     * @param main_frame Referencia a la ventana principal para actualizar el menú tras el registro.
     */
    public RegisterVUT(JPanel main_container_panel, Main main_frame) {

        initComponents();

        register_form_panel.setLayout(
                new javax.swing.BoxLayout(register_form_panel, javax.swing.BoxLayout.PAGE_AXIS));

        register_form_panel.removeAll();

        register_form_panel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));
        register_form_panel.add(Box.createVerticalGlue());

        register_form_panel.add(title_panel);
        register_form_panel.add(Box.createRigidArea(new java.awt.Dimension(0, 10)));
        register_form_panel.add(name_panel);
        register_form_panel.add(Box.createRigidArea(new java.awt.Dimension(0, 10)));
        register_form_panel.add(apikey_panel);
        register_form_panel.add(Box.createRigidArea(new java.awt.Dimension(0, 10)));
        register_form_panel.add(link_panel);
        register_form_panel.add(Box.createRigidArea(new java.awt.Dimension(0, 10)));
        register_form_panel.add(cover_panel);
        register_form_panel.add(Box.createRigidArea(new java.awt.Dimension(0, 10)));
        register_form_panel.add(submit_panel);

        register_form_panel.add(Box.createVerticalGlue());

        this.container_panel = main_container_panel;
        this.main_frame = main_frame;

        this.db_service = new DatabaseService();
        this.storage_service = new ImageStorageService();

        this.submit_image = null;

        name_input.setText(name_placeholder);
        link_input.setText(url_placeholder);

        submit_button.setMargin(new java.awt.Insets(8, 25, 8, 25));
        ButtonUtils.applyRoundedStyle(submit_button);

        cover_select_button.setMargin(new java.awt.Insets(8, 25, 8, 25));
        ButtonUtils.applyRoundedStyle(cover_select_button);

    }

    /**
     * Restablece todos los campos del formulario a sus valores predeterminados (placeholders).
     * * Se llama después de un registro exitoso para dejar el formulario limpio para el siguiente uso.
     */
    private void reset_form_fields() {
        name_input.setText(name_placeholder);
        link_input.setText(url_placeholder);
        apikey_input.setText(apikey_placeholder);
        cover_selected_label.setText("Sin selección");
        this.submit_image = null;
    }


    private void initComponents() {

        register_form_panel = new javax.swing.JPanel();
        title_panel = new javax.swing.JPanel();
        title = new javax.swing.JLabel();
        name_panel = new javax.swing.JPanel();
        name_label = new javax.swing.JLabel();
        name_input = new javax.swing.JTextField();
        name_separator = new javax.swing.JSeparator();
        apikey_panel = new javax.swing.JPanel();
        apikey_label = new javax.swing.JLabel();
        apikey_input = new javax.swing.JTextField();
        apikey_separator = new javax.swing.JSeparator();
        link_panel = new javax.swing.JPanel();
        link_label = new javax.swing.JLabel();
        link_input = new javax.swing.JTextField();
        link_separator = new javax.swing.JSeparator();
        cover_panel = new javax.swing.JPanel();
        cover_label = new javax.swing.JLabel();
        cover_separator = new javax.swing.JSeparator();
        cover_select_button = new javax.swing.JButton();
        cover_selected_label = new javax.swing.JLabel();
        submit_panel = new javax.swing.JPanel();
        submit_button = new javax.swing.JButton();
        right_bg = new javax.swing.JPanel();

        setLayout(new java.awt.GridLayout(1, 2));

        register_form_panel.setBackground(new java.awt.Color(255, 255, 255));
        register_form_panel.setLayout(new javax.swing.BoxLayout(register_form_panel, javax.swing.BoxLayout.Y_AXIS));

        title_panel.setBackground(new java.awt.Color(255, 255, 255));
        title_panel.setMaximumSize(new java.awt.Dimension(550, 50));
        title_panel.setMinimumSize(new java.awt.Dimension(300, 0));
        title_panel.setPreferredSize(new java.awt.Dimension(300, 80));

        title.setFont(new java.awt.Font("Segoe UI", 1, 24));
        title.setText("Registrar Apartamento (VUT)");
        title.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout title_panelLayout = new javax.swing.GroupLayout(title_panel);
        title_panel.setLayout(title_panelLayout);
        title_panelLayout.setHorizontalGroup(
                title_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(title_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(title_panelLayout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(title)
                                        .addGap(0, 0, Short.MAX_VALUE))));
        title_panelLayout.setVerticalGroup(
                title_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 50, Short.MAX_VALUE)
                        .addGroup(title_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(title_panelLayout.createSequentialGroup()
                                        .addGap(0, 0, Short.MAX_VALUE)
                                        .addComponent(title)
                                        .addGap(0, 0, Short.MAX_VALUE))));

        register_form_panel.add(title_panel);

        name_panel.setBackground(new java.awt.Color(255, 255, 255));
        name_panel.setMaximumSize(new java.awt.Dimension(550, 80));
        name_panel.setMinimumSize(new java.awt.Dimension(300, 80));
        name_panel.setPreferredSize(new java.awt.Dimension(300, 80));

        name_label.setFont(new java.awt.Font("Segoe UI", 1, 14));
        name_label.setText("¿Cuál es el nombre de su VUT?");

        name_input.setForeground(new java.awt.Color(115, 115, 115));
        name_input.setText("Introduzca el nombre de su VUT");
        name_input.setBorder(null);

        name_separator.setForeground(new java.awt.Color(115, 115, 115));

        javax.swing.GroupLayout name_panelLayout = new javax.swing.GroupLayout(name_panel);
        name_panel.setLayout(name_panelLayout);
        name_panelLayout.setHorizontalGroup(
                name_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(name_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        name_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(name_separator)
                                                .addGroup(name_panelLayout.createSequentialGroup()
                                                        .addComponent(name_label)
                                                        .addGap(0, 336, Short.MAX_VALUE))
                                                .addComponent(name_input))
                                .addContainerGap()));
        name_panelLayout.setVerticalGroup(
                name_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(name_panelLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(name_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(name_input, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(name_separator, javax.swing.GroupLayout.PREFERRED_SIZE, 10,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)));

        register_form_panel.add(name_panel);

        apikey_panel.setBackground(new java.awt.Color(255, 255, 255));
        apikey_panel.setMaximumSize(new java.awt.Dimension(550, 80));
        apikey_panel.setMinimumSize(new java.awt.Dimension(300, 80));

        apikey_label.setFont(new java.awt.Font("Segoe UI", 1, 14));
        apikey_label.setText("¿Cuál es su clave API (sede.interior.gob)?");

        apikey_input.setForeground(new java.awt.Color(115, 115, 115));
        apikey_input.setText("Introduzca la clave API de su alojamiento");
        apikey_input.setBorder(null);

        apikey_separator.setForeground(new java.awt.Color(115, 115, 115));

        javax.swing.GroupLayout apikey_panelLayout = new javax.swing.GroupLayout(apikey_panel);
        apikey_panel.setLayout(apikey_panelLayout);
        apikey_panelLayout.setHorizontalGroup(
                apikey_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(apikey_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(apikey_panelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(apikey_separator)
                                        .addGroup(apikey_panelLayout.createSequentialGroup()
                                                .addComponent(apikey_label)
                                                .addGap(0, 265, Short.MAX_VALUE))
                                        .addComponent(apikey_input))
                                .addContainerGap()));
        apikey_panelLayout.setVerticalGroup(
                apikey_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(apikey_panelLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(apikey_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(apikey_input, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(apikey_separator, javax.swing.GroupLayout.PREFERRED_SIZE, 10,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)));

        register_form_panel.add(apikey_panel);

        link_panel.setBackground(new java.awt.Color(255, 255, 255));
        link_panel.setMaximumSize(new java.awt.Dimension(550, 80));
        link_panel.setMinimumSize(new java.awt.Dimension(300, 80));

        link_label.setFont(new java.awt.Font("Segoe UI", 1, 14));
        link_label.setText("¿Dónde se puede reservar su VUT?");

        link_input.setForeground(new java.awt.Color(115, 115, 115));
        link_input.setText("Introduzca el enlace de su apartamento");
        link_input.setBorder(null);

        link_separator.setForeground(new java.awt.Color(115, 115, 115));

        javax.swing.GroupLayout link_panelLayout = new javax.swing.GroupLayout(link_panel);
        link_panel.setLayout(link_panelLayout);
        link_panelLayout.setHorizontalGroup(
                link_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(link_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(
                                        link_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(link_separator)
                                                .addGroup(link_panelLayout.createSequentialGroup()
                                                        .addComponent(link_label)
                                                        .addGap(0, 309, Short.MAX_VALUE))
                                                .addComponent(link_input))
                                .addContainerGap()));
        link_panelLayout.setVerticalGroup(
                link_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(link_panelLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(link_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(link_input, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(link_separator, javax.swing.GroupLayout.PREFERRED_SIZE, 10,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)));

        register_form_panel.add(link_panel);

        cover_panel.setBackground(new java.awt.Color(255, 255, 255));
        cover_panel.setMaximumSize(new java.awt.Dimension(550, 80));
        cover_panel.setMinimumSize(new java.awt.Dimension(300, 80));

        cover_label.setFont(new java.awt.Font("Segoe UI", 1, 14));
        cover_label.setText("¿Dónde se puede reservar su VUT?");

        cover_separator.setForeground(new java.awt.Color(115, 115, 115));

        cover_select_button.setText("Seleccionar foto");
        ButtonUtils.styleSecondary(cover_select_button); 
        cover_select_button.addActionListener(this::cover_select_buttonActionPerformed);

        cover_selected_label.setForeground(new java.awt.Color(115, 115, 115));
        cover_selected_label.setText("Sin selección");

        javax.swing.GroupLayout cover_panelLayout = new javax.swing.GroupLayout(cover_panel);
        cover_panel.setLayout(cover_panelLayout);
        cover_panelLayout.setHorizontalGroup(
                cover_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(cover_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(cover_panelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(cover_separator)
                                        .addGroup(cover_panelLayout.createSequentialGroup()
                                                .addGroup(cover_panelLayout
                                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(cover_label)
                                                        .addGroup(cover_panelLayout.createSequentialGroup()
                                                                .addComponent(cover_select_button)
                                                                .addPreferredGap(
                                                                        javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(cover_selected_label)))
                                                .addGap(0, 309, Short.MAX_VALUE)))
                                .addContainerGap()));
        cover_panelLayout.setVerticalGroup(
                cover_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(cover_panelLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(cover_label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(cover_panelLayout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(cover_select_button, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
                                                javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cover_selected_label))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cover_separator, javax.swing.GroupLayout.PREFERRED_SIZE, 10,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)));

        register_form_panel.add(cover_panel);

        submit_panel.setBackground(new java.awt.Color(255, 255, 255));
        submit_panel.setMaximumSize(new java.awt.Dimension(550, 50));
        submit_panel.setMinimumSize(new java.awt.Dimension(300, 50));

        submit_button.setBackground(new java.awt.Color(0, 153, 255));
        submit_button.setFont(new java.awt.Font("Segoe UI", 1, 12));
        submit_button.setForeground(new java.awt.Color(255, 255, 255));
        submit_button.setActionCommand("Subir");
        submit_button.setAlignmentX(0.5F);
        submit_button.setBorder(null);

        submit_button.setText("Registrar Propiedad");
        ButtonUtils.stylePrimary(submit_button);
        submit_button.setPreferredSize(new java.awt.Dimension(200, 45)); 
        submit_button.addActionListener(this::submit_buttonActionPerformed);

        submit_button.addActionListener(this::submit_buttonActionPerformed);

        javax.swing.GroupLayout submit_panelLayout = new javax.swing.GroupLayout(submit_panel);
        submit_panel.setLayout(submit_panelLayout);
        submit_panelLayout.setHorizontalGroup(
                submit_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(submit_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(submit_button, javax.swing.GroupLayout.PREFERRED_SIZE, 150,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(394, Short.MAX_VALUE)));
        submit_panelLayout.setVerticalGroup(
                submit_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(submit_panelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(submit_button, javax.swing.GroupLayout.PREFERRED_SIZE, 35,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(9, Short.MAX_VALUE)));

        submit_button.getAccessibleContext().setAccessibleName("Registrar");
        submit_button.getAccessibleContext().setAccessibleDescription("");

        register_form_panel.add(submit_panel);

        add(register_form_panel);

        right_bg.setBackground(new java.awt.Color(240, 249, 255));

        javax.swing.GroupLayout right_bgLayout = new javax.swing.GroupLayout(right_bg);
        right_bg.setLayout(right_bgLayout);
        right_bgLayout.setHorizontalGroup(
                right_bgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 500, Short.MAX_VALUE));
        right_bgLayout.setVerticalGroup(
                right_bgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 550, Short.MAX_VALUE));

        add(right_bg);
    }// </editor-fold>//GEN-END:initComponents

    private void submit_buttonActionPerformed(java.awt.event.ActionEvent evt) {

        String name = name_input.getText().trim();
        String url = link_input.getText().trim();
        String apikey = apikey_input.getText().trim();

        if (apikey.isEmpty() || apikey.equals(apikey_placeholder) || name.isEmpty() || name.equals(name_placeholder)
                || url.isEmpty() || url.equals(url_placeholder)) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, rellene todos los campos.",
                    "Campos Vacíos",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (this.submit_image == null) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, seleccione una imagen de portada.",
                    "Imagen Requerida",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {

            System.out.println("[debug] Saving image...");
            String cover_path = storage_service.saveImage(this.submit_image);

            if (cover_path == null) {
                JOptionPane.showMessageDialog(this,
                        "Error: No se pudo guardar la imagen de portada.",
                        "Error de Archivo",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            System.out.println("[debug] Registering in database...");
            int success = db_service.register_vut(name, cover_path, url, apikey);

            if (success > -1) {
                JOptionPane.showMessageDialog(this,
                        "VUT registrada exitosamente: " + name,
                        "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);

                if (this.main_frame != null) {
                    this.main_frame.reload_vut_menu();
                    this.main_frame.setCurrentSelectedVutID(success, name);

                    reset_form_fields();

                    this.main_frame.set_panel_view("DASHBOARD", name);
                }

            } else {
                JOptionPane.showMessageDialog(this,
                        "Error: No se pudo registrar la VUT en la base de datos.",
                        "Error de Base de Datos",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (HeadlessException e) {
            JOptionPane.showMessageDialog(this,
                    "Ha ocurrido un error inesperado: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cover_select_buttonActionPerformed(java.awt.event.ActionEvent evt) {

        JFileChooser file_chooser = new JFileChooser();
        FileNameExtensionFilter image_filter = new FileNameExtensionFilter("Archivos de Imagen (.jpg, .png)", "jpg",
                "jpeg", "png");

        file_chooser.setFileFilter(image_filter);
        file_chooser.setAcceptAllFileFilterUsed(false);

        int result = file_chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {

            this.submit_image = file_chooser.getSelectedFile();
            System.out.println("[info] Selected file: " + this.submit_image.getAbsolutePath());
            cover_selected_label.setText(this.submit_image.getAbsolutePath());

        } else if (result == JFileChooser.CANCEL_OPTION) {
            System.out.println("[info] Selection cancelled.");
            if (this.submit_image == null) {
                cover_selected_label.setText("Sin selección");
            }
        }
    }

    private javax.swing.JTextField apikey_input;
    private javax.swing.JLabel apikey_label;
    private javax.swing.JPanel apikey_panel;
    private javax.swing.JSeparator apikey_separator;
    private javax.swing.JLabel cover_label;
    private javax.swing.JPanel cover_panel;
    private javax.swing.JButton cover_select_button;
    private javax.swing.JLabel cover_selected_label;
    private javax.swing.JSeparator cover_separator;
    private javax.swing.JTextField link_input;
    private javax.swing.JLabel link_label;
    private javax.swing.JPanel link_panel;
    private javax.swing.JSeparator link_separator;
    private javax.swing.JTextField name_input;
    private javax.swing.JLabel name_label;
    private javax.swing.JPanel name_panel;
    private javax.swing.JSeparator name_separator;
    private javax.swing.JPanel register_form_panel;
    private javax.swing.JPanel right_bg;
    private javax.swing.JButton submit_button;
    private javax.swing.JPanel submit_panel;
    private javax.swing.JLabel title;
    private javax.swing.JPanel title_panel;
    
}
