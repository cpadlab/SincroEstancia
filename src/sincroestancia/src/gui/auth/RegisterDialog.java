package sincroestancia.src.gui.auth;

import java.awt.*;
import javax.swing.*;
import sincroestancia.src.services.DatabaseService;

/**
 * Dialogo modal para el registro inicial del primer usuario administrador.
 * * Esta clase se encarga de presentar la interfaz para crear la primera cuenta
 * del sistema cuando la base de datos de usuarios esta vacia. Gestiona la
 * validacion de la entrada de datos (usuario y contrasena) y realiza la
 * insercion a traves del servicio de base de datos.
 * * @author Carlos Padilla Labella
 */
public class RegisterDialog extends JDialog {

    private boolean isRegistered = false;
    private JTextField userField;
    private JPasswordField passField;
    private JPasswordField confirmPassField;
    private DatabaseService dbService;
    private java.util.Map<String, Object> registeredUser = null;

    /**
     * Constructor del dialogo de registro.
     * * Inicializa el servicio de base de datos y configura la interfaz grafica.
     * Establece el titulo de la ventana y su modalidad.
     * * @param parent El marco (Frame) padre del dialogo.
     */
    public RegisterDialog(Frame parent) {
        super(parent, "Bienvenido - Crear Admin", true);
        this.dbService = new DatabaseService();
        setupUI();
    }

    /**
     * Configura e inicializa los componentes visuales del dialogo.
     * * Define la distribucion (Layout) de los elementos, crea los campos de texto,
     * etiquetas y botones. Tambien configura los oyentes (listeners) para los
     * botones de registrar y cancelar, asi como el panel informativo superior.
     */
    private void setupUI() {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        userField = new JTextField();
        passField = new JPasswordField();
        confirmPassField = new JPasswordField();
        JButton btnRegister = new JButton("Registrar Admin");
        JButton btnCancel = new JButton("Salir");

        panel.add(new JLabel("Usuario:"));
        panel.add(userField);
        panel.add(new JLabel("Contraseña:"));
        panel.add(passField);
        panel.add(new JLabel("Confirmar:"));
        panel.add(confirmPassField);

        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel info = new JLabel("<html><center>Bienvenido a SincroEstancia.<br>Cree el primer usuario administrador para comenzar.</center></html>");
        info.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        infoPanel.add(info, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnCancel);
        btnPanel.add(btnRegister);

        add(infoPanel, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        btnRegister.addActionListener(e -> attemptRegister());
        btnCancel.addActionListener(e -> dispose());

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * Ejecuta la logica de registro cuando se pulsa el boton.
     * * Realiza las siguientes validaciones:
     * - Comprueba que los campos no esten vacios.
     * - Verifica que la contrasena y su confirmacion coincidan.
     * * Si las validaciones pasan, intenta registrar el usuario como 'admin' en la
     * base de datos. Si tiene exito, guarda los datos del usuario en memoria
     * para el auto-login y cierra el dialogo.
     */
    private void attemptRegister() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        String confirm = new String(confirmPassField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Rellene todos los campos.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (dbService.registerUser(user, pass, "admin")) {
            JOptionPane.showMessageDialog(this, "Usuario administrador creado correctamente.");
            registeredUser = new java.util.HashMap<>();
            registeredUser.put("username", user);
            registeredUser.put("type", "admin");
            
            isRegistered = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Error al crear usuario.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Obtiene los datos del usuario recien registrado.
     * * @return Un mapa con el nombre de usuario y su tipo (rol), o null si no
     * se ha completado el registro.
     */
    public java.util.Map<String, Object> getRegisteredUser() {
        return registeredUser;
    }

    /**
     * Verifica si el registro se ha completado con exito.
     * * @return true si el usuario se registro correctamente, false en caso contrario.
     */
    public boolean isRegistered() {
        return isRegistered;
    }
}