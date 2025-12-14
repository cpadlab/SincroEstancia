package sincroestancia.src.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import sincroestancia.src.gui.config.Config;
import sincroestancia.src.gui.dasboard.Dashboard;
import sincroestancia.src.gui.register.RegisterVUT;
import sincroestancia.src.gui.reports.ReportsPanel;
import sincroestancia.src.models.VutItem;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.services.SyncManager;

/**
 * Clase principal que gestiona la navegación, el ciclo de vida y la estructura visual de la aplicación.
 * * Extiende de JFrame y actúa como el contenedor raíz.
 * * Sus responsabilidades principales son:
 * - Gestionar el 'CardLayout' para la navegación entre pantallas (Dashboard, Registro, Configuración, Reportes).
 * - Mantener el estado global de la aplicación (Vivienda seleccionada actualmente).
 * - Inicializar y coordinar los servicios de backend (Base de datos y Sincronización).
 * - Renderizar la barra de estado inferior (Footer) y el menú superior.
 * * @author Carlos Padilla Labella
 */
public final class Main extends javax.swing.JFrame {

    private int current_selected_vut_id = -1;
    private String current_selected_vut_name = "";
    private boolean are_vuts_registered_global;
    private String current_visible_panel = "";
    
    private JLabel lblSyncStatus;

    private VutItem selected_vut;
    private RegisterVUT panel_register_vut_form;
    private Dashboard panel_dashboard;
    private ReportsPanel panel_reports;
    private Config panel_config;

    private DatabaseService db_service;

    /**
     * Constructor principal de la ventana.
     * * Flujo de inicialización:
     * 1. Inicializa componentes gráficos base (NetBeans).
     * 2. Instancia el servicio de base de datos.
     * 3. Configura el Layout principal añadiendo el Footer personalizado.
     * 4. Instancia y añade los paneles principales al gestor de tarjetas (CardLayout).
     * 5. Carga los datos iniciales y determina qué pantalla mostrar (Registro o Dashboard).
     * 6. Configura los listeners de navegación del menú.
     * 7. Inicia el hilo del servicio de sincronización en segundo plano.
     */
    public Main() {
        
        initComponents();
        
        this.setTitle("Sincro Estancia");
        
        db_service = new DatabaseService();
        
        setupMainLayoutWithFooter();
        
        panel_dashboard = new Dashboard();
        panel_register_vut_form = new RegisterVUT(container_panel, this);
        panel_config = new Config();
        panel_reports = new ReportsPanel();

        System.out.println("[info] Panels started correctly.");

        container_panel.add(panel_dashboard, "DASHBOARD");
        container_panel.add(panel_register_vut_form, "REGISTER VUT FORM");
        container_panel.add(panel_config, "CONFIG");
        container_panel.add(panel_reports, "REPORTS");

        loadInitialData();
        setupMenuListeners();
        startSyncService();

    }
    
    /**
     * Reorganiza la estructura del ContentPane para incluir una barra de estado inferior (Footer).
     * * Implementación:
     * - Retira el panel contenedor original.
     * - Crea un nuevo JPanel con BorderLayout.
     * - Coloca el contenedor de tarjetas en el CENTRO.
     * - Crea un panel inferior (SUR) con estilo visual similar a VSCode (Azul).
     * - Añade al footer la etiqueta de estado y el botón de sincronización manual.
     */
    private void setupMainLayoutWithFooter() {
        
        this.getContentPane().remove(container_panel);

        JPanel mainWrapper = new JPanel(new BorderLayout());
        mainWrapper.add(container_panel, BorderLayout.CENTER);
    
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        footerPanel.setBackground(new Color(0, 122, 204));
        footerPanel.setPreferredSize(new Dimension(getWidth(), 25));
        
        lblSyncStatus = new JLabel("Status: Initializing Sync Service...");
        lblSyncStatus.setForeground(Color.WHITE);
        lblSyncStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        javax.swing.JButton btnForceSync = new javax.swing.JButton("↻ Sync Now");
        btnForceSync.setFocusPainted(false);
        btnForceSync.setBorderPainted(false);
        btnForceSync.setContentAreaFilled(false);
        btnForceSync.setForeground(Color.WHITE);
        btnForceSync.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnForceSync.setFont(new Font("Segoe UI", Font.BOLD, 11));
        
        btnForceSync.addActionListener(e -> {
            
            btnForceSync.setEnabled(false);
            btnForceSync.setText("↻ Checking...");

            SyncManager.getInstance().forceSync();
            javax.swing.Timer timer = new javax.swing.Timer(2000, evt -> {
                btnForceSync.setEnabled(true);
                btnForceSync.setText("↻ Sync Now");
                ((javax.swing.Timer)evt.getSource()).stop();
            });

            timer.start();

        });
        
        footerPanel.add(btnForceSync);
        footerPanel.add(new javax.swing.JSeparator(javax.swing.SwingConstants.VERTICAL));
        footerPanel.add(lblSyncStatus);
        
        mainWrapper.add(footerPanel, BorderLayout.SOUTH);
        
        this.setContentPane(mainWrapper);
        this.revalidate();

    }
    
