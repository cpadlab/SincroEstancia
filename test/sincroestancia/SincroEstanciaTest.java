
package sincroestancia;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.*;

import java.util.Map;
import java.sql.Date;

import sincroestancia.src.models.*; 
import sincroestancia.src.services.DatabaseService;

/**
 *
 * @author Carlos Padilla
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SincroEstanciaTest {
    
    private static DatabaseService dbService;
    private static int createdVutId;
    
    @BeforeClass
    public static void setup() {
        dbService = new DatabaseService();
        System.out.println("Iniciando batería de pruebas...");
    }
    
    @Test
    public void test01_VutItemModel() {
        System.out.println("[U01] Verificando Modelo VutItem...");
        VutItem vut = new VutItem(999, "Casa Test", "/img.jpg", "www.url.com", "api123");
        
        assertEquals(999, vut.getId());
        assertEquals("Casa Test", vut.getName());
        assertEquals("api123", vut.getApiKey());
    }
    
    @Test
    public void test02_DayInfoRecord() {
        System.out.println("[U02] Verificando Record DayInfo...");
        DayInfo day = new DayInfo("high", "reserved");
        
        assertEquals("high", day.season());
        assertEquals("reserved", day.status());
    }

    @Test
    public void test03_AreVutsRegistered() {
        System.out.println("[U03] Verificando conexión DB...");
        try {
            dbService.are_vuts_registered();
        } catch (Exception e) {
            fail("Error crítico de DB: " + e.getMessage());
        }
    }

    @Test
    public void test04_RegisterVut() {
        System.out.println("[U04] Registrando VUT de prueba...");
        String name = "Test House JUnit";
        createdVutId = dbService.register_vut(name, "path/test.png", "http://test.com", "api_key");
        
        assertTrue("El ID generado debe ser mayor a 0", createdVutId > 0);
    }

    @Test
    public void test05_GetVutDetails() {
        System.out.println("[U05] Leyendo VUT ID: " + createdVutId);
        VutItem vut = dbService.get_vut_details_by_id(createdVutId);
        
        assertNotNull("No se recuperó el VUT", vut);
        assertEquals("Test House JUnit", vut.getName());
    }

    @Test
    public void test06_UpdateVut() {
        System.out.println("[U06] Actualizando VUT...");
        boolean success = dbService.update_vut(createdVutId, "Test House UPDATED", "path/new.png", "http://new.com", "new_key");
        
        assertTrue("El update falló", success);
        
        VutItem check = dbService.get_vut_details_by_id(createdVutId);
        assertEquals("Test House UPDATED", check.getName());
    }

    @Test
    public void test07_GetMonthData() {
        System.out.println("[U07] Obteniendo calendario...");
        Map<Integer, DayInfo> data = dbService.get_month_data(createdVutId, 2025, 12); 
        assertNotNull(data);
    }

    @Test
    public void test08_GoogleConfig() {
        System.out.println("[U08] Actualizando config Google...");
        dbService.update_google_config(1, "cal_test", "cred.json");
    }

    @Test
    public void test09_IntegrationReservation() {
        System.out.println("[INT] Probando flujo de Reserva...");
        
        String checkIn = "2025-08-01";
        String checkOut = "2025-08-03";

        java.time.LocalDate inicio = java.time.LocalDate.parse(checkIn);
        java.time.LocalDate fin = java.time.LocalDate.parse(checkOut);
        
        dbService.update_price_range(createdVutId, inicio, fin, 100.0, "high");
        
        boolean resOk = dbService.register_reservation(
            createdVutId, "User Test", "11111111H", "email@test.com", "600123123", 
            checkIn, checkOut, 2, true
        );
        assertTrue("Fallo al registrar reserva", resOk);
        
        java.sql.Date fecha = java.sql.Date.valueOf("2025-08-01");
        FullDayInfo infoDia = dbService.get_full_day_details(createdVutId, fecha);
        
        assertNotNull("El día 1 de agosto debería existir en DB tras inicializarlo", infoDia);
        assertEquals("El estado debería haber cambiado a 'paid'", "paid", infoDia.status());
    }

    @Test
    public void test10_DeleteVut() {
        System.out.println("[U10] Borrando VUT de prueba...");
        boolean delOk = dbService.delete_vut(createdVutId);
        assertTrue(delOk);
        
        VutItem check = dbService.get_vut_details_by_id(createdVutId);
        assertNull("El VUT debería haber desaparecido", check);
    }
    
    @Test
    public void test11_IntegrationReservationFlow() {
        
        System.out.println("[INT] Probando flujo de Reserva Completo (Independiente)...");
        
        String vutName = "Integration Vut";
        int localVutId = dbService.register_vut(vutName, "path", "url", "key");
        
        assertTrue("Fallo al crear VUT para integración", localVutId > 0);

        String checkIn = "2025-06-01";
        String checkOut = "2025-06-03";
        
        java.time.LocalDate inicio = java.time.LocalDate.parse(checkIn);
        java.time.LocalDate fin = java.time.LocalDate.parse(checkOut);
        
        dbService.update_price_range(localVutId, inicio, fin, 100.0, "high");

        boolean resSuccess = dbService.register_reservation(
            localVutId, 
            "Juan Integracion", 
            "12345678Z", 
            "juan@test.com", 
            "600000000", 
            checkIn, 
            checkOut, 
            2, 
            true
        );
        
        assertTrue("La reserva debería haberse registrado correctamente", resSuccess);

        java.sql.Date dateCheck = java.sql.Date.valueOf("2025-06-01");
        FullDayInfo dayInfo = dbService.get_full_day_details(localVutId, dateCheck);
        
        assertNotNull("La información del día 2025-06-01 debería existir", dayInfo);
        assertEquals("El estado del día debería ser 'paid'", "paid", dayInfo.status());

        dbService.delete_vut(localVutId);
    }
    
}
