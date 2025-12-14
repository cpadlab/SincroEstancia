package sincroestancia.src.gui.config.components;

import java.awt.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.services.GoogleCalendarService;
import sincroestancia.src.gui.components.ButtonUtils;

/**
 * Panel de configuración para la integración con la API de Google Calendar.
 * * Permite al usuario establecer las credenciales de OAuth 2.0 y seleccionar
 * con qué calendario remoto se sincronizarán las reservas.
 * * Utiliza hilos en segundo plano (SwingWorker) para manejar el proceso de
 * autenticación web sin bloquear la interfaz gráfica.
 * * @author Carlos Padilla Labella
 */
public class GoogleConfigPanel extends JPanel {

    private JTextField txtCredentialsPath;
    private JButton btnBrowse;
    private JButton btnConnect;
    private JComboBox<String> cmbCalendars;
    private JButton btnSave;
    private JLabel lblStatus;

    private DatabaseService dbService;
    private GoogleCalendarService googleService;
    
    private Map<String, String> loadedCalendars; 

    /**
     * Constructor.
     * * Inicializa servicios y carga la configuración existente si la hay.
     */
    public GoogleConfigPanel() {
        dbService = new DatabaseService();
        googleService = new GoogleCalendarService();
        initComponents();
        loadSavedConfig();
    }

    /**
     * Construye la interfaz gráfica del panel.
     * * Incluye instrucciones paso a paso para el usuario final.
     */
    private void initComponents() {
        
        setLayout(new BorderLayout(20, 20));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Sincronización con Google Calendar");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        JLabel help = new JLabel("<html>1. Descargue <b>credentials.json</b> desde Google Cloud Console (ID de cliente OAuth 2.0 para escritorio).<br>2. Seleccione el archivo a continuación.<br>3. Haga clic en Conectar para autorizar.<br>4. Seleccione el calendario a sincronizar.</html>");
        help.setForeground(Color.GRAY);
        formPanel.add(help, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1;
        formPanel.add(new JLabel("Archivo de Credenciales:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtCredentialsPath = new JTextField();
        txtCredentialsPath.setEditable(false);
        formPanel.add(txtCredentialsPath, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        btnBrowse = new JButton("Buscar...");
        formPanel.add(btnBrowse, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        btnConnect = new JButton("Conectar y cargar calendarios");
        ButtonUtils.stylePrimary(btnConnect);
        btnConnect.setBackground(new Color(66, 133, 244));
        formPanel.add(btnConnect, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Seleccionar calendario:"), gbc);

        gbc.gridx = 1; gbc.gridwidth = 2;
        cmbCalendars = new JComboBox<>();
        cmbCalendars.setEnabled(false);
        formPanel.add(cmbCalendars, gbc);

        gbc.gridx = 1; gbc.gridy = 4;
        lblStatus = new JLabel("No conectado");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(Color.RED);
        formPanel.add(lblStatus, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        btnSave = new JButton("Guardar configuración");
        ButtonUtils.stylePrimary(btnSave);
        btnSave.setEnabled(false);
        footer.add(btnSave);
        
        add(footer, BorderLayout.SOUTH);

        btnBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            int res = fc.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                txtCredentialsPath.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        btnConnect.addActionListener(e -> connectGoogle());
        btnSave.addActionListener(e -> saveConfig());
    }

    /**
     * Carga la configuración guardada previamente en la base de datos.
     * * Solo rellena la ruta del archivo de credenciales. No conecta automáticamente
     * por seguridad y para no abrir el navegador sin acción explícita del usuario.
     */
    private void loadSavedConfig() {
        Map<String, String> config = dbService.get_google_config();
        if (config.containsKey("credentialsPath")) {
            txtCredentialsPath.setText(config.get("credentialsPath"));
        }
    }

    /**
     * Inicia el proceso de conexión con Google API.
     * * Implementación asíncrona mediante SwingWorker:
     * - Valida que se haya seleccionado un archivo.
     * - DoInBackground: Llama al servicio de autenticación (que puede abrir el navegador) y recupera la lista de calendarios.
     * - Done: Actualiza la UI con el resultado (Éxito/Error) y rellena el ComboBox.
     */
    private void connectGoogle() {
        
        String path = txtCredentialsPath.getText();
        
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione primero el archivo credentials.json.");
            return;
        }

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            
            @Override
            protected Boolean doInBackground() throws Exception {
                lblStatus.setText("Conectando... Comprueba tu navegador.");
                lblStatus.setForeground(Color.ORANGE);
                
                boolean ok = googleService.authenticate(path);
                if (ok) {
                    loadedCalendars = googleService.getAvailableCalendars();
                }
                return ok;
            }

            @Override
            protected void done() {
                try {
                    
                    boolean success = get();
                    if (success) {
                        
                        lblStatus.setText("Conectado!");
                        lblStatus.setForeground(new Color(0, 150, 0));
                        
                        cmbCalendars.removeAllItems();
                        for (String name : loadedCalendars.keySet()) {
                            cmbCalendars.addItem(name);
                        }
                        cmbCalendars.setEnabled(true);
                        btnSave.setEnabled(true);
                        
                        JOptionPane.showMessageDialog(GoogleConfigPanel.this, "¡Conectado correctamente! Seleccione un calendario.");

                    } else {
                        lblStatus.setText("Error de conexión");
                        lblStatus.setForeground(Color.RED);
                        JOptionPane.showMessageDialog(GoogleConfigPanel.this, "Error en la autenticación. Comprueba el archivo de credenciales.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    /**
     * Guarda la configuración seleccionada en la base de datos.
     * * Mapea el nombre del calendario seleccionado a su ID técnico de Google
     * y persiste tanto el ID como la ruta de credenciales.
     */
    private void saveConfig() {
        
        String selectedName = (String) cmbCalendars.getSelectedItem();
        if (selectedName == null || loadedCalendars == null) return;

        String calendarId = loadedCalendars.get(selectedName);
        String credPath = txtCredentialsPath.getText();

        boolean ok = dbService.update_google_config(1, calendarId, credPath);
        
        if (ok) {
            JOptionPane.showMessageDialog(this, "¡Configuración guardada!\nID de Calendario: " + calendarId);
        } else {
            JOptionPane.showMessageDialog(this, "Error en la base de datos al guardar la configuración.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        
    }
}