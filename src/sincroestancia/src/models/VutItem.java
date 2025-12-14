package sincroestancia.src.models;

/**
 * Modelo de datos que representa una Vivienda de Uso Turístico (VUT).
 * * Actúa como un objeto de transferencia de datos (DTO) para transportar
 * la información de una propiedad desde la base de datos hacia la interfaz
 * de usuario y los servicios de lógica.
 * * Almacena información esencial como credenciales, ubicación de recursos
 * visuales y metadatos de conexión.
 * * @author Carlos Padilla Labella
 */
public class VutItem {
    
    private int id;
    private String name;
    private String cover_path;
    private String url;
    private String apikey;

    /**
     * Constructor completo para instanciar una VUT.
     * * @param id Identificador único en la base de datos (Primary Key).
     * @param name Nombre descriptivo o comercial de la vivienda.
     * @param cover_path Ruta del sistema de archivos donde se aloja la imagen de portada.
     * @param url Enlace web asociado a la vivienda (ej. anuncio en plataforma).
     * @param apikey Clave de autenticación específica para servicios externos de esta vivienda.
     */
    public VutItem(int id, String name, String cover_path, String url, String apikey) {
        this.id = id;
        this.name = name;
        this.cover_path = cover_path;
        this.url = url;
        this.apikey = apikey;
    }
    
    /**
     * Obtiene la clave de API configurada para esta vivienda.
     * * @return La API Key como cadena de texto.
     */
    public String getApiKey() {
        return apikey;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public String getCoverPath() {
        return cover_path;
    }
    
    public String getURL() {
        return url;
    }

    @Override
    public String toString() {
        return this.name;
    }
    
}
