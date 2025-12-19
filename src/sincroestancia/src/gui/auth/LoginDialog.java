package sincroestancia.src.gui.auth;

import java.awt.*;
import javax.swing.*;
import sincroestancia.src.services.DatabaseService;

/**
 * Diálogo modal de autenticación para el acceso a la aplicación SincroEstancia.
 * Esta clase presenta una interfaz gráfica para que los usuarios introduzcan sus
 * credenciales (nombre de usuario y contraseña). Se encarga de validar dicha
 * información contra la base de datos y gestionar el estado de la sesión antes
 * de permitir el acceso a la ventana principal.
 * * @author Carlos Padilla Labella
 */
public class LoginDialog extends JDialog {
    
    private boolean isAuthenticated = false;
    private JTextField userField;
    private JPasswordField passField;
    private DatabaseService dbService;
    private java.util.Map<String, Object> loggedUser = null;

    /**
     * Constructor del diálogo de inicio de sesión.
     * Inicializa el servicio de base de datos, configura las propiedades del diálogo
     * (título, modalidad) e invoca la construcción de la interfaz visual.
     * * @param parent La ventana padre sobre la cual se mostrará este diálogo (puede ser null).
     */
    public LoginDialog(Frame parent) {
        super(parent, "Iniciar Sesión - SincroEstancia", true);
        this.dbService = new DatabaseService();
        setupUI();
    }

    /**
     * Construye y organiza los componentes visuales del diálogo.
     * Configuración realizada:
     * - Usa un {@code BorderLayout} general y un {@code GridLayout} para los campos.</li>
     * - Añade campos para usuario y contraseña.</li>
     * - Configura los botones de "Entrar" y "Salir".</li>
     * - Establece el botón de "Entrar" como el predeterminado (activable con Enter).</li>
     * - Ajusta el tamaño y centra la ventana en la pantalla.</li>
     */
    private void setupUI() {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        userField = new JTextField();
        passField = new JPasswordField();
        JButton btnLogin = new JButton("Entrar");
        JButton btnCancel = new JButton("Salir");

        panel.add(new JLabel("Usuario:"));
        panel.add(userField);
        panel.add(new JLabel("Contraseña:"));
        panel.add(passField);
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnCancel);
        btnPanel.add(btnLogin);

        add(panel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        btnLogin.addActionListener(e -> attemptLogin());
        btnCancel.addActionListener(e -> dispose());
        
        getRootPane().setDefaultButton(btnLogin);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * Lógica principal de validación de credenciales.
     * Pasos que realiza:
     * - Recupera el texto de los campos de usuario y contraseña.</li>
     * - Consulta al {@link DatabaseService#login(String, String)} para verificar credenciales.</li>
     * - Si es correcto: Actualiza el estado {@code isAuthenticated}, guarda el usuario y cierra el diálogo.</li>
     * - Si es incorrecto: Muestra un mensaje de error y mantiene el diálogo abierto.</li>
     */
    private void attemptLogin() {

        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        loggedUser = dbService.login(user, pass);

        if (loggedUser != null) {
            isAuthenticated = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Obtiene los datos del usuario que ha iniciado sesión correctamente.
     * * @return Un Mapa con las claves "id", "username" y "type" (rol), 
     * o {@code null} si el login no fue exitoso.
     */
    public java.util.Map<String, Object> getLoggedUser() {
        return loggedUser;
    }

    /**
     * Verifica si el proceso de autenticación finalizó con éxito.
     * * @return {@code true} si las credenciales fueron validadas correctamente, 
     * {@code false} si el usuario canceló o falló el login.
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
}