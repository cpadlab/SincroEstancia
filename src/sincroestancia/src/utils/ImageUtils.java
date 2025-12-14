package sincroestancia.src.utils;

import java.awt.Image ;
import javax.swing.ImageIcon;

/**
 * Clase de utilidades para la gestión de recursos gráficos e imágenes.
 * * Proporciona métodos estáticos para facilitar la carga, redimensionamiento
 * y manipulación de iconos utilizados en la interfaz de usuario Swing.
 * * @author Carlos Padilla Labella
 */
public class ImageUtils {
    
    /**
    * Carga un archivo de imagen desde los recursos del proyecto y ajusta su tamaño.
    * * Pasos de implementación:
    * - Localiza el recurso utilizando el ClassLoader (compatible con ejecución desde JAR).
    * - Verifica la existencia del archivo; si no existe, notifica el error en consola.
    * - Carga la imagen original en memoria.
    * - Aplica una transformación de escalado suave (SCALE_SMOOTH) para mantener la calidad visual.
    * - Encapsula la imagen resultante en un nuevo ImageIcon listo para usar en componentes Swing.
    * * @param path Ruta relativa al recurso (ej. "/sincroestancia/assets/icons/mi_icono.png").
    * @param width Ancho deseado en píxeles.
    * @param height Alto deseado en píxeles.
    * @return Un objeto ImageIcon reescalado, o null si el recurso no se encuentra o ocurre un error.
    */
   public static ImageIcon get_scaled_icon(String path, int width, int height) {
       try {
           
           java.net.URL imgUrl = ImageUtils.class.getResource(path);
           if (imgUrl == null) {
               System.err.println("The resource could not be found.: " + path);
               return null;
           }

           ImageIcon originalIcon = new ImageIcon(imgUrl);
           Image image = originalIcon.getImage();
           Image newimg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);

           return new ImageIcon(newimg);

       } catch (Exception e) {
           System.err.println("[error] Error loading or scaling the icon: " + path);
           e.printStackTrace();
           return null;
       }
   }
    
}
