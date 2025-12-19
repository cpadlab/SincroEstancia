package sincroestancia.src.gui.config.components;

import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import sincroestancia.src.services.DatabaseService;
import sincroestancia.src.gui.components.ButtonUtils;

/**
 * Panel de configuracion para la gestion de usuarios del sistema.
 * * Este panel permite a los administradores visualizar, anadir, eliminar y
 * modificar los roles de los usuarios registrados en la base de datos.
 * Incluye logica de autoproteccion para impedir que el usuario activo
 * elimine su propia cuenta o modifique sus propios privilegios.
 * * @author Carlos Padilla Labella
 */
public class UsersConfigPanel extends JPanel {

    private JTable usersTable;
    private DefaultTableModel tableModel;
    private DatabaseService dbService;
    private int currentSessionUserId = -1;

    /**
     * Constructor del panel de gestion de usuarios.
     * * Inicializa el servicio de base de datos, construye la interfaz grafica
     * y realiza la primera carga de datos en la tabla.
     */
    public UsersConfigPanel() {
        this.dbService = new DatabaseService();
        initComponents();
        refreshTable();
    }

    /**
     * Establece el ID del usuario que esta usando la aplicacion.
     * * Este metodo es crucial para las restricciones de seguridad (RBAC).
     * Permite al panel saber quien esta operando para bloquear acciones
     * destructivas sobre la propia cuenta (como auto-eliminarse).
     * * @param id Identificador unico del usuario logueado.
     */
    public void setCurrentSessionUserId(int id) {
        this.currentSessionUserId = id;
    }

    /**
     * Inicializa y organiza los componentes visuales del panel.
     * * Configura la tabla de visualizacion (modelo no editable), la barra de
     * herramientas inferior y asigna los oyentes (listeners) a los botones
     * de accion (Crear, Borrar, Cambiar Rol, Refrescar).
     */
    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Gestión de Usuarios");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Usuario", "Rol"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        usersTable = new JTable(tableModel);
        usersTable.setRowHeight(30);
        usersTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(usersTable);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(230,230,230)));
        add(scrollPane, BorderLayout.CENTER);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBackground(Color.WHITE);

        JButton btnAdd = new JButton("Nuevo Usuario");
        JButton btnDelete = new JButton("Eliminar");
        JButton btnChangeRole = new JButton("Cambiar Rol");
        JButton btnRefresh = new JButton("Refrescar");

        ButtonUtils.stylePrimary(btnAdd);
        ButtonUtils.styleDanger(btnDelete);
        ButtonUtils.styleSecondary(btnChangeRole);
        ButtonUtils.styleSecondary(btnRefresh);

        btnAdd.addActionListener(e -> showAddUserDialog());
        btnRefresh.addActionListener(e -> refreshTable());
        
        btnDelete.addActionListener(e -> {
            int selectedRow = usersTable.getSelectedRow();
            if (selectedRow == -1) return;

            int userId = (int) tableModel.getValueAt(selectedRow, 0);
            
            if (userId == currentSessionUserId) {
                JOptionPane.showMessageDialog(this, "No puedes eliminar tu propio usuario.", "Acción Bloqueada", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, "¿Estás seguro de eliminar este usuario?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (dbService.deleteUser(userId)) refreshTable();
            }
        });

        btnChangeRole.addActionListener(e -> {
            int selectedRow = usersTable.getSelectedRow();
            if (selectedRow == -1) return;

            int userId = (int) tableModel.getValueAt(selectedRow, 0);
            String currentRole = (String) tableModel.getValueAt(selectedRow, 2);

            if (userId == currentSessionUserId) {
                JOptionPane.showMessageDialog(this, 
                    "No puedes cambiar tu propio rol desde esta sesión.\nDebe hacerlo otro administrador.", 
                    "Acción Bloqueada", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String newRole = currentRole.equals("admin") ? "user" : "admin";
            int confirm = JOptionPane.showConfirmDialog(this, 
                "El usuario pasará de '" + currentRole + "' a '" + newRole + "'. ¿Continuar?", 
                "Cambiar Rol", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                if (dbService.updateUserRole(userId, newRole)) refreshTable();
            }
        });

        toolbar.add(btnAdd);
        toolbar.add(btnDelete);
        toolbar.add(btnChangeRole);
        toolbar.add(btnRefresh);

        add(toolbar, BorderLayout.SOUTH);
    }

    /**
     * Recarga los datos de la tabla desde la base de datos.
     * * Limpia el modelo actual y solicita la lista completa de usuarios
     * al servicio de base de datos para mostrarlos actualizados.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Map<String, Object>> users = dbService.getAllUsers();
        for (Map<String, Object> user : users) {
            tableModel.addRow(new Object[]{
                user.get("id"),
                user.get("username"),
                user.get("type")
            });
        }
    }

    /**
     * Muestra un dialogo modal para registrar un nuevo usuario en el sistema.
     * * Solicita nombre, contrasena y rol. Realiza las validaciones basicas
     * antes de llamar al servicio de registro.
     */
    private void showAddUserDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nuevo Usuario", true);
        dialog.setLayout(new GridLayout(4, 2, 10, 10));
        
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"user", "admin"});
        JButton btnSave = new JButton("Guardar");

        dialog.add(new JLabel(" Usuario:")); dialog.add(userField);
        dialog.add(new JLabel(" Contraseña:")); dialog.add(passField);
        dialog.add(new JLabel(" Rol:")); dialog.add(roleCombo);
        dialog.add(new JLabel("")); dialog.add(btnSave);

        btnSave.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            String r = (String) roleCombo.getSelectedItem();
            
            if (!u.isEmpty() && !p.isEmpty()) {
                if (dbService.registerUser(u, p, r)) {
                    refreshTable();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Error al crear (¿usuario duplicado?)");
                }
            }
        });

        dialog.setSize(300, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}