    /**
     * Vincula la interfaz gráfica con el gestor de sincronización (Singleton).
     * * Ejecuta el inicio del servicio en un hilo separado (SwingUtilities) para
     * no bloquear el arranque de la aplicación.
     */
    private void startSyncService() {
        SyncManager.getInstance().setStatusLabel(lblSyncStatus);
        SwingUtilities.invokeLater(() -> {
            new Thread(() -> SyncManager.getInstance().start()).start();
        });
    }

    /**
     * Carga los datos de persistencia y decide la pantalla inicial.
     * * Lógica:
     * - Intenta recuperar la última vivienda seleccionada desde la base de datos.
     * - Verifica si existen viviendas registradas en total.
     * - Si no hay viviendas -> Muestra pantalla de Registro.
     * - Si hay viviendas -> Carga el Dashboard con la vivienda seleccionada (o la primera disponible).
     */
    private void loadInitialData() {
        
        selected_vut = db_service.get_selected_vut_details();
        
        if (selected_vut != null) {
            this.current_selected_vut_id = selected_vut.getId();
            this.current_selected_vut_name = selected_vut.getName();
            System.out.println("[info] VUT loaded from DB: " + this.current_selected_vut_name);
        } else {
            System.out.println("[info] No VUT has been used yet.");
            this.current_selected_vut_id = -1;
            this.current_selected_vut_name = "Select VUT";
        }

        this.are_vuts_registered_global = db_service.are_vuts_registered();
        load_vuts_into_selector();

        if (!this.are_vuts_registered_global) {
            System.out.println("[info] There are no registered vuts.");
            set_panel_view("REGISTER VUT FORM", "Registrar VUT");

        } else {

            System.out.println("[info] There are registered vuts.");

            if (this.current_selected_vut_id == -1) {
                
                List<VutItem> vuts = this.db_service.get_all_vuts();
                
                if (vuts != null && !vuts.isEmpty()) {
                    VutItem firstVut = vuts.get(0);
                    setCurrentSelectedVutID(firstVut.getId(), firstVut.getName());
                    panel_dashboard.updateVutData(firstVut.getId());
                }

            }

            set_panel_view("DASHBOARD", current_selected_vut_name);
        }
    }

