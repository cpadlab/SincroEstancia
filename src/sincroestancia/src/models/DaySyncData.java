package sincroestancia.src.models;

/**
 * Record para la sincronización de eventos de calendario (Días) con Google.
 * * @author Carlos Padilla Labella
 */
public record DaySyncData(
    int vutId, 
    String date, 
    double price, 
    String status, 
    String season,
    String googleEventId, 
    String guestName
) {}