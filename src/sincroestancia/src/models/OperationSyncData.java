package sincroestancia.src.models;

/**
 * Record para la sincronización de operaciones logísticas (Check-in/Out) con Google.
 * * @author Carlos Padilla Labella
 */
public record OperationSyncData(
    int id,
    String guestName,
    String checkInDate,
    String checkOutDate,
    boolean hasCheckIn,
    boolean hasCheckOut,
    String eventInId,
    String eventOutId
) {}