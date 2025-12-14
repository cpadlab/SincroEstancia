package sincroestancia;

import sincroestancia.src.gui.Main;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

import sincroestancia.src.database.DatabaseManager;

/**
 * Clase principal de entrada para la aplicación SincroEstancia.
 * * Esta clase se encarga de la inicialización de los componentes críticos antes
 * de mostrar la interfaz gráfica de usuario (GUI). Sus responsabilidades incluyen:
 * - Establecer la conexión con la base de datos SQLite.
 * - Verificar e inicializar las tablas necesarias.
 * - Cargar y aplicar la tipografía corporativa (Inter) globalmente.
 * - Configurar el tema visual (Look and Feel) FlatLaf.
 * - Gestionar el cierre seguro de recursos mediante Shutdown Hooks.
 * * @author Carlos Padilla Labella
 */
public class SincroEstancia {
    
    static final String INTER_TTF_FILE_PATH = "/sincroestancia/assets/fonts/inter.ttf";
    
    /**
     * Método principal de ejecución (Entry Point).
     * * Flujo de ejecución:
     * 1. Base de Datos: Conecta e inicializa las tablas mediante DatabaseManager.
     * 2. Estilos: Carga la fuente personalizada.
     * 3. Gestión de Recursos: Registra un Shutdown Hook para asegurar que la conexión 
     * a la base de datos se cierre correctamente al terminar la JVM.
     * 4. Look and Feel: Aplica el tema FlatIntelliJLaf para una estética moderna.
     * 5. GUI: Lanza la ventana principal Main dentro del Event Dispatch Thread (EDT)
     * de AWT/Swing para garantizar la seguridad de hilos.
     * * @param args Argumentos de línea de comandos (no utilizados actualmente).
     */
    public static void main(String[] args) {
       
        DatabaseManager.connect();
        DatabaseManager.initialise_tables();
        
        SincroEstancia.load_fonts();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseManager.disconnect();
        }));
        
        try {
            javax.swing.UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatIntelliJLaf());
            javax.swing.JFrame.setDefaultLookAndFeelDecorated(true);
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("[warning] Failed to initialize LaF");
        }
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main().setVisible(true);
            }
        });
        
    }
    
    /**
     * Carga la fuente 'Inter' desde los recursos y la establece como predeterminada
     * para todos los componentes de Swing.
     * * Pasos de implementación:
     * - Obtiene el flujo de entrada (InputStream) del archivo .ttf.
     * - Crea el objeto Font y lo registra en el GraphicsEnvironment local.
     * - Override Global: Itera sobre todas las claves del UIManager. 
     * Si una clave corresponde a un recurso de fuente, la reemplaza por la fuente cargada.
     * Esto evita tener que establecer `setFont()` componente por componente.
     */
    private static void load_fonts() {
        
        try {
            
            java.io.InputStream is = SincroEstancia.class.getResourceAsStream(INTER_TTF_FILE_PATH);
            
            if (is == null) {
                throw new java.io.IOException("[error] Source not found. Check the path.");
            }

            Font base_typography = Font.createFont(Font.TRUETYPE_FONT, is);
            Font app_typography = base_typography.deriveFont(12f);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(base_typography);

            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof FontUIResource) {
                    UIManager.put(key, new FontUIResource(app_typography));
                }
            }
            
            System.out.println("[debug] Inter TTF loaded correctly");

        } catch (FontFormatException | IOException e) {
            System.err.println("[warning] Error loading font (the app will use the default font): " + e.getMessage());
        }
        
    }
    
}
