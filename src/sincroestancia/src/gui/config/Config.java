package sincroestancia.src.gui.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import java.io.File;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import sincroestancia.src.gui.Main;
import sincroestancia.src.models.VutItem;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.services.ImageStorageService;
import sincroestancia.src.utils.StyleUtils;
import sincroestancia.src.gui.components.ButtonUtils;
import sincroestancia.src.gui.config.components.GoogleConfigPanel;
import sincroestancia.src.gui.config.components.PriceConfigPanel;
import sincroestancia.src.gui.config.components.UsersConfigPanel;

/**
 * Panel principal de configuración del sistema.
 * * Organiza las diferentes opciones de administración mediante un sistema de pestañas (JTabbedPane):
 * 1. General: Edición de datos básicos de la vivienda (Nombre, Foto, URL).
 * 2. Calendario: Configuración de precios por temporada.
 * 3. Google Sync: Configuración de credenciales y conexión con Google Calendar.
 * * Detecta automáticamente cuándo el panel se hace visible para recargar los datos
 * de la base de datos sin necesidad de reiniciar la aplicación.
 * * @author Carlos Padilla Labella
 */
public class Config extends JPanel {

    private JTabbedPane tabbedPane;
    private JPanel vutSettingsPanel;
    private PriceConfigPanel priceConfigPanel;
    private GoogleConfigPanel googleConfigPanel;

    private JTextField txtName;
    private JLabel lblCoverPath;
    private JTextField txtUrl;
    private JTextField txtApiKey;
    private JLabel lblTitle;

    private DatabaseService db;
    private ImageStorageService storageService;
    private VutItem currentVut;
    private File newCoverFile;
    private UsersConfigPanel usersPanel;

    /**
     * Constructor.
     * * Inicializa los servicios de persistencia y almacenamiento.
     * * Construye la estructura visual de pestañas.
     */
    public Config() {
        db = new DatabaseService();
        storageService = new ImageStorageService();
        initComponents();
        usersPanel = new UsersConfigPanel();
    }

    /**
     * Método llamado desde Main para configurar permisos.
     * Corrige el error de "config_tabbed_pane cannot be resolved" usando "tabbedPane".
     */
    public void setSessionUser(Map<String, Object> user) {
        if (user == null) return;
        int userId = (int) user.get("id");
        String role = (String) user.get("type");
        usersPanel.setCurrentSessionUserId(userId);
        tabbedPane.remove(usersPanel);
        if ("admin".equals(role)) {
            tabbedPane.addTab("Gestión Usuarios", usersPanel);
        }
    }

    /**
     * Recupera los datos de la vivienda seleccionada actualmente en la sesión y rellena el formulario.
     * * Pasos de implementación:
     * - Consulta la base de datos para obtener el 'VutItem' activo.
     * - Propaga los datos al sub-panel de precios (PriceConfigPanel).
     * - Si existe una vivienda activa, rellena los campos de texto y actualiza el título.
     * - Si no hay vivienda seleccionada, limpia el formulario.
     */
    public void loadVutData() {
        
        this.currentVut = db.get_selected_vut_details();
        
        if (priceConfigPanel != null) {
            priceConfigPanel.setVut(this.currentVut);
        }

        if (currentVut != null) {
            lblTitle.setText("Configuración - " + currentVut.getName());
            txtName.setText(currentVut.getName());
            lblCoverPath.setText(currentVut.getCoverPath());
            txtUrl.setText(currentVut.getURL());
            txtApiKey.setText(currentVut.getApiKey());
        } else {
            lblTitle.setText("Configuración del Sistema");
            clearFields();
        }

    }

    /**
     * Resetea todos los campos visuales del formulario de configuración general.
     */
    private void clearFields() {
        txtName.setText("");
        lblCoverPath.setText("Sin imagen seleccionada");
        newCoverFile = null;
        txtUrl.setText("");
        txtApiKey.setText("");
    }

    /**
     * Inicializa y organiza los componentes visuales del panel.
     * * Configura el 'JTabbedPane' y añade un 'AncestorListener'.
     * * El AncestorListener es crucial: detecta cuando el usuario navega a esta pestaña
     * (desde el menú principal) y ejecuta 'loadVutData()' automáticamente, asegurando
     * que los datos mostrados estén siempre frescos.
     */
    private void initComponents() {
        
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        lblTitle = new JLabel("Configuración del Sistema");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(lblTitle, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        createVutSettingsPanel();
        tabbedPane.addTab("Vivienda (VUT)", vutSettingsPanel);
        
        priceConfigPanel = new PriceConfigPanel();
        tabbedPane.addTab("Calendario", priceConfigPanel);

        googleConfigPanel = new GoogleConfigPanel();
        tabbedPane.addTab("Google Sync", googleConfigPanel);

        add(tabbedPane, BorderLayout.CENTER);

        this.addAncestorListener(new javax.swing.event.AncestorListener() {
            
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                loadVutData();
            }

            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}
            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}

        });
    }

    /**
     * Construye el panel de la primera pestaña (Ajustes Generales).
     * * Incluye campos para nombre, imagen, URL y API Key.
     * * Añade los botones de acción principales: Guardar Cambios y Eliminar Vivienda.
     */
    private void createVutSettingsPanel() {

        vutSettingsPanel = new JPanel();
        vutSettingsPanel.setLayout(new BoxLayout(vutSettingsPanel, BoxLayout.Y_AXIS));
        vutSettingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        vutSettingsPanel.setBackground(Color.WHITE);

        txtName = createTextField();
        txtUrl = createTextField();
        txtApiKey = createTextField();

        vutSettingsPanel.add(createInputGroup("Nombre de la Vivienda", txtName));
        vutSettingsPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel coverPanel = new JPanel(new BorderLayout(10, 0));
        coverPanel.setBackground(Color.WHITE);

        JButton btnSelectCover = new JButton("Seleccionar Imagen");
        ButtonUtils.styleSecondary(btnSelectCover);
        btnSelectCover.addActionListener(this::selectCoverImage);

        lblCoverPath = new JLabel("Sin imagen");
        lblCoverPath.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        coverPanel.add(btnSelectCover, BorderLayout.WEST);
        coverPanel.add(lblCoverPath, BorderLayout.CENTER);

        vutSettingsPanel.add(createInputGroup("Portada (Imagen)", coverPanel));
        vutSettingsPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        vutSettingsPanel.add(createInputGroup("URL de la Página Web", txtUrl));
        vutSettingsPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        vutSettingsPanel.add(createInputGroup("API Key (sede.gob.es)", txtApiKey));
        vutSettingsPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnSave = new JButton("Guardar Cambios");
        ButtonUtils.stylePrimary(btnSave);
        btnSave.addActionListener(this::saveVutChanges);

        JButton btnDelete = new JButton("Eliminar Vivienda");
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDelete.setBackground(new Color(255, 235, 235));
        btnDelete.setForeground(new Color(220, 53, 69));
        btnDelete.setFocusPainted(false);
        btnDelete.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnDelete.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        ButtonUtils.styleDanger(btnDelete);
        btnDelete.addActionListener(this::deleteVut);

        btnPanel.add(btnSave);
        btnPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        btnPanel.add(btnDelete);

        vutSettingsPanel.add(btnPanel);
    }

    /**
     * Abre un diálogo de selección de archivos para actualizar la portada.
     * * Filtra por extensiones de imagen (.jpg, .png).
     */
    private void selectCoverImage(ActionEvent e) {
        
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter imageFilter = new FileNameExtensionFilter("Archivos de Imagen (.jpg, .png)", "jpg", "jpeg", "png");
        fileChooser.setFileFilter(imageFilter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            newCoverFile = fileChooser.getSelectedFile();
            lblCoverPath.setText(newCoverFile.getName());
        }

    }

    /**
     * Valida y persiste los cambios realizados en el formulario.
     * * Flujo:
     * - Verifica campos obligatorios (nombre y URL).
     * - Si se seleccionó una nueva imagen, la guarda en disco mediante 'ImageStorageService'.
     * - Actualiza el registro en la base de datos.
     * - Notifica a la ventana principal (Main) para que refresque el menú de navegación.
     */
    private void saveVutChanges(ActionEvent e) {
        
        if (currentVut == null)
            return;

        String name = txtName.getText().trim();
        String url = txtUrl.getText().trim();
        String key = txtApiKey.getText().trim();

        if (name.isEmpty() || url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre y la URL son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String coverPath = currentVut.getCoverPath();
        if (newCoverFile != null) {
            String savedPath = storageService.saveImage(newCoverFile);
            if (savedPath != null) {
                coverPath = savedPath;
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar la nueva imagen.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        boolean success = db.update_vut(currentVut.getId(), name, coverPath, url, key);
        
        if (success) {
            
            JOptionPane.showMessageDialog(this, "Datos actualizados correctamente.");
            
            lblTitle.setText("Configuración - " + name);
            lblTitle.setFont(StyleUtils.FONT_TITLE);
            lblTitle.setForeground(StyleUtils.COLOR_TEXT_SECONDARY);
            
            newCoverFile = null;

            refreshMainApp();

        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar los cambios.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Gestiona la eliminación completa de la vivienda.
     * * Pide confirmación explícita al usuario ya que la acción es irreversible.
     * * Si se confirma, borra la vivienda y todos sus datos dependientes (reservas, huéspedes, calendario).
     * * Redirige la navegación en la ventana principal mediante 'Main.onVutDeleted()'.
     */
    private void deleteVut(ActionEvent e) {
        
        if (currentVut == null)
            return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que quieres eliminar esta vivienda?\nEsta acción borrará todas las reservas y datos asociados.\nNo se puede deshacer.",
                "Eliminar Vivienda",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            
            boolean success = db.delete_vut(currentVut.getId());
            
            if (success) {
                
                JOptionPane.showMessageDialog(this, "Vivienda eliminada correctamente.");
                
                refreshMainApp();
                
                Main mainFrame = (Main) SwingUtilities.getWindowAncestor(this);
                
                if (mainFrame != null) {
                    mainFrame.onVutDeleted();
                }

            } else {
                JOptionPane.showMessageDialog(this, "Error al eliminar la vivienda.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Método auxiliar para comunicar cambios a la ventana principal.
     */
    private void refreshMainApp() {
        Main mainFrame = (Main) SwingUtilities.getWindowAncestor(this);
        if (mainFrame != null) {
            mainFrame.reload_vut_menu();
        }
    }

    /**
     * Crea un panel compuesto con una etiqueta (Label) encima y un componente de entrada debajo.
     * * @param label Texto de la etiqueta.
     * @param field Componente de entrada (TextField, Panel, etc.).
     * @return JPanel formateado.
     */
    private JPanel createInputGroup(String label, Component field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(80, 80, 80));

        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    /**
     * Genera un campo de texto con la fuente estándar de la aplicación.
     */
    private JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return tf;
    }

}