    /**
     * Configura los eventos de clic para los menús de navegación (Configuración y Reportes).
     * * Incluye validación: Si no hay viviendas registradas, bloquea el acceso y muestra alerta.
     */
    private void setupMenuListeners() {
        
        config_btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (are_vuts_registered_global) {
                    set_panel_view("CONFIG", "Configuración (" + current_selected_vut_name + ")");
                } else {
                    showNoVutWarning();
                }
            }
        });

        reports_btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (are_vuts_registered_global) {
                    set_panel_view("REPORTS", "Reportes (" + current_selected_vut_name + ")");
                } else {
                    showNoVutWarning();
                }
            }
        });
        
    }

    /**
     * Método centralizado para cambiar la vista actual del CardLayout.
     * * Actualiza también el título de la ventana principal para dar contexto al usuario.
     * * @param new_panel Clave del panel a mostrar (ej. "DASHBOARD").
     * @param new_title Título legible para la ventana.
     */
    public void set_panel_view(String new_panel, String new_title) {
        CardLayout cl = (CardLayout) container_panel.getLayout();
        cl.show(container_panel, new_panel);
        current_visible_panel = new_panel;
        this.setTitle("Sincro Estancia - " + new_title);
    }

    /**
     * Fuerza una recarga completa del menú de selección de viviendas.
     * * Se utiliza tras operaciones de creación o eliminación de propiedades.
     */
    public void reload_vut_menu() {
        System.out.println("[debug] Load the VUTs in the Menu...");
        this.are_vuts_registered_global = db_service.are_vuts_registered();
        load_vuts_into_selector();
    }

    /**
     * Actualiza la vivienda activa en la sesión y persiste el cambio en la base de datos.
     */
    public void setCurrentSelectedVutID(int new_selected_vut_id, String new_selected_vut_name) {
        
        this.current_selected_vut_name = new_selected_vut_name;
        this.current_selected_vut_id = new_selected_vut_id;
        
        dashboard_btn.setText("Dashboard");
        
        if (!this.db_service.update_selected_vut(new_selected_vut_id)) {
            System.err.println("[error] The selected VUT could not be saved in the database..");
        }
    }
    
    /**
     * Genera dinámicamente los elementos del menú "Dashboard" basándose en las viviendas disponibles.
     * * Crea un JMenuItem por cada propiedad.
     * * Al seleccionar una propiedad:
     * - Actualiza el ID global.
     * - Refresca los datos del Dashboard y Reportes.
     * - Gestiona la navegación: Si el usuario estaba en Configuración, pide confirmación antes de salir.
     */
    private void load_vuts_into_selector() {
        while (dashboard_btn.getItemCount() > 2) {
            dashboard_btn.remove(2);
        }

        if (!this.are_vuts_registered_global) {
            dashboard_btn.setText("Dashboard");
            return;
        }

        List<VutItem> vuts = this.db_service.get_all_vuts();
        
        for (VutItem vut : vuts) {
            JMenuItem vutMenuItem = new JMenuItem(vut.getName());
            
            vutMenuItem.addActionListener(e -> {
                
                int selected_id = vut.getId();
                setCurrentSelectedVutID(selected_id, vut.getName());
                
                panel_dashboard.updateVutData(selected_id);
                panel_reports.updateVut(selected_id);
                System.out.println("[info] Selected VUT switched to: " + vut.getName());

                if ("CONFIG".equals(this.current_visible_panel)) {
                    int confirm = JOptionPane.showConfirmDialog(Main.this,
                            "Are you sure you want to leave Configuration?",
                            "Leave Configuration",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);

                    if (confirm == JOptionPane.YES_OPTION) {
                        set_panel_view("DASHBOARD", this.current_selected_vut_name);
                    }
                } else {
                    set_panel_view("DASHBOARD", this.current_selected_vut_name);
                }
            });
            
            dashboard_btn.add(vutMenuItem);
        }
        
        dashboard_btn.setText("Dashboard");
        
        if (current_selected_vut_id != -1) {
             panel_dashboard.updateVutData(this.current_selected_vut_id);
             panel_reports.updateVut(this.current_selected_vut_id);
        }
    }
    
    /**
     * Gestiona el flujo de navegación tras la eliminación de una vivienda.
     * * Si no quedan viviendas, redirige al formulario de registro.
     * * Si quedan otras, selecciona automáticamente la siguiente disponible y va al Dashboard.
     */
    public void onVutDeleted() {
        
        System.out.println("[info] Handling post-deletion navigation...");
        
        List<VutItem> vuts = this.db_service.get_all_vuts();
        this.are_vuts_registered_global = !vuts.isEmpty();
        
        load_vuts_into_selector();

        if (vuts.isEmpty()) {
            this.current_selected_vut_id = -1;
            this.current_selected_vut_name = "Select VUT"; 
            panel_dashboard.updateVutData(-1);
            set_panel_view("REGISTER VUT FORM", "Registrar Vivienda");
        } else {
            VutItem nextVut = vuts.get(0);
            setCurrentSelectedVutID(nextVut.getId(), nextVut.getName());
            panel_dashboard.updateVutData(nextVut.getId());
            set_panel_view("DASHBOARD", nextVut.getName());
        }

    }
    
    /**
     * Muestra un diálogo modal de advertencia cuando se intenta acceder a secciones
     * que requieren una vivienda activa.
     */
    private void showNoVutWarning() {
        JOptionPane.showMessageDialog(Main.this,
            "You must register at least one property to access this section.",
            "Action not available",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void initComponents() {

        container_panel = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        
        dashboard_btn = new javax.swing.JMenu();
        reports_btn = new javax.swing.JMenu();
        config_btn = new javax.swing.JMenu();
        register_vut_btn = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(900, 600));
        setPreferredSize(new java.awt.Dimension(1024, 768));

        container_panel.setBackground(new java.awt.Color(255, 255, 255));
        container_panel.setLayout(new java.awt.CardLayout());
        getContentPane().add(container_panel, java.awt.BorderLayout.CENTER);

        jMenuBar1.setBorder(null);

        dashboard_btn.setText("Dashboard");
        
        register_vut_btn.setText("Nuevo VUT");
        register_vut_btn.addActionListener(new ActionListenerImpl());
        
        dashboard_btn.add(register_vut_btn);
        dashboard_btn.add(jSeparator1);
        jMenuBar1.add(dashboard_btn);
        reports_btn.setText("Reportes");
        jMenuBar1.add(reports_btn);
        config_btn.setText("Configuración");
        jMenuBar1.add(config_btn);

        setJMenuBar(jMenuBar1);

        pack();
    }

    private void register_vut_btnActionPerformed(java.awt.event.ActionEvent evt) {                                                 
        set_panel_view("REGISTER VUT FORM", "Registrar Vivienda");
    }                                                

    private javax.swing.JMenu config_btn;
    private javax.swing.JPanel container_panel;
    private javax.swing.JMenu dashboard_btn;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JMenuItem register_vut_btn;
    private javax.swing.JMenu reports_btn;

    private class ActionListenerImpl implements ActionListener {
        public ActionListenerImpl() { }
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            register_vut_btnActionPerformed(evt);
        }
    }
}