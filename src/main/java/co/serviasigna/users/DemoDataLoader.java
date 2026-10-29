package co.serviasigna.users;

import co.serviasigna.catalog.CatalogService;
import co.serviasigna.domain.*;
import co.serviasigna.persistence.MessageTemplateRepository;
import co.serviasigna.persistence.RateRepository;
import co.serviasigna.service.DispatchService;
import co.serviasigna.service.Settings;
import co.serviasigna.service.ScheduledDeliveryPlanner;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar (después de los scripts de base de datos):
 *   1. lee los parámetros (y crea los que falten);
 *   2. crea el administrador y el despachador iniciales si no existen, la tarifa base, los mensajes predefinidos
 *      y la política de datos v1;
 *   3. opcional: cuentas de prueba (serviasigna.demo-data=true);
 *   4. reconstruye la operación en curso desde Oracle y enciende el planificador.
 */
@Component
public class DemoDataLoader implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UserAccountService users;
    private final DispatchService dispatch;
    private final CatalogService catalog;
    private final Settings settings;
    private final ScheduledDeliveryPlanner planner;
    private final RateRepository rates;
    private final MessageTemplateRepository templates;
    private final String adminPassword, dispatcherPassword;
    private final boolean demoData;

    public DemoDataLoader(UserAccountService users, DispatchService dispatch, CatalogService catalog, Settings settings,
                       ScheduledDeliveryPlanner planner, RateRepository rates, MessageTemplateRepository templates,
                       @Value("${serviasigna.initial-admin.password:admin123}") String adminPassword,
                       @Value("${serviasigna.initial-dispatcher.password:central123}") String dispatcherPassword,
                       @Value("${serviasigna.demo-data:false}") boolean demoData) {
        this.users = users; this.dispatch = dispatch; this.catalog = catalog; this.settings = settings;
        this.planner = planner; this.rates = rates; this.templates = templates;
        this.adminPassword = adminPassword; this.dispatcherPassword = dispatcherPassword; this.demoData = demoData;
    }

    @Override
    public void run(String... args) {
        settings.load();
        if (!users.hasRole(Role.ADMIN)) {
            users.create("Administrador", "admin", adminPassword, Role.ADMIN, AccountStatus.ACTIVE);
            log.info("Cuenta inicial creada: usuario 'admin' (clave en application.properties: cámbiala)");
        }
        if (!users.hasRole(Role.DISPATCHER)) users.create("Central", "central", dispatcherPassword, Role.DISPATCHER, AccountStatus.ACTIVE);
        if (rates.findFirstByActiveTrueOrderByIdDesc().isEmpty())
            rates.save(new Rate("Tarifa base", 4000, 900, 1500, 4000, 5000, 70));
        if (templates.count() == 0) {
            for (String t : new String[]{"¿Dónde vas?", "Recoge primero el pedido más urgente", "El cliente pregunta por su pedido", "Comunícate con la central"})
                templates.save(new MessageTemplate(t, "CENTRAL"));
            for (String t : new String[]{"Voy en camino", "Hay mucho tráfico, voy un poco tarde", "El pedido no está listo", "El cliente no contesta", "Llegué al punto"})
                templates.save(new MessageTemplate(t, "DOMICILIARIO"));
        }
        if (users.currentPolicy().isEmpty()) users.publishPolicy("1.0", POLICY_V1, "sistema");
        users.loadCouriers();
        if (demoData) createDemo();
        dispatch.rateChanged();
        dispatch.load();
        planner.start();
    }

    /** Cuentas de prueba: juan, maria, pedro y ana (activos) y luis (pendiente). Clave: domi1234. */
    private void createDemo() {
        String[][] demo = { {"Juan Pérez", "juan", "1065111111", "3001111111", "MOTORCYCLE", "ABC12D"},
                            {"María Gómez", "maria", "1065222222", "3002222222", "MOTORCYCLE", "XYZ34E"},
                            {"Pedro Ruiz", "pedro", "1065333333", "3003333333", "BICYCLE", ""},
                            {"Ana Torres", "ana", "1065444444", "3004444444", "MOTORCYCLE", "JKL56F"} };
        for (String[] d : demo) {
            if (users.byUsername(d[1]).isPresent()) continue;
            UserAccount u = users.create(d[0], d[1], "domi1234", Role.COURIER, AccountStatus.ACTIVE);
            dispatch.registerCourier(u.getId(), d[0], d[1], d[2], d[3]);
            catalog.assignVehicle(u.getId(), d[5], VehicleType.valueOf(d[4]), null, "sistema");
        }
        if (users.byUsername("luis").isEmpty())
            users.registerCourier("Luis Díaz", "luis", "domi1234", "domi1234", "1065555555", "3005555555");
        if (catalog.searchCustomers("Restaurante").isEmpty()) {
            Customer c = catalog.saveCustomer(null, "Restaurante El Sabor", "6055801234", "sistema");
            catalog.addAddress(c.getId(), "Cra 9 #15-30, Centro", "Frente al parque", new GeoPoint(10.4747, -73.2448));
            Customer c2 = catalog.saveCustomer(null, "Laura Méndez", "3157778899", "sistema");
            catalog.addAddress(c2.getId(), "Calle 16 #12-45, Novalito", "Casa esquinera", new GeoPoint(10.4795, -73.2512));
        }
    }

    static final String POLICY_V1 = """
        POLÍTICA DE TRATAMIENTO DE DATOS PERSONALES Y DE UBICACIÓN (versión 1.0)

        En cumplimiento de la Ley 1581 de 2012 y el Decreto 1377 de 2013, al usar este sistema autorizas el tratamiento de tus datos:

        1. Datos que se recogen: nombre, documento, teléfono, usuario, y —solo mientras estás disponible, ocupado o en ruta— tu ubicación GPS.
           Nunca se captura tu ubicación cuando estás fuera de servicio.
        2. Finalidad: asignar y supervisar servicios de domicilio, calcular rutas y tiempos, liquidar pagos, atender novedades y reclamos,
           y generar indicadores de la operación.
        3. Quién accede: solo el personal autorizado de la central (administradores y despachadores).
        4. Conservación: el historial de ubicaciones se guarda máximo el número de días configurado (90 por defecto) y luego se borra
           automáticamente. Los demás registros se conservan mientras exista la relación con la empresa.
        5. Tus derechos: conocer, actualizar, rectificar y suprimir tus datos, y revocar esta autorización, escribiendo a la central.
        6. Las fotos y firmas de las pruebas de entrega se usan solo como evidencia del servicio.

        Al aceptar declaras que leíste esta política y autorizas el tratamiento de tus datos para las finalidades descritas.
        """;
}
