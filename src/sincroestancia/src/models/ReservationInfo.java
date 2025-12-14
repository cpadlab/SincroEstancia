package sincroestancia.src.models;

/**
 * Record que agrupa todos los datos de una reserva para su visualización o procesamiento.
 * * @author Carlos Padilla Labella
 */
public record ReservationInfo(
    int id, 
    String guestName, 
    String guestDni, 
    String guestEmail, 
    String guestPhone,
    String checkIn, 
    String checkOut, 
    int pax, 
    boolean isPaid, 
    boolean hasCheckin, 
    boolean hasCheckout
) {}