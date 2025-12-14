package sincroestancia.src.services;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import sincroestancia.src.database.DatabaseManager;
import sincroestancia.src.models.VutItem;
import sincroestancia.src.models.FullDayInfo;
import sincroestancia.src.models.ReservationInfo;
import sincroestancia.src.models.DayInfo;
import sincroestancia.src.models.DaySyncData;
import sincroestancia.src.models.GuestData;
import sincroestancia.src.models.OperationSyncData;

/**
 * Servicio centralizado (Repository) para la gestión de toda la persistencia de datos SQLite.
 * * Actúa como una fachada que encapsula todas las consultas SQL, transacciones y lógica de mapeo
 * de datos para las entidades: VUTs, Reservas, Huéspedes, Configuración y Sincronización.
 * * @author Carlos Padilla Labella
 */
public class DatabaseService {

    Connection conn = DatabaseManager.get_connection();

    /**
     * Constructor predeterminado.
     * * Inicializa la estructura de la base de datos verificando el script SQL inicial al instanciarse.
     */
    public DatabaseService() {
        initDatabase();
    }

    /**
     * Lee y ejecuta el script SQL 'init-databases.sql' desde los recursos del JAR.
     * * Divide el archivo por sentencias (separador ';') y las ejecuta secuencialmente
     * para crear tablas si no existen.
     */
    private void initDatabase() {
        try {

            InputStream is = getClass().getResourceAsStream("/sincroestancia/assets/database/init-databases.sql");

            if (is == null) {
                System.err.println("[error] No se encontró el archivo init-databases.sql");
                return;
            }

            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {

                String sql = scanner.useDelimiter("\\A").next();
                String[] statements = sql.split(";");

                try (Statement stmt = conn.createStatement()) {
                    for (String statement: statements) {
                        if (!statement.trim().isEmpty()) {
                            stmt.execute(statement);
                        }
                    }
                }

            }

            System.out.println("[info] Base de datos inicializada correctamente.");

        } catch (Exception e) {
            System.err.println("[error] Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica si existe al menos una vivienda registrada en el sistema.
     * * @return true si hay registros en la tabla 'vuts', false si está vacía.
     */
    public boolean are_vuts_registered() {

        String sql = "SELECT COUNT(*) AS total FROM vuts";

        try (Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total") > 0;
            }

        } catch (SQLException e) {
            System.err.println("[error] Error executing 'SELECT COUNT(*)' in vuts: " + e.getMessage());
        }

        return false;
    }

    /**
     * Registra una nueva vivienda turística en la base de datos.
     * * Utiliza PreparedStatement para prevenir inyección SQL y recupera la clave generada (ID).
     * * @param name Nombre comercial de la vivienda.
     * @param cover Ruta de la imagen de portada.
     * @param url URL del anuncio o web.
     * @param apikey Clave de API asociada.
     * @return El ID de la nueva vivienda o -1 si hubo un error.
     */
    public int register_vut(String name, String cover, String url, String apikey) {

        String sql = "INSERT INTO vuts (name, cover, url, apikey) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            pstmt.setString(2, cover);
            pstmt.setString(3, url);
            pstmt.setString(4, apikey);

            int affected_rows = pstmt.executeUpdate();
            if (affected_rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int new_id = rs.getInt(1);
                        System.out.println("[info] VUT successfully registered: " + name + " (ID: " + new_id + ")");
                        return new_id;
                    }
                }
            }

            return -1;

        } catch (SQLException e) {
            System.err.println("[error] Error registering the VUT ('" + name + "'): " + e.getMessage());
            return -1;
        }

    }

    /**
     * Recupera la lista completa de viviendas registradas ordenadas por nombre.
     * * @return Lista de objetos VutItem.
     */
    public List < VutItem > get_all_vuts() {

        List < VutItem > vuts = new ArrayList < > ();
        String sql = "SELECT id, name , cover, url, apikey FROM vuts ORDER BY name ASC";

        try (Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                vuts.add(new VutItem(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("cover"),
                    rs.getString("url"),
                    rs.getString("apikey")
                ));
            }

        } catch (SQLException e) {
            System.err.println("[error] Error al cargar VUTs: " + e.getMessage());
        }

        return vuts;
    }

    /**
     * Actualiza la configuración global seleccionando la vivienda activa.
     * * @param vutId ID de la vivienda a seleccionar.
     * @return true si la actualización fue correcta.
     */
    public boolean update_selected_vut(int vutId) {

        String sql = "UPDATE config SET selected_vut = ? WHERE id = 1";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vutId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[error] Error updating the selected VUT: " + e.getMessage());
            return false;
        }

    }

    /**
     * Obtiene los detalles completos de la vivienda actualmente seleccionada en la configuración.
     * * Realiza un JOIN entre la tabla 'vuts' y la tabla 'config'.
     * * @return Objeto VutItem con los datos, o null si no hay selección.
     */
    public VutItem get_selected_vut_details() {
        
        String sql = "SELECT v.id, v.name, v.cover, v.url, v.apikey " +
            "FROM vuts v " +
            "JOIN config c ON v.id = c.selected_vut " +
            "WHERE c.id = 1";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return new VutItem(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("cover"),
                    rs.getString("url"),
                    rs.getString("apikey")
                );
            }

        } catch (SQLException e) {
            System.err.println("[error] Error obtaining details of selected VUT: " + e.getMessage());
        }

        return null;

    }

    /**
     * Busca una vivienda específica por su identificador.
     * * @param vutId ID de la vivienda.
     * @return Objeto VutItem o null si no existe.
     */
    public VutItem get_vut_details_by_id(int vutId) {

        String sql = "SELECT id, name, cover, url, apikey FROM vuts WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vutId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new VutItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("cover"),
                        rs.getString("url"),
                        rs.getString("apikey")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("[error] Error obtaining VUT details for ID " + vutId + ": " + e.getMessage());
        }

        return null;

    }

    /**
     * Actualiza los datos informativos de una vivienda existente.
     * * @param id ID de la vivienda.
     * @param name Nuevo nombre.
     * @param cover Nueva ruta de portada.
     * @param url Nueva URL.
     * @param apikey Nueva API Key.
     * @return true si se actualizó correctamente.
     */
    public boolean update_vut(int id, String name, String cover, String url, String apikey) {
        
        String sql = "UPDATE vuts SET name = ?, cover = ?, url = ?, apikey = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, name);
            pstmt.setString(2, cover);
            pstmt.setString(3, url);
            pstmt.setString(4, apikey);
            pstmt.setInt(5, id);
            
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[error] Error updating VUT: " + e.getMessage());
            return false;
        }

    }

    /**
     * Elimina una vivienda del sistema.
     * * @param id ID de la vivienda a eliminar.
     * @return true si se eliminó correctamente.
     */
    public boolean delete_vut(int id) {
        
        String sql = "DELETE FROM vuts WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[error] Error deleting VUT: " + e.getMessage());
            return false;
        }

    }

    /**
     * Obtiene la temporada asignada a una fecha específica.
     * * @param vutId ID de la vivienda.
     * @param date Fecha a consultar.
     * @return Nombre de la temporada (low, average, high) o null.
     */
    public String get_day_season(int vutId, Date date) {
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdf.format(date);
        String sql = "SELECT season FROM days WHERE vut_id = ? AND day_date = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vutId);
            pstmt.setString(2, formattedDate);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("season");
                }
            }

        } catch (SQLException e) {
            System.err.println("[error] Error getting day season (" + formattedDate + "): " + e.getMessage());
        }
        
        return null;

    }

    /**
     * Recupera el estado y temporada de todos los días de un mes y año específicos.
     * * Útil para pintar el calendario mensual en la interfaz.
     * * @param vutId ID de la vivienda.
     * @param year Año.
     * @param month Mes (0-11).
     * @return Mapa donde la Clave es el día del mes (int) y el Valor es DayInfo.
     */
    public Map < Integer, DayInfo > get_month_data(int vutId, int year, int month) {
        
        Map < Integer, DayInfo > monthData = new HashMap < > ();
        String sqlMonth = String.format("%02d", month + 1);
        String sqlYear = String.valueOf(year);
        
        String sql = "SELECT day_date, season, status FROM days " +
            "WHERE vut_id = ? AND strftime('%Y', day_date) = ? AND strftime('%m', day_date) = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vutId);
            pstmt.setString(2, sqlYear);
            pstmt.setString(3, sqlMonth);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String dayDateStr = rs.getString("day_date");
                    String season = rs.getString("season");
                    String status = rs.getString("status");
                    int day = Integer.parseInt(dayDateStr.substring(8, 10));
                    monthData.put(day, new DayInfo(season, status));
                }
            }

        } catch (SQLException | NumberFormatException e) {
            System.err.println("[error] Error getting month data: " + e.getMessage());
        }

        return monthData;
    }

    /**
     * Obtiene información detallada (incluyendo precio) de un día concreto.
     * * @param vutId ID de la vivienda.
     * @param date Fecha consultada.
     * @return Record FullDayInfo con precio, estado y temporada.
     */
    public FullDayInfo get_full_day_details(int vutId, Date date) {
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdf.format(date);
        String sql = "SELECT season, status, day_price FROM days WHERE vut_id = ? AND day_date = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vutId);
            pstmt.setString(2, formattedDate);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new FullDayInfo(rs.getString("season"), rs.getString("status"), rs.getDouble("day_price"));
                }
            }

        } catch (SQLException e) {
            System.err.println("[error] Error getting full day details: " + e.getMessage());
        }
        
        return null;

    }

    /**
     * Crea una nueva reserva y bloquea los días correspondientes en el calendario.
     * * Implementación Transaccional:
     * - Inicia transacción.
     * - Inserta el registro en la tabla 'reservations'.
     * - Actualiza por lotes (batch) la tabla 'days' marcando los días como ocupados/pagados y reseteando 'is_synced'.
     * - Si algo falla, hace rollback completo.
     * * @param vutId ID de la vivienda.
     * @param name Nombre del huésped principal.
     * @param checkIn Fecha de entrada.
     * @param checkOut Fecha de salida.
     * @param isPaid Estado del pago inicial.
     * @return true si la transacción fue exitosa.
     */
    public boolean register_reservation(int vutId, String name, String dni, String email, String phone, String checkIn, String checkOut, int pax, boolean isPaid) {

        String newStatus = isPaid ? "paid" : "reserved";
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        String sqlInsertReservation = "INSERT INTO reservations (vut_id, guest_name, guest_dni, guest_email, guest_phone, " +
            "check_in_date, check_out_date, pax_count, is_paid, created_at, has_checkout) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";

        String sqlUpdateDays = "UPDATE days SET status = ?, is_synced = 0 WHERE vut_id = ? AND day_date = ?";

        List < String > datesToUpdate;

        try {
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(checkIn, formatter);
            LocalDate end = LocalDate.parse(checkOut, formatter);
            
            datesToUpdate = Stream.iterate(start, date -> date.plusDays(1))
                .limit(ChronoUnit.DAYS.between(start, end))
                .map(formatter::format)
                .collect(Collectors.toList());

            if (datesToUpdate.isEmpty()) return false;

        } catch (Exception e) {
            System.err.println("[error] Error parsing dates: " + e.getMessage());
            return false;
        }

        try {

            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmtReservation = conn.prepareStatement(sqlInsertReservation)) {
                pstmtReservation.setInt(1, vutId);
                pstmtReservation.setString(2, name);
                pstmtReservation.setString(3, dni);
                pstmtReservation.setString(4, email);
                pstmtReservation.setString(5, phone);
                pstmtReservation.setString(6, checkIn);
                pstmtReservation.setString(7, checkOut);
                pstmtReservation.setInt(8, pax);
                pstmtReservation.setBoolean(9, isPaid);
                pstmtReservation.setString(10, createdAt);
                
                if (pstmtReservation.executeUpdate() == 0) throw new SQLException("Reservation insert failed.");

            }

            try (PreparedStatement pstmtUpdateDays = conn.prepareStatement(sqlUpdateDays)) {
                
                for (String date: datesToUpdate) {
                    pstmtUpdateDays.setString(1, newStatus);
                    pstmtUpdateDays.setInt(2, vutId);
                    pstmtUpdateDays.setString(3, date);
                    pstmtUpdateDays.addBatch();
                }

                pstmtUpdateDays.executeBatch();
            }
            
            conn.commit();
            
            return true;

        } catch (SQLException e) {
            
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            
            System.err.println("[error] Reservation transaction failed: " + e.getMessage());
            return false;

        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Busca si existe una reserva activa que cubra la fecha indicada.
     * * Se utiliza para mostrar detalles al hacer clic en un día ocupado del calendario.
     * * @param vutId ID de la vivienda.
     * @param date Fecha a comprobar.
     * @return Datos de la reserva (ReservationInfo) o null.
     */
    public ReservationInfo get_reservation_details_for_day(int vutId, Date date) {
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdf.format(date);

        String sql = "SELECT id, guest_name, guest_dni, guest_email, guest_phone, check_in_date, check_out_date, pax_count, is_paid, has_checkin, has_checkout " +
            "FROM reservations " +
            "WHERE vut_id = ? AND ? BETWEEN check_in_date AND date(check_out_date, '-1 day')";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vutId);
            pstmt.setString(2, formattedDate);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new ReservationInfo(
                        rs.getInt("id"),
                        rs.getString("guest_name"),
                        rs.getString("guest_dni"),
                        rs.getString("guest_email"),
                        rs.getString("guest_phone"),
                        rs.getString("check_in_date"),
                        rs.getString("check_out_date"),
                        rs.getInt("pax_count"),
                        rs.getBoolean("is_paid"),
                        rs.getBoolean("has_checkin"),
                        rs.getBoolean("has_checkout")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("[error] Error getting reservation details: " + e.getMessage());
        }

        return null;
    }

    /**
     * Actualiza el estado de pago de una reserva y sus días asociados.
     * * Transaccional: Actualiza 'reservations' y actualiza el estado (reserved -> paid) en 'days'.
     * * @param reservationId ID de la reserva.
     * @param isPaid Nuevo estado de pago.
     * @return true si se completó correctamente.
     */
    public boolean update_reservation_payment_status(int reservationId, boolean isPaid, int vutId, String checkIn, String checkOut) {
        
        String newDayStatus = isPaid ? "paid" : "reserved";
        String sqlUpdateReservation = "UPDATE reservations SET is_paid = ? WHERE id = ?";
        String sqlUpdateDays = "UPDATE days SET status = ? WHERE vut_id = ? AND day_date BETWEEN ? AND date(?, '-1 day')";

        try {
            
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmtRes = conn.prepareStatement(sqlUpdateReservation)) {
                pstmtRes.setBoolean(1, isPaid);
                pstmtRes.setInt(2, reservationId);
                pstmtRes.executeUpdate();
            }
            
            try (PreparedStatement pstmtDays = conn.prepareStatement(sqlUpdateDays)) {
                pstmtDays.setString(1, newDayStatus);
                pstmtDays.setInt(2, vutId);
                pstmtDays.setString(3, checkIn);
                pstmtDays.setString(4, checkOut);
                pstmtDays.executeUpdate();
            }
            
            conn.commit();
            return true;

        } catch (SQLException e) {
            
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            
            System.err.println("[error] Payment status update failed: " + e.getMessage());
            
            return false;

        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Modifica los datos principales de una reserva existente.
     * * Actualiza tanto la información del huésped como el estado de los días afectados.
     * * @return true si la actualización fue exitosa.
     */
    public boolean update_reservation_details(int reservationId, int vutId, String name, String dni, String email, String phone, int pax, boolean isPaid, String checkIn, String checkOut) {

        String newDayStatus = isPaid ? "paid" : "reserved";
        String sqlUpdateReservation = "UPDATE reservations SET guest_name = ?, guest_dni = ?, guest_email = ?, guest_phone = ?, pax_count = ?, is_paid = ? WHERE id = ?";
        String sqlUpdateDays = "UPDATE days SET status = ? WHERE vut_id = ? AND day_date BETWEEN ? AND date(?, '-1 day')";

        try {

            conn.setAutoCommit(false);

            try (PreparedStatement pstmtRes = conn.prepareStatement(sqlUpdateReservation)) {
                pstmtRes.setString(1, name);
                pstmtRes.setString(2, dni);
                pstmtRes.setString(3, email);
                pstmtRes.setString(4, phone);
                pstmtRes.setInt(5, pax);
                pstmtRes.setBoolean(6, isPaid);
                pstmtRes.setInt(7, reservationId);
                pstmtRes.executeUpdate();
            }

            try (PreparedStatement pstmtDays = conn.prepareStatement(sqlUpdateDays)) {
                pstmtDays.setString(1, newDayStatus);
                pstmtDays.setInt(2, vutId);
                pstmtDays.setString(3, checkIn);
                pstmtDays.setString(4, checkOut);
                pstmtDays.executeUpdate();
            }

            conn.commit();

            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            System.err.println("[error] Update reservation failed: " + e.getMessage());
            return false;

        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Elimina una reserva y libera los días en el calendario.
     * * Transaccional:
     * - Borra la reserva.
     * - Pone en 'free' los días del rango en la tabla 'days'.
     * * @return true si se eliminó correctamente.
     */
    public boolean delete_reservation(int reservationId, int vutId, String checkIn, String checkOut) {
        String sqlDeleteRes = "DELETE FROM reservations WHERE id = ?";
        String sqlFreeDays = "UPDATE days SET status = 'free' WHERE vut_id = ? AND day_date BETWEEN ? AND date(?, '-1 day')";

        try {
            
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmtDays = conn.prepareStatement(sqlFreeDays)) {
                pstmtDays.setInt(1, vutId);
                pstmtDays.setString(2, checkIn);
                pstmtDays.setString(3, checkOut);
                pstmtDays.executeUpdate();
            }
            
            try (PreparedStatement pstmtRes = conn.prepareStatement(sqlDeleteRes)) {
                pstmtRes.setInt(1, reservationId);
                if (pstmtRes.executeUpdate() == 0) throw new SQLException("Reservation not found.");
            }
            
            conn.commit();
            return true;

        } catch (SQLException e) {
            
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            
            System.err.println("[error] Delete reservation failed: " + e.getMessage());
            return false;

        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Busca una reserva cuyo día de salida coincida con la fecha dada.
     * * Útil para identificar operaciones de Check-out pendientes.
     */
    public ReservationInfo get_reservation_by_checkout_date(int vutId, Date date) {
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = sdf.format(date);

        String sql = "SELECT id, guest_name, guest_dni, guest_email, guest_phone, check_in_date, check_out_date, pax_count, is_paid, has_checkin, has_checkout " +
            "FROM reservations " +
            "WHERE vut_id = ? AND check_out_date = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vutId);
            pstmt.setString(2, formattedDate);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new ReservationInfo(
                        rs.getInt("id"),
                        rs.getString("guest_name"),
                        rs.getString("guest_dni"),
                        rs.getString("guest_email"),
                        rs.getString("guest_phone"),
                        rs.getString("check_in_date"),
                        rs.getString("check_out_date"),
                        rs.getInt("pax_count"),
                        rs.getBoolean("is_paid"),
                        rs.getBoolean("has_checkin"),
                        rs.getBoolean("has_checkout")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("[error] Error getting checkout reservation: " + e.getMessage());
        }
        
        return null;

    }

    /**
     * Obtiene una reserva por su ID primario.
     */
    public ReservationInfo get_reservation_by_id(int reservationId) {
        
        String sql = "SELECT id, guest_name, guest_dni, guest_email, guest_phone, check_in_date, check_out_date, pax_count, is_paid, has_checkin, has_checkout " +
            "FROM reservations WHERE id = ?";
        
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, reservationId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new ReservationInfo(
                    rs.getInt("id"),
                    rs.getString("guest_name"),
                    rs.getString("guest_dni"),
                    rs.getString("guest_email"),
                    rs.getString("guest_phone"),
                    rs.getString("check_in_date"),
                    rs.getString("check_out_date"),
                    rs.getInt("pax_count"),
                    rs.getBoolean("is_paid"),
                    rs.getBoolean("has_checkin"),
                    rs.getBoolean("has_checkout")
                );
            }

        } catch (SQLException e) {
            System.err.println("[error] Error getting reservation by ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Obtiene el ID de un check-in existente o crea uno nuevo si no existe para la reserva.
     * * @param reservationId ID de la reserva asociada.
     * @return ID del registro en la tabla 'checkins'.
     */
    public int get_or_create_checkin_id(int reservationId) {
        
        String sqlSelect = "SELECT id FROM checkins WHERE reservation_id = ?";
        String sqlInsert = "INSERT INTO checkins (reservation_id, signed_at) VALUES (?, datetime('now'))";

        try {
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {
                pstmt.setInt(1, reservationId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) return rs.getInt("id");
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, reservationId);
                pstmt.executeUpdate();
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        
        } catch (SQLException e) {
            System.err.println("[error] Error getting/creating checkin ID: " + e.getMessage());
        }
        
        return -1;
    }

    /**
     * Finaliza el proceso de Check-in guardando datos financieros y legales.
     * * Transaccional: Actualiza la tabla 'checkins' y marca 'has_checkin=1' en la reserva.
     */
    public boolean finalize_checkin(int checkinId, String paymentMethod, String paymentIdentifier, String paymentHolderName,
        
        String cardExpiry, String paymentDate, boolean rulesAccepted, boolean gdprAccepted) {

        String sqlUpdateCheckin = "UPDATE checkins SET payment_method = ?, payment_identifier = ?, payment_holder = ?, card_expiry_date = ?, payment_date = ?, rules_accepted = ?, gdpr_accepted = ? WHERE id = ?";
        String sqlUpdateReservation = "UPDATE reservations SET has_checkin = 1 WHERE id = (SELECT reservation_id FROM checkins WHERE id = ?)";

        try {
            
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateCheckin)) {
                pstmt.setString(1, paymentMethod);
                pstmt.setString(2, paymentIdentifier);
                pstmt.setString(3, paymentHolderName);
                pstmt.setString(4, cardExpiry);
                pstmt.setString(5, paymentDate);
                pstmt.setBoolean(6, rulesAccepted);
                pstmt.setBoolean(7, gdprAccepted);
                pstmt.setInt(8, checkinId);
                pstmt.executeUpdate();
            }
            
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateReservation)) {
                pstmt.setInt(1, checkinId);
                pstmt.executeUpdate();
            }
            
            conn.commit();
            
            return true;

        } catch (SQLException e) {
            
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            
            System.err.println("[error] Error finalizing checkin: " + e.getMessage());
            return false;

        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Registra el Check-out (salida) de los huéspedes.
     * * Guarda hora real de salida, estado de llaves y reporte de daños.
     */
    public boolean register_checkout(int reservationId, String exitTime, boolean keysReturned, boolean damageDetected, String damageDesc) {

        String sqlInsert = "INSERT INTO checkouts (reservation_id, actual_exit_time, keys_returned, damage_detected, damage_description, created_at) VALUES (?, ?, ?, ?, ?, datetime('now'))";
        String sqlUpdate = "UPDATE reservations SET has_checkout = 1 WHERE id = ?";

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setInt(1, reservationId);
                pstmt.setString(2, exitTime);
                pstmt.setBoolean(3, keysReturned);
                pstmt.setBoolean(4, damageDetected);
                pstmt.setString(5, damageDesc);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                pstmt.setInt(1, reservationId);
                pstmt.executeUpdate();
            }

            conn.commit();
            System.out.println("[info] Checkout registrado para reserva ID: " + reservationId);

            return true;

        } catch (SQLException e) {

            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("[error] Error registrando checkout: " + e.getMessage());
            return false;

        } finally {

            try {
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        }
    }

    /**
     * Añade un huésped individual al parte de entrada (Check-in).
     * * @param isMinor Indica si es menor de edad (requiere guardian_id).
     * @return true si se insertó correctamente.
     */
    public boolean add_guest(int checkinId, String fullname, String surname1, String surname2, String sex, String birthDate,
        
        String nationality, String docType, String docNumber, String supportNumber, String address, String city,
        String country, String phone, String email, boolean isMinor, Integer guardianId) {

        String sql = "INSERT INTO guests (checkin_id, fullname, surname1, surname2, sex, birth_date, nationality, " +
            "id_document_type, id_document_number, id_support_number, address_full, address_municipality, " +
            "address_country, phone, email, is_minor, guardian_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, checkinId);
            pstmt.setString(2, fullname);
            pstmt.setString(3, surname1);
            pstmt.setString(4, surname2);
            pstmt.setString(5, sex);
            pstmt.setString(6, birthDate);
            pstmt.setString(7, nationality);
            pstmt.setString(8, docType);
            pstmt.setString(9, docNumber);
            pstmt.setString(10, supportNumber);
            pstmt.setString(11, address);
            pstmt.setString(12, city);
            pstmt.setString(13, country);
            pstmt.setString(14, phone);
            pstmt.setString(15, email);
            pstmt.setBoolean(16, isMinor);
            
            if (guardianId != null && guardianId > 0) pstmt.setInt(17, guardianId);
            else pstmt.setNull(17, java.sql.Types.INTEGER);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[error] Error adding guest: " + e.getMessage());
            return false;
        }

    }

    /**
     * Obtiene la lista simplificada de todos los huéspedes de un check-in.
     */
    public List < GuestData > get_guests_by_checkin(int checkinId) {
        
        List < GuestData > list = new ArrayList < > ();
        String sql = "SELECT id, fullname, surname1, id_document_number, is_minor FROM guests WHERE checkin_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, checkinId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                list.add(new GuestData(
                    rs.getInt("id"),
                    rs.getString("fullname") + " " + rs.getString("surname1"),
                    rs.getString("id_document_number"),
                    rs.getBoolean("is_minor")
                ));
            }

        } catch (SQLException e) {
            System.err.println("[error] Error loading guests: " + e.getMessage());
        }
        
        return list;

    }

    /**
     * Obtiene solo la lista de huéspedes adultos (para asignar tutores a menores).
     */
    public List < GuestData > get_adults_by_checkin(int checkinId) {
        
        List < GuestData > list = new ArrayList < > ();
        String sql = "SELECT id, fullname, surname1, id_document_number, is_minor FROM guests WHERE checkin_id = ? AND is_minor = 0";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, checkinId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                list.add(new GuestData(
                    rs.getInt("id"),
                    rs.getString("fullname") + " " + rs.getString("surname1"),
                    rs.getString("id_document_number"),
                    rs.getBoolean("is_minor")
                ));
            }
        
        } catch (SQLException e) {
            System.err.println("[error] Error loading adults: " + e.getMessage());
        }
        
        return list;
    }

    /**
     * Obtiene todos los detalles de un huésped específico por su ID.
     * * @return Mapa con los campos del huésped para rellenar formularios.
     */
    public Map < String, Object > get_guest_details(int guestId) {
        
        String sql = "SELECT * FROM guests WHERE id = ?";
        Map < String, Object > data = new HashMap < > ();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, guestId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                
                data.put("id", rs.getInt("id"));
                data.put("fullname", rs.getString("fullname"));
                data.put("surname1", rs.getString("surname1"));
                data.put("surname2", rs.getString("surname2"));
                data.put("sex", rs.getString("sex"));
                data.put("birthDate", rs.getString("birth_date"));
                data.put("nationality", rs.getString("nationality"));
                data.put("docType", rs.getString("id_document_type"));
                data.put("docNumber", rs.getString("id_document_number"));
                data.put("supportNumber", rs.getString("id_support_number"));
                data.put("address", rs.getString("address_full"));
                data.put("city", rs.getString("address_municipality"));
                data.put("country", rs.getString("address_country"));
                data.put("phone", rs.getString("phone"));
                data.put("email", rs.getString("email"));
                data.put("isMinor", rs.getBoolean("is_minor"));
                
                return data;

            }

        } catch (SQLException e) {
            System.err.println("[error] Error getting guest details: " + e.getMessage());
        }
        return null;
    }

    /**
     * Actualiza los datos de un huésped existente.
     */
    public boolean update_guest(int guestId, String fullname, String surname1, String surname2, String sex, String birthDate,
        
        String nationality, String docType, String docNumber, String supportNumber, String address, String city,
        String country, String phone, String email, boolean isMinor, Integer guardianId) {
        String sql = "UPDATE guests SET fullname=?, surname1=?, surname2=?, sex=?, birth_date=?, nationality=?, id_document_type=?, id_document_number=?, id_support_number=?, address_full=?, address_municipality=?, address_country=?, phone=?, email=?, is_minor=?, guardian_id=? WHERE id=?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, fullname);
            pstmt.setString(2, surname1);
            pstmt.setString(3, surname2);
            pstmt.setString(4, sex);
            pstmt.setString(5, birthDate);
            pstmt.setString(6, nationality);
            pstmt.setString(7, docType);
            pstmt.setString(8, docNumber);
            pstmt.setString(9, supportNumber);
            pstmt.setString(10, address);
            pstmt.setString(11, city);
            pstmt.setString(12, country);
            pstmt.setString(13, phone);
            pstmt.setString(14, email);
            pstmt.setBoolean(15, isMinor);
            
            if (guardianId != null && guardianId > 0) pstmt.setInt(16, guardianId);
            else pstmt.setNull(16, java.sql.Types.INTEGER);
            
            pstmt.setInt(17, guestId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[error] Error updating guest: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un huésped de la base de datos.
     */
    public boolean delete_guest(int guestId) {
        
        String sql = "DELETE FROM guests WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, guestId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[error] Error deleting guest: " + e.getMessage());
            return false;
        }
    }

    /**
     * Establece precios y temporadas para un rango de fechas (Bulk Update).
     * * Utiliza 'UPSERT' (INSERT OR UPDATE) para crear los días si no existen o actualizarlos si ya están.
     * * Fuerza 'is_synced = 0' para que el sincronizador de Google actualice estos cambios.
     * * No sobrescribe días que ya estén reservados o pagados.
     */
    public boolean update_price_range(int vutId, LocalDate startDate, LocalDate endDate, double price, String season) {
        
        String sql = "INSERT INTO days (vut_id, day_date, day_price, status, season, is_synced) " +
            "VALUES (?, ?, ?, 'free', ?, 0) " +
            "ON CONFLICT(vut_id, day_date) DO UPDATE SET " +
            "day_price = excluded.day_price, " +
            "season = excluded.season, " +
            "is_synced = 0 " +
            "WHERE status != 'reserved' AND status != 'paid'";

        try {
            
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
                long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            
                for (int i = 0; i <= daysBetween; i++) {
            
                    String dateStr = startDate.plusDays(i).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
                    pstmt.setInt(1, vutId);
                    pstmt.setString(2, dateStr);
                    pstmt.setDouble(3, price);
                    pstmt.setString(4, season);
                    pstmt.addBatch();

                }
                
                pstmt.executeBatch();
                conn.commit();

                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("[error] Batch update failed: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("[error] Transaction error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcula los ingresos mensuales (Confirmados).
     */
    public double getMonthlyRevenue(int vutId, int month, int year) {
        
        String dateFilter = String.format("%d-%02d%%", year, month);
        String sql = "SELECT SUM(day_price) FROM days WHERE vut_id = ? AND day_date LIKE ? AND status = 'paid'";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vutId);
            pstmt.setString(2, dateFilter);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) return rs.getDouble(1);

        } catch (SQLException e) {
            System.err.println("[error] Error calculating revenue: " + e.getMessage());
        }

        return 0.0;

    }

    /**
     * Calcula el porcentaje de ocupación mensual.
     */
    public double getMonthlyOccupancy(int vutId, int month, int year) {
        
        String dateFilter = String.format("%d-%02d%%", year, month);
        String sql = "SELECT COUNT(*) FROM days WHERE vut_id = ? AND day_date LIKE ? AND (status = 'reserved' OR status = 'paid')";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vutId);
            pstmt.setString(2, dateFilter);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int occupied = rs.getInt(1);
                int totalDays = java.time.YearMonth.of(year, month).lengthOfMonth();
                return totalDays > 0 ? ((double) occupied / totalDays) * 100.0 : 0.0;
            }

        } catch (SQLException e) {
            System.err.println("[error] Error calculating occupancy: " + e.getMessage());
        }

        return 0.0;
    }

    /**
     * Obtiene los próximos movimientos (Entradas y Salidas) ordenados por fecha.
     * * Realiza una UNION de reservas por fecha de entrada y fecha de salida.
     */
    public ArrayList < Map < String, String >> getUpcomingMovements(int vutId) {
        
        ArrayList < Map < String, String >> movements = new ArrayList < > ();
        
        String today = LocalDate.now().toString();
        String sql = "SELECT id, guest_name, check_in_date as date, 'CHECK-IN' as type FROM reservations WHERE vut_id = ? AND check_in_date >= ? " +
            "UNION ALL " +
            "SELECT id, guest_name, check_out_date as date, 'CHECK-OUT' as type FROM reservations WHERE vut_id = ? AND check_out_date >= ? " +
            "ORDER BY date ASC LIMIT 10";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vutId);
            pstmt.setString(2, today);
            pstmt.setInt(3, vutId);
            pstmt.setString(4, today);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                
                Map < String, String > item = new HashMap < > ();
                
                item.put("id", String.valueOf(rs.getInt("id")));
                item.put("guest", rs.getString("guest_name"));
                item.put("date", rs.getString("date"));
                item.put("type", rs.getString("type"));
                
                movements.add(item);

            }

        } catch (SQLException e) {
            System.err.println("[error] Error fetching movements: " + e.getMessage());
        }

        return movements;
    }

    /**
     * Obtiene datos agregados de ingresos por mes para un año completo.
     * * @return Mapa (Mes -> Total Ingresos).
     */
    public Map < Integer, Double > getYearlyRevenueData(int vutId, int year) {
        
        Map < Integer, Double > data = new HashMap < > ();
        for (int i = 1; i <= 12; i++) data.put(i, 0.0);
        
        String sql = "SELECT strftime('%m', day_date) as month, SUM(day_price) as total FROM days " +
            "WHERE vut_id = ? AND strftime('%Y', day_date) = ? AND status = 'paid' GROUP BY month";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, vutId);
            pstmt.setString(2, String.valueOf(year));
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                data.put(rs.getInt("month"), rs.getDouble("total"));
            }

        } catch (SQLException e) {
            System.err.println("[error] Error fetching yearly revenue: " + e.getMessage());
        }

        return data;

    }

    /**
     * Genera estadísticas de nacionalidad de los huéspedes históricos.
     * * Realiza JOINS entre guests, checkins y reservations.
     */
    public Map < String, Integer > getNationalityStats(int vutId) {

        Map < String, Integer > data = new HashMap < > ();

        String sql = "SELECT g.nationality, COUNT(*) as count FROM guests g " +
            "JOIN checkins c ON g.checkin_id = c.id " +
            "JOIN reservations r ON c.reservation_id = r.id " +
            "WHERE r.vut_id = ? GROUP BY g.nationality ORDER BY count DESC LIMIT 10";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vutId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                data.put(rs.getString("nationality"), rs.getInt("count"));
            }

        } catch (SQLException e) {
            System.err.println("[error] Error fetching nationality stats: " + e.getMessage());
        }

        return data;

    }

    /**
     * Obtiene datos agregados de ocupación por mes para un año completo.
     */
    public Map < Integer, Double > getYearlyOccupancyStats(int vutId, int year) {

        Map < Integer, Double > data = new HashMap < > ();
        for (int i = 1; i <= 12; i++) data.put(i, 0.0);

        String sql = "SELECT strftime('%m', day_date) as month, COUNT(*) as occupied FROM days " +
            "WHERE vut_id = ? AND strftime('%Y', day_date) = ? AND (status = 'reserved' OR status = 'paid') GROUP BY month";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vutId);
            pstmt.setString(2, String.valueOf(year));

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int occupied = rs.getInt("occupied");
                int totalDays = java.time.YearMonth.of(year, rs.getInt("month")).lengthOfMonth();
                data.put(rs.getInt("month"), ((double) occupied / totalDays) * 100.0);
            }

        } catch (SQLException e) {
            System.err.println("[error] Error fetching occupancy stats: " + e.getMessage());
        }

        return data;
    }

    /**
     * Actualiza las credenciales y configuración de Google Calendar.
     */
    public boolean update_google_config(int configId, String calendarId, String credentialsPath) {

        String sql = "UPDATE config SET google_calendar_id = ?, google_credentials_path = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, calendarId);
            pstmt.setString(2, credentialsPath);
            pstmt.setInt(3, configId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[error] Error updating Google Config: " + e.getMessage());
            return false;
        }

    }

    /**
     * Recupera la configuración actual de Google.
     */
    public Map < String, String > get_google_config() {

        Map < String, String > config = new HashMap < > ();
        String sql = "SELECT google_calendar_id, google_credentials_path FROM config WHERE id = 1";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                config.put("calendarId", rs.getString("google_calendar_id"));
                config.put("credentialsPath", rs.getString("google_credentials_path"));
            }
        } catch (SQLException e) {
            System.err.println("[error] Error fetching Google Config: " + e.getMessage());
        }

        return config;

    }

    /**
     * Obtiene la lista de días futuros que han cambiado localmente y necesitan enviarse a Google.
     * * Realiza un LEFT JOIN con reservas para enriquecer el evento con el nombre del huésped.
     * * @return Lista de DaySyncData listos para procesar.
     */
    public List < DaySyncData > getUnsyncedFutureDays() {

        List < DaySyncData > list = new ArrayList < > ();

        String sql = """
        SELECT d.vut_id, d.day_date, d.day_price, d.status, d.season, d.google_event_id, r.guest_name
        FROM days d
        LEFT JOIN reservations r ON d.vut_id = r.vut_id
        AND d.day_date >= r.check_in_date
        AND d.day_date < r.check_out_date
        WHERE d.is_synced = 0 AND d.day_date >= date('now')
        """;

        try (Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new DaySyncData(
                    rs.getInt("vut_id"),
                    rs.getString("day_date"),
                    rs.getDouble("day_price"),
                    rs.getString("status"),
                    rs.getString("season"),
                    rs.getString("google_event_id"),
                    rs.getString("guest_name")
                ));
            }

        } catch (SQLException e) {
            System.err.println("[error] Error fetching unsynced days: " + e.getMessage());
        }

        return list;
    }

    /**
     * Confirma que un día ha sido sincronizado guardando el ID del evento remoto.
     */
    public void markDayAsSynced(String date, String googleEventId) {

        String sql = "UPDATE days SET is_synced = 1, google_event_id = ? WHERE day_date = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, googleEventId);
            pstmt.setString(2, date);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[error] Error marking day as synced: " + e.getMessage());
        }
    }

    /**
     * Obtiene las operaciones de Check-in/Out pendientes de sincronizar.
     */
    public List < OperationSyncData > getOperationsToSync() {

        List < OperationSyncData > list = new ArrayList < > ();

        String sql = """
        SELECT id, guest_name, check_in_date, check_out_date,
        has_checkin, has_checkout,
        google_event_in_id, google_event_out_id
        FROM reservations
        WHERE check_out_date >= date('now', '-1 day')
        """;

        try (Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new OperationSyncData(
                    rs.getInt("id"),
                    rs.getString("guest_name"),
                    rs.getString("check_in_date"),
                    rs.getString("check_out_date"),
                    rs.getBoolean("has_checkin"),
                    rs.getBoolean("has_checkout"),
                    rs.getString("google_event_in_id"),
                    rs.getString("google_event_out_id")
                ));
            }

        } catch (SQLException e) {
            System.err.println("[error] Error fetching operations: " + e.getMessage());
        }
        return list;
    }

    /**
     * Guarda los IDs de los eventos de Google Calendar (Check-in y Check-out) en la reserva local.
     */
    public void updateReservationEventIds(int reservationId, String eventInId, String eventOutId) {

        String sql = "UPDATE reservations SET google_event_in_id = ?, google_event_out_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, eventInId);
            pstmt.setString(2, eventOutId);
            pstmt.setInt(3, reservationId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[error] Error saving operation IDs: " + e.getMessage());
        }
    }

    /**
     * Calcula los ingresos anuales totales para reportes.
     */
    public double getTotalYearlyRevenue(int vutId, int year) {

        String yearStr = String.valueOf(year);
        String sql = "SELECT SUM(day_price) FROM days WHERE vut_id = ? AND strftime('%Y', day_date) = ? AND status = 'paid'";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vutId);
            pstmt.setString(2, yearStr);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);

        } catch (SQLException e) {
            System.err.println("[error] Error calculating yearly revenue: " + e.getMessage());
        }

        return 0.0;

    }

    /**
     * Calcula el porcentaje de ocupación anual global.
     */
    public double getYearlyOccupancyPercentage(int vutId, int year) {

        String yearStr = String.valueOf(year);
        String sql = "SELECT COUNT(*) FROM days WHERE vut_id = ? AND strftime('%Y', day_date) = ? AND (status = 'reserved' OR status = 'paid')";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, vutId);
            pstmt.setString(2, yearStr);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int occupied = rs.getInt(1);
                int totalDays = java.time.Year.of(year).length();
                return totalDays > 0 ? ((double) occupied / totalDays) * 100.0 : 0.0;
            }

        } catch (SQLException e) {
            System.err.println("[error] Error calculating yearly occupancy: " + e.getMessage());
        }

        return 0.0;

    }

}