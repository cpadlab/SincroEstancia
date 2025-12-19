package sincroestancia.src.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Clase de utilidad para la gestion de seguridad y criptografia de contrasenas.
 * * Proporciona metodos estaticos para hashear (encriptar) contrasenas nuevas y
 * verificar contrasenas existentes utilizando el algoritmo BCrypt, garantizando
 * que las credenciales no se almacenen en texto plano.
 * * @author Carlos Padilla Labella
 */
public class PasswordUtils {
    
    /**
     * Genera un hash seguro para una contrasena en texto plano.
     * * Utiliza BCrypt con una sal (salt) generada aleatoriamente para cada
     * ejecucion, lo que protege contra ataques de diccionario y rainbow tables.
     * * @param plainTextPassword La contrasena tal cual la introduce el usuario.
     * @return El hash BCrypt resultante (String) listo para guardar en BD.
     */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    /**
     * Verifica si una contrasena en texto plano coincide con un hash almacenado.
     * * Realiza comprobaciones previas para asegurar que el hash almacenado
     * es valido (no nulo y con formato BCrypt correcto).
     * * @param plainTextPassword La contrasena introducida en el login.
     * @param storedHash El hash recuperado de la base de datos.
     * @return true si la contrasena coincide, false si es incorrecta o el hash es invalido.
     */
    public static boolean checkPassword(String plainTextPassword, String storedHash) {
        if (storedHash == null || !storedHash.startsWith("$2a$")) {
            return false;
        }
        return BCrypt.checkpw(plainTextPassword, storedHash);
    }
}