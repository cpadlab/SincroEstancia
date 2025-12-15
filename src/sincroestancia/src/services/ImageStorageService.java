package sincroestancia.src.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.swing.ImageIcon;

/**
 * Servicio responsable de la gestión física de archivos de imagen en el sistema de archivos local.
 * * Se encarga de la persistencia de las imágenes subidas por el usuario (como portadas),
 * copiándolas a un directorio gestionado ('data/uploads'), y provee mecanismos para
 * recuperarlas como objetos visuales de Swing.
 * * @author Carlos Padilla Labella
 */
public class ImageStorageService {
    
    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/.local/share/sincroestancia/data/uploads";
    private static final Path TARGET_PATH = Paths.get(UPLOAD_DIR);
    
    static {
        try {
            Files.createDirectories(TARGET_PATH);
            System.out.println("[info] Image storage directory created/verified: " + UPLOAD_DIR);
        } catch (IOException e) {
            System.err.println("[fatal] Could not create image storage directory. App will not function correctly: " + e.getMessage());
        }
    }
    /**
     * Copia un archivo seleccionado por el usuario al directorio de almacenamiento persistente.
     * * Pasos de implementación:
     * - Valida que el archivo fuente exista y no sea nulo.
     * - Garantiza que el directorio destino ('data/uploads') exista, creándolo si es necesario.
     * - Copia el archivo físico reemplazando cualquier existente con el mismo nombre.
     * - Devuelve la ruta relativa resultante para ser almacenada en la base de datos.
     * * @param source_file El archivo original seleccionado (generalmente vía JFileChooser).
     * @return La ruta relativa (String) donde se guardó la imagen, o null si falló la operación.
     */
    public String saveImage(File source_file) {
        
        if (source_file == null || !source_file.exists()) {
            System.err.println("[warning] The source file is invalid or does not exist.");
            return null;
        }

        try {
            
            Path source_path = source_file.toPath();
            String file_name = source_file.getName();

            Path targetDir = Paths.get(UPLOAD_DIR);

            Files.createDirectories(targetDir);
            Path target_path = targetDir.resolve(file_name);

            Files.copy(source_path, target_path, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[info] File successfully copied to: " + target_path.toString());

            return target_path.toString();

        } catch (IOException e) {
            System.err.println("[error] Error copying file: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Carga una imagen desde el disco y la convierte en un componente utilizable por Swing.
     * * Pasos de implementación:
     * - Valida que la ruta proporcionada no esté vacía.
     * - Verifica la existencia física del archivo en el disco.
     * - Si existe, instancia y devuelve un ImageIcon.
     * - Si no existe o hay error, registra el problema en el log y devuelve null.
     * * @param image_path La ruta relativa o absoluta al archivo de imagen.
     * @return Un objeto ImageIcon listo para la UI, o null si el archivo no es válido.
     */
    public ImageIcon loadImageIcon(String image_path) {
        
        if (image_path == null || image_path.isEmpty()) {
            System.err.println("[warning] The image path is null or empty.");
            return null;
        }

        File imageFile = new File(image_path);

        if (imageFile.exists() && imageFile.isFile()) {
            return new ImageIcon(image_path);
        } else {
            System.err.println("[error] The file does not exist on the disk.: " + image_path);
            return null;
        }
    }
    
}
