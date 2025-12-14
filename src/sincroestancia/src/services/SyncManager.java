package sincroestancia.src.services;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import sincroestancia.src.models.DaySyncData;
import sincroestancia.src.models.OperationSyncData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * Controlador principal para la sincronización en segundo plano entre la base de datos local y Google Calendar.
 * * Implementa el patrón Singleton para asegurar que solo exista un proceso de sincronización activo.
 * * Utiliza un ScheduledExecutorService para ejecutar tareas periódicas sin bloquear la interfaz de usuario.
 * * Gestiona dos tipos de eventos:
 * - Eventos de Día: Muestran precio, estado (libre/ocupado) y temporada.
 * - Eventos de Operación: Muestran Check-in y Check-out con su estado de completado.
 * * @author Carlos Padilla Labella
 */
public class SyncManager {

    private static SyncManager instance;
    private final ScheduledExecutorService scheduler;
    private final DatabaseService dbService;
    private final GoogleCalendarService googleService;
    private JLabel statusLabel; 

    private String calendarId;
    private String credentialsPath;
    private boolean isRunning = false;

    /**
     * Constructor privado para imponer el patrón Singleton.
     * * Inicializa los servicios dependientes y el planificador de hilos (SingleThread).
     */
    private SyncManager() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.dbService = new DatabaseService();
        this.googleService = new GoogleCalendarService();
    }

    /**
     * Obtiene la instancia única del gestor de sincronización.
     * * @return La instancia Singleton de SyncManager.
     */
    public static SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }

    /**
     * Asigna la etiqueta de la interfaz gráfica donde se mostrarán los mensajes de estado.
     * * @param label Componente JLabel de la barra de estado o dashboard.
     */
    public void setStatusLabel(JLabel label) {
        this.statusLabel = label;
    }

    /**
     * Inicia el servicio de sincronización automática.
     * * Configura una tarea programada que se ejecuta cada 30 segundos con un retraso inicial de 5 segundos.
     * * Si el servicio ya está corriendo, no hace nada.
     */
    public void start() {
        if (isRunning) return; 
        loadConfig();
        scheduler.scheduleAtFixedRate(this::syncLoop, 5, 30, TimeUnit.SECONDS);
        isRunning = true;
    }

    /**
     * Fuerza una ejecución inmediata del ciclo de sincronización.
     * * Útil cuando el usuario realiza cambios manuales y quiere verlos reflejados al momento
     * sin esperar al siguiente ciclo programado.
     */
    public void forceSync() {
        updateStatus("Manual Sync Requested...");
        loadConfig();
        scheduler.execute(this::syncLoop);
    }
    
    /**
     * Carga o recarga la configuración de conexión desde la base de datos.
     * * Obtiene la ruta de credenciales y el ID del calendario destino.
     */
    private void loadConfig() {
        Map<String, String> config = dbService.get_google_config();
        this.credentialsPath = config.get("credentialsPath");
        this.calendarId = config.get("calendarId");
    }

    /**
     * Ciclo principal de sincronización (Core Loop).
     * * Flujo de ejecución:
     * - Verifica que existan configuraciones válidas.
     * - Comprueba la conexión y autenticación con Google API; si falla, intenta re-autenticar.
     * - Ejecuta secuencialmente la sincronización de Días y de Operaciones.
     * - Actualiza la interfaz con el número total de cambios realizados.
     * - Captura cualquier excepción para evitar que el hilo muera silenciosamente.
     */
    private void syncLoop() {

        if (credentialsPath == null || calendarId == null) {
            updateStatus("Sync Skipped: Config missing");
            return;
        }

        if (!googleService.isConnected()) {
            if (!googleService.authenticate(credentialsPath)) {
                updateStatus("Sync Error: Auth Failed");
                return;
            }
        }

        updateStatus("Syncing...");

        try {
            
            int changes = 0;
            changes += syncDays();
            changes += syncOperations();

            if (changes > 0) {
                updateStatus("Synced " + changes + " updates");
            } else {
                updateStatus("System Synced (No changes)");
            }

        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Sync Error: Check Console");
        }
    }

    /**
     * Actualiza el texto de la etiqueta de estado de forma segura para hilos.
     * * Utiliza SwingUtilities.invokeLater para asegurar que la manipulación de la UI
     * ocurra en el Event Dispatch Thread (EDT).
     * * @param text Mensaje a mostrar.
     */
    private void updateStatus(String text) {
        if (statusLabel != null) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + text));
        }
    }

    /**
     * Sincroniza la información de disponibilidad y precios (Eventos de día completo).
     * * Procesa solo los días marcados como 'no sincronizados' en la base de datos.
     * * Construye el título del evento incluyendo estado, nombre del huésped (si existe) y precio.
     * * Asigna colores según la temporada (Baja, Media, Alta).
     * * @return Número de eventos creados o actualizados.
     */
    private int syncDays() {

        int count = 0;
        List<DaySyncData> pendingDays = dbService.getUnsyncedFutureDays();

        for (DaySyncData day : pendingDays) {
            
            String title;
            
            if (day.guestName() != null) {
                title = String.format("[%s] %s - %.0f€", day.status().toUpperCase(), day.guestName(), day.price());
            } else {
                title = String.format("[%s] - %.0f€", day.status().toUpperCase(), day.price());
            }
            
            String desc = "Status: " + day.status() + "\nPrice: " + day.price();
            String colorId = getColorIdBySeason(day.season());
            String newId = upsertEvent(day.date(), day.googleEventId(), title, desc, colorId);
            
            if (newId != null) {
                dbService.markDayAsSynced(day.date(), newId);
                count++;
            }
        }
        return count;
    }

    /**
     * Sincroniza los eventos logísticos de Check-in y Check-out.
     * * Genera eventos visuales distintos para entrada (➡) y salida (⬅).
     * * Cambia el color y el icono cuando la operación se marca como completada ([✓]).
     * * Actualiza los IDs de eventos en la tabla de reservas para mantener el enlace.
     * * @return Número de operaciones sincronizadas.
     */
    private int syncOperations() {

        int count = 0;
        List<OperationSyncData> ops = dbService.getOperationsToSync();

        for (OperationSyncData op : ops) {
            
            String inTitle = (op.hasCheckIn() ? "[✓] " : "➡ ") + "CHECK-IN: " + op.guestName();
            String inColor = op.hasCheckIn() ? "10" : "7";
            String inDesc = "Reservation ID: " + op.id() + "\nStatus: " + (op.hasCheckIn() ? "COMPLETED" : "PENDING");
            String newInId = upsertEvent(op.checkInDate(), op.eventInId(), inTitle, inDesc, inColor);
            String outTitle = (op.hasCheckOut() ? "[✓] " : "⬅ ") + "CHECK-OUT: " + op.guestName();
            String outColor = op.hasCheckOut() ? "8" : "6";
            String outDesc = "Reservation ID: " + op.id() + "\nStatus: " + (op.hasCheckOut() ? "COMPLETED" : "PENDING");
            
            String newOutId = upsertEvent(op.checkOutDate(), op.eventOutId(), outTitle, outDesc, outColor);

            if (newInId != null || newOutId != null) {

                String saveIn = (newInId != null) ? newInId : op.eventInId();
                String saveOut = (newOutId != null) ? newOutId : op.eventOutId();
                
                dbService.updateReservationEventIds(op.id(), saveIn, saveOut);
                count++;
            }
        }

        return count;
    }

    /**
     * Método auxiliar para Crear o Actualizar (Upsert) un evento en Google Calendar.
     * * Estrategia:
     * - Si se proporciona un eventId, intenta actualizar el evento existente.
     * - Si la actualización falla (ej. evento borrado manualmente en Google), captura el error e intenta crearlo de nuevo.
     * - Si no hay eventId, crea uno nuevo directamente.
     * * @param date Fecha del evento (formato YYYY-MM-DD).
     * @param eventId ID existente de Google (puede ser null).
     * @param title Título del evento.
     * @param description Descripción detallada.
     * @param colorId ID de color de Google.
     * @return El ID del evento resultante (nuevo o existente), o null si falla.
     */
    private String upsertEvent(String date, String eventId, String title, String description, String colorId) {
        
        Event event = new Event()
                .setSummary(title)
                .setDescription(description)
                .setColorId(colorId);
        
        EventDateTime dt = new EventDateTime().setDate(new DateTime(date));

        event.setStart(dt);
        event.setEnd(dt);

        try {
            if (eventId != null && !eventId.isEmpty()) {
                try {
                    googleService.updateEvent(calendarId, eventId, event);
                    return eventId;
                } catch (Exception e) {
                    System.out.println("Event not found, recreating: " + date);
                }
            }
            
            return googleService.createEvent(calendarId, event);

        } catch (Exception e) {
            System.err.println("Error syncing event for " + date + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Convierte el nombre de la temporada interna a IDs de color de Google Calendar.
     * * Mapeo:
     * - Low -> 10 (Verde Albahaca)
     * - Average -> 6 (Naranja Mandarina)
     * - High -> 11 (Rojo Tomate)
     * - Default -> 8 (Gris Grafito)
     * * @param season Nombre de la temporada ('low', 'average', 'high').
     * @return String con el ID numérico del color.
     */
    private String getColorIdBySeason(String season) {
        
        if (season == null) return "8"; 
        
        return switch (season.toLowerCase()) {
            case "low" -> "10";
            case "average" -> "6";
            case "high" -> "11";
            default -> "8";
        };

    }
}