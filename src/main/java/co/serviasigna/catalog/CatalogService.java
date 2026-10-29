package co.serviasigna.catalog;

import co.serviasigna.domain.*;
import co.serviasigna.service.DataStore;
import co.serviasigna.service.DispatchService;
import co.serviasigna.service.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Datos maestros (módulo 2): clientes con direcciones (RF-M2-04), vehículos (RF-M2-02), documentos con vencimiento
 * (RF-M2-03), tarifas (RF-M2-07) y mensajes predefinidos (RF-M4-06). Nada se borra: se desactiva (RN-16).
 * Cuando algo afecta a un domiciliario (vehículo, documentos) se le pide a DispatchService que recalcule si puede recibir ofertas.
 */
@Service
public class CatalogService {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+() -]{7,20}$");
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[A-Z0-9-]{3,10}$");

    private final DataStore db;
    private final DispatchService dispatch;
    private final Clock clock;

    public CatalogService(DataStore db, DispatchService dispatch, Clock clock) {
        this.db = db; this.dispatch = dispatch; this.clock = clock;
    }

    // ---------------- Clientes y direcciones ----------------

    public record CustomerWithAddresses(Customer customer, List<Address> addresses, int deliveries) {}

    public List<Customer> searchCustomers(String text) {
        String t = text == null ? "" : text.trim();
        if (t.replaceAll("[^0-9]", "").length() >= 3 && t.matches("[0-9+() -]+"))
            return db.customers().findTop30ByActiveTrueAndPhoneContainingOrderByNameAsc(t.replaceAll("[^0-9]", ""));
        return db.customers().findTop30ByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(t);
    }

    public Optional<CustomerWithAddresses> customerByPhone(String phone) {
        String t = phone == null ? "" : phone.trim();
        return db.customers().findByPhone(t).filter(Customer::isActive).map(this::withAddresses);
    }

    public CustomerWithAddresses customer(long id) {
        return withAddresses(db.customers().findById(id).orElseThrow(() -> new IllegalArgumentException("No existe ese cliente")));
    }

    private CustomerWithAddresses withAddresses(Customer c) {
        return new CustomerWithAddresses(c, db.addresses().findByCustomerIdAndActiveTrue(c.getId()),
                db.deliveries().findByCustomerIdOrderByIdDesc(c.getId()).size());
    }

    public Customer saveCustomer(Long id, String name, String phone, String currentUser) {
        String n = name == null ? "" : name.trim();
        String t = phone == null ? "" : phone.trim();
        if (n.length() < 2 || n.length() > 80) throw new IllegalArgumentException("El nombre del cliente debe tener entre 2 y 80 letras");
        if (!PHONE_PATTERN.matcher(t).matches()) throw new IllegalArgumentException("Teléfono inválido");
        Optional<Customer> other = db.customers().findByPhone(t);
        if (other.isPresent() && !other.get().getId().equals(id)) throw new IllegalArgumentException("Ya existe un cliente con ese teléfono: " + other.get().getName());
        Customer c;
        if (id == null) c = db.customers().save(new Customer(n, t));
        else {
            c = db.customers().findById(id).orElseThrow(() -> new IllegalArgumentException("No existe ese cliente"));
            c.update(n, t);
            db.customers().save(c);
        }
        audit(currentUser, id == null ? "CREAR_CLIENTE" : "EDITAR_CLIENTE", "CLIENTE", c.getId());
        return c;
    }

    public void deactivateCustomer(long id, String currentUser) {
        Customer c = db.customers().findById(id).orElseThrow(() -> new IllegalArgumentException("No existe ese cliente"));
        c.setActive(false);
        db.customers().save(c);
        audit(currentUser, "DESACTIVAR_CLIENTE", "CLIENTE", id);
    }

    public Address addAddress(long customerId, String text, String reference, GeoPoint point) {
        db.customers().findById(customerId).orElseThrow(() -> new IllegalArgumentException("No existe ese cliente"));
        String t = text == null ? "" : text.trim();
        if (t.length() < 3) throw new IllegalArgumentException("Escribe la dirección");
        if (point == null || !point.isValid()) throw new IllegalArgumentException("Ubica la dirección en el mapa");
        return db.addresses().save(new Address(customerId, t.length() > 150 ? t.substring(0, 150) : t,
                reference == null || reference.isBlank() ? null : reference.trim(), point));
    }

    public void removeAddress(long id) {
        Address d = db.addresses().findById(id).orElseThrow(() -> new IllegalArgumentException("No existe esa dirección"));
        d.deactivate();
        db.addresses().save(d);
    }

    // ---------------- Vehículos (un vehículo activo por domiciliario) ----------------

    public List<Vehicle> vehicles() { return db.vehicles().findByActiveTrue(); }

    public Vehicle assignVehicle(long courierId, String plate, VehicleType type, Integer capacity, String currentUser) {
        db.couriers().findById(courierId).orElseThrow(() -> new IllegalArgumentException("Ese domiciliario no existe"));
        String p = plate == null ? "" : plate.trim().toUpperCase().replace(" ", "");
        if (type == null) throw new IllegalArgumentException("Escoge el tipo de vehículo");
        if (type == VehicleType.BICYCLE || type == VehicleType.ON_FOOT) { if (p.isEmpty()) p = "SIN-PLACA"; }
        else if (!PLATE_PATTERN.matcher(p).matches()) throw new IllegalArgumentException("Placa inválida (ej. ABC12D)");
        int cap = capacity != null ? capacity : switch (type) { case MOTORCYCLE -> 4; case BICYCLE -> 2; case CAR -> 10; case ON_FOOT -> 1; };
        if (cap < 1 || cap > 50) throw new IllegalArgumentException("La capacidad debe estar entre 1 y 50 unidades");
        final String finalPlate = p;
        if (!p.equals("SIN-PLACA") && db.vehicles().findByActiveTrue().stream()
                .anyMatch(v -> v.getPlate().equals(finalPlate) && !v.getCourierId().equals(courierId)))
            throw new IllegalArgumentException("Esa placa ya está registrada a otro domiciliario");
        for (Vehicle v : db.vehicles().findByCourierIdAndActiveTrue(courierId)) { v.deactivate(); db.vehicles().save(v); }
        Vehicle v = db.vehicles().save(new Vehicle(p, type, cap, courierId));
        audit(currentUser, "ASIGNAR_VEHICULO", "VEHICULO", v.getId());
        dispatch.refreshCourier(courierId);
        return v;
    }

    public void removeVehicle(long vehicleId, String currentUser) {
        Vehicle v = db.vehicles().findById(vehicleId).orElseThrow(() -> new IllegalArgumentException("No existe ese vehículo"));
        dispatch.validateNoDeliveries(v.getCourierId());
        v.deactivate();
        db.vehicles().save(v);
        audit(currentUser, "QUITAR_VEHICULO", "VEHICULO", vehicleId);
        dispatch.refreshCourier(v.getCourierId());
    }

    // ---------------- Tarifa ----------------

    /** Publica una tarifa nueva (la anterior queda inactiva, pero los servicios viejos conservan su valor). */
    public Rate newRate(String name, long base, long perKm, long medium, long large, long minimum, int percentage, String currentUser) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Ponle un nombre a la tarifa");
        if (base < 0 || perKm < 0 || medium < 0 || large < 0 || minimum < 0) throw new IllegalArgumentException("Los valores no pueden ser negativos");
        if (percentage < 0 || percentage > 100) throw new IllegalArgumentException("El porcentaje del domiciliario debe estar entre 0 y 100");
        db.rates().findFirstByActiveTrueOrderByIdDesc().ifPresent(t -> { t.deactivate(); db.rates().save(t); });
        Rate t = db.rates().save(new Rate(name.trim(), base, perKm, medium, large, minimum, percentage));
        audit(currentUser, "NUEVA_TARIFA", "TARIFA", t.getId());
        dispatch.rateChanged();
        return t;
    }

    // ---------------- Mensajes predefinidos ----------------

    public List<MessageTemplate> templates() { return db.templates().findByActiveTrueOrderByIdAsc(); }

    public MessageTemplate addTemplate(String text, String forRole) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty() || t.length() > 200) throw new IllegalArgumentException("Escribe el mensaje (máx. 200 caracteres)");
        if (!"CENTRAL".equals(forRole) && !"DOMICILIARIO".equals(forRole)) throw new IllegalArgumentException("Para quién: CENTRAL o DOMICILIARIO");
        return db.templates().save(new MessageTemplate(t, forRole));
    }

    public void removeTemplate(long id) {
        MessageTemplate p = db.templates().findById(id).orElseThrow(() -> new IllegalArgumentException("No existe ese mensaje"));
        p.deactivate();
        db.templates().save(p);
    }

    private void audit(String currentUser, String action, String entity, Long id) {
        db.audit().save(new AuditEntry(clock.now(), currentUser == null ? "sistema" : currentUser, action, entity, id, null, false));
    }
}
