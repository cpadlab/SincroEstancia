package sincroestancia.src.utils;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 * Clase de utilidades que define el Sistema de Diseño centralizado de la aplicación.
 * * Contiene las constantes globales para la paleta de colores y tipografías,
 * garantizando la consistencia visual (Look & Feel) en todas las pantallas.
 * * Facilita el mantenimiento de la interfaz, permitiendo cambios globales
 * de estilo modificando únicamente los valores en esta clase.
 * * @author Carlos Padilla Labella
 */
public class StyleUtils {

    public static final Color COLOR_PRIMARY = new Color(0, 122, 204);
    public static final Color COLOR_PRIMARY_HOVER = new Color(0, 100, 180);
    
    public static final Color COLOR_SECONDARY = new Color(245, 245, 245);
    public static final Color COLOR_SECONDARY_HOVER = new Color(225, 225, 225);
    public static final Color COLOR_TEXT_SECONDARY = new Color(50, 50, 50);

    public static final Color COLOR_DANGER = new Color(220, 53, 69);
    public static final Color COLOR_DANGER_HOVER = new Color(200, 35, 50);
    
    public static final Color COLOR_SUCCESS = new Color(40, 167, 69);
    public static final Color COLOR_SUCCESS_HOVER = new Color(30, 130, 55);

    public static final Color COLOR_BORDER = new Color(220, 220, 220);

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);
    
    /**
     * Aplica un margen interior (padding) a un componente Swing sin sobrescribir su borde actual.
     * * Pasos de implementación:
     * - Obtiene el borde actual del componente.
     * - Crea un borde vacío (EmptyBorder) con las dimensiones especificadas.
     * - Si ya existe un borde, crea un borde compuesto (CompoundBorder) combinando el actual (exterior)
     * y el nuevo borde vacío (interior).
     * - Si no existe borde previo, aplica directamente el borde vacío.
     * * @param c El componente Swing al que se aplicará el padding.
     * @param top Píxeles de margen superior.
     * @param left Píxeles de margen izquierdo.
     * @param bottom Píxeles de margen inferior.
     * @param right Píxeles de margen derecho.
     */
    public static void addPadding(JComponent c, int top, int left, int bottom, int right) {
        
        Border current = c.getBorder();
        Border empty = BorderFactory.createEmptyBorder(top, left, bottom, right);
        
        if (current == null) {
            c.setBorder(empty);
        } else {
            c.setBorder(BorderFactory.createCompoundBorder(current, empty));
        }

    }
}