package sincroestancia.src.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Gestor centralizado para la base de datos SQLite.
 * * Esta clase implementa un patrón Singleton simplificado para mantener una única 
 * conexión abierta durante el ciclo de vida de la aplicación.
 * * Sus responsabilidades son:
 * - Establecer la conexión JDBC con el archivo local.
 * - Proveer acceso a la conexión para los repositorios y servicios.
 * - Ejecutar el script de inicialización (DDL) para crear las tablas necesarias.
 * - Cerrar la conexión de forma segura al salir.
 * * @author Carlos Padilla Labella
 * @version 1.0
 */
public class DatabaseManager {
    
    private static Connection conn = null;

    private static final String USER_DATA_DIR = 
            System.getProperty("user.home") + 
            java.io.File.separator + ".local" + 
            java.io.File.separator + "share" + 
            java.io.File.separator + "sincroestancia" + 
            java.io.File.separator + "data" + 
            java.io.File.separator;
    
    private static final String DB_FILENAME = "sqlite.db"; 
    private static final String DATABASE_FULL_PATH = USER_DATA_DIR + DB_FILENAME;
    private static final String DATABASE_URL = "jdbc:sqlite:" + DATABASE_FULL_PATH;
    private static final String SQL_INIT_SCRIPT_FILEPATH = "/sincroestancia/assets/database/init-databases.sql";
    
    /**
     * Establece la conexión física con el archivo de base de datos local.
     * * Pasos de implementación:
     * - Verifica si la conexión ya está establecida (para evitar reconexiones).
     * - Carga dinámicamente el driver JDBC de SQLite.
     * - Intenta conectar a la URL definida en DATABASE_URL.
     * - Si ocurre un error crítico (falta driver o error SQL), termina la aplicación con código 1.
     */
    public static void connect() {
        
        if (conn != null) { return; }

        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(USER_DATA_DIR));
            System.out.println("[info] User data directory created/verified: " + USER_DATA_DIR);
        } catch (java.io.IOException e) {
            System.err.println("[error] Could not create user data directory: " + e.getMessage());
            System.exit(1); 
        }

        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("[debug] Attempting to connect to URL: " + DATABASE_URL);
            conn = DriverManager.getConnection(DATABASE_URL);
            System.out.println("[info] Connection to SQLite successfully established.");
        } catch (SQLException e) {
            System.err.println("[error] Error connecting to SQLite: " + e.getMessage());
            System.exit(1); 
        } catch (ClassNotFoundException e) {
            System.err.println("[error] SQLite driver not found (missing .jar in Libraries).");
            System.exit(1);
        }
    }

    /**
     * Cierra la conexión activa con la base de datos de manera segura.
     * * Generalmente invocado por el Shutdown Hook de la clase principal al cerrar la app
     * para evitar bloqueos en el archivo .db.
     */
    public static void disconnect() {
        try {
            if (conn != null) {
                conn.close();
                System.out.println("[info] Connection to SQLite closed");
            }
        } catch (SQLException ex) {
            System.err.println("[error] Error closing connection: " + ex.getMessage());
        }
    }

    /**
     * Obtiene la instancia única de la conexión a la base de datos.
     * * Si la conexión no existe o está cerrada, intenta reconectar automáticamente.
     * * @return El objeto Connection activo para realizar consultas.
     */
    public static Connection get_connection() {
        if (conn == null) { connect(); }
        return conn;
    }
    
    /**
     * Ejecuta el script SQL inicial para asegurar que la estructura de datos exista.
     * * Flujo de ejecución:
     * - Localiza el archivo 'init-databases.sql' dentro de los recursos del JAR.
     * - Lee el contenido completo del archivo usando UTF-8.
     * - Divide el contenido en comandos individuales usando el separador ';'.
     * - Itera y ejecuta cada comando SQL (CREATE TABLE, INSERT iniciales, etc.).
     * - Si el script no se encuentra o falla la ejecución, termina la aplicación.
     */
    public static void initialise_tables() {
        
        try (Statement stmt = conn.createStatement();
            
            InputStream is = DatabaseManager.class.getResourceAsStream(SQL_INIT_SCRIPT_FILEPATH)) {

           if (is == null) {
               System.err.println("[error] The script 'init.sql' could not be found in: " + SQL_INIT_SCRIPT_FILEPATH);
               System.exit(1);
           }

           String scriptCompleto = new String(is.readAllBytes(), StandardCharsets.UTF_8);
           String[] comandosSQL = scriptCompleto.split(";");

           for (String comando : comandosSQL) {
               if (!comando.trim().isBlank()) {
                   stmt.execute(comando);
               }
           }

           System.out.println("[info] Script 'init.sql' successfully executed. Tables verified/created.");

       } catch (Exception e) {
           System.err.println("[error] Fatal error when executing the 'init.sql' script: " + e.getMessage());
           System.exit(1);
       }
        
    }
    
}
