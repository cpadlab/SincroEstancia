package sincroestancia.src.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio encargado de gestionar la comunicación directa con la API de Google Calendar.
 * * Maneja el ciclo de vida completo de la autenticación OAuth 2.0 y las operaciones CRUD
 * (Crear, Leer, Actualizar) sobre los eventos del calendario.
 * * Utiliza la librería oficial 'Google API Client for Java'.
 * * @author Carlos Padilla Labella
 */
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "SincroEstancia Desktop";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.home") 
        + java.io.File.separator + ".local" 
        + java.io.File.separator + "share" 
        + java.io.File.separator + "sincroestancia" 
        + java.io.File.separator + "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    private Calendar service;

    /**
     * Realiza el flujo de autenticación OAuth 2.0 utilizando el archivo de credenciales JSON.
     * * Flujo de ejecución:
     * - Carga el archivo 'credentials.json' proporcionado por el usuario.
     * - Configura el almacén de datos (DataStore) en la carpeta 'tokens' para persistir la sesión.
     * - Configura el acceso 'offline' para obtener un Refresh Token (permite renovar acceso sin pedir login de nuevo).
     * - Levanta un servidor local en el puerto 8888 para recibir el callback de Google tras el login en el navegador.
     * - Si es la primera vez, abre el navegador; si ya hay token guardado, lo reutiliza.
     * * @param credentialsFilePath Ruta absoluta o relativa al archivo .json de credenciales de Google Cloud.
     * @return true si la autenticación fue exitosa y el servicio está listo; false en caso contrario.
     */
    public boolean authenticate(String credentialsFilePath) {
        try {
            
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            FileInputStream in = new FileInputStream(credentialsFilePath);
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                    .setAccessType("offline")
                    .build();

            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

            this.service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            return true;

        } catch (IOException | GeneralSecurityException e) {
            System.err.println("Error en autenticación Google: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Recupera la lista de todos los calendarios asociados a la cuenta de Google conectada.
     * * Útil para permitir al usuario seleccionar en qué calendario sincronizar las reservas.
     * * Implementación:
     * - Utiliza paginación (nextPageToken) para asegurar que se recuperan todos los calendarios
     * si el usuario tiene muchos.
     * - Prioriza el 'SummaryOverride' (nombre personalizado por el usuario) sobre el 'Summary' original.
     * * @return Un Mapa donde la Clave es el Nombre visible del calendario y el Valor es su ID técnico.
     */
    public Map<String, String> getAvailableCalendars() {
        
        Map<String, String> calendars = new HashMap<>();
        if (service == null) return calendars;

        try {
            String pageToken = null;
            do {
                
                CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
                List<CalendarListEntry> items = calendarList.getItems();

                for (CalendarListEntry calendarListEntry : items) {
                    String name = calendarListEntry.getSummaryOverride() != null ? calendarListEntry.getSummaryOverride() : calendarListEntry.getSummary();
                    calendars.put(name, calendarListEntry.getId());
                }

                pageToken = calendarList.getNextPageToken();

            } while (pageToken != null);
        } catch (IOException e) {
            System.err.println("Error listando calendarios: " + e.getMessage());
        }
        return calendars;
    }

    /**
     * Verifica si el servicio de Google Calendar ha sido inicializado correctamente.
     * * @return true si existe una instancia de cliente activa.
     */
    public boolean isConnected() {
        return service != null;
    }

    /**
     * Crea un nuevo evento en el calendario especificado.
     * * @param calendarId ID del calendario destino (ej. 'primary' o un ID específico).
     * @param event Objeto Event de la librería de Google con los datos (título, fechas, descripción).
     * @return El ID único del evento creado (necesario para guardarlo en BD local y poder actualizarlo luego).
     * @throws IOException Si ocurre un error de red o de API.
     */
    public String createEvent(String calendarId, Event event) throws IOException {
        Event createdEvent = service.events().insert(calendarId, event).execute();
        return createdEvent.getId();
    }

    /**
     * Actualiza un evento existente en Google Calendar.
     * * @param calendarId ID del calendario donde está el evento.
     * @param eventId ID único del evento a modificar.
     * @param event Objeto Event con los nuevos datos actualizados.
     * @throws IOException Si el evento no existe o hay error de red.
     */
    public void updateEvent(String calendarId, String eventId, Event event) throws IOException {
        service.events().update(calendarId, eventId, event).execute();
    }
    
}