package co.serviasigna.api;

import co.serviasigna.api.Requests.*;
import co.serviasigna.catalog.CatalogService;
import co.serviasigna.domain.*;
import co.serviasigna.service.QueryService;
import co.serviasigna.service.DispatchService;
import co.serviasigna.service.Settings;
import co.serviasigna.users.Role;
import co.serviasigna.users.UserAccount;
import co.serviasigna.users.UserAccountService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

/**
 * Administración (solo ADMIN): cuentas, aprobación de domiciliarios, vehículos, documentos,
 * parámetros de operación, tarifas, política de datos, plantillas y auditoría (M1, M2, M10).
 */
@RestController
@RequestMapping("/api/central/admin")
public class AdminApi {
    private final UserAccountService users;
    private final DispatchService dispatch;
    private final CatalogService catalog;
    private final Settings settings;
    private final QueryService queries;

    public AdminApi(UserAccountService users, DispatchService dispatch, CatalogService catalog, Settings settings, QueryService queries) {
        this.users = users; this.dispatch = dispatch; this.catalog = catalog; this.settings = settings; this.queries = queries;
    }

    /** Una cuenta para la pantalla (sin la clave). Si es domiciliario trae sus datos y su vehículo. */
    public record Account(long id, String name, String username, String role, String accountStatus, boolean mustChangePassword,
                         LocalDateTime created, LocalDateTime lastLogin, String idNumber, String phone,
                         Vehicle vehicle) {}

    @GetMapping("/users")
    public List<Account> accounts() {
        Map<Long, Courier> couriers = dispatch.couriers().stream().collect(Collectors.toMap(Courier::getId, d -> d, (a, b) -> a));
        Map<Long, Vehicle> vehicles = dispatch.vehicles();
        List<Account> list = new ArrayList<>();
        for (UserAccount u : users.all()) {
            Courier d = couriers.get(u.getId());
            list.add(new Account(u.getId(), u.getName(), u.getUsername(), u.getRole().name(), u.getAccountStatus().name(), u.isMustChangePassword(),
                    u.getCreated(), u.getLastLogin(), d == null ? null : d.getIdNumber(), d == null ? null : d.getPhone(),
                    vehicles.get(u.getId())));
        }
        return list;
    }

    @PostMapping("/users")
    public Map<String, Long> create(@Valid @RequestBody NewAccount c, Principal p) {
        if (c.role() == Role.COURIER) throw new IllegalArgumentException("Los domiciliarios se registran solos en /registro y aquí se aprueban");
        return Map.of("id", users.createCentralAccount(c.name(), c.username(), c.password(), c.role(), p.getName()).getId());
    }

    @PostMapping("/users/{id}/approve")
    public Map<String, Boolean> approve(@PathVariable long id, Principal p) { users.approve(id, p.getName()); return Map.of("ok", true); }

    @PostMapping("/users/{id}/reject")
    public Map<String, Boolean> reject(@PathVariable long id, Principal p) { users.reject(id, p.getName()); return Map.of("ok", true); }

    @PostMapping("/users/{id}/status")
    public Map<String, Boolean> status(@PathVariable long id, @Valid @RequestBody AccountStatusData e, Principal p) {
        if (users.byUsername(p.getName()).map(UserAccount::getId).orElse(-1L) == id) throw new IllegalArgumentException("No puedes cambiar el estado de tu propia cuenta");
        users.changeAccountStatus(id, e.status(), p.getName());
        return Map.of("ok", true);
    }

    /** Clave temporal (se muestra una sola vez; la persona debe cambiarla al entrar). */
    @PostMapping("/users/{id}/password")
    public Map<String, String> reset(@PathVariable long id, Principal p) { return Map.of("temporary", users.resetPassword(id, p.getName())); }

    @PutMapping("/users/{id}")
    public Map<String, Boolean> edit(@PathVariable long id, @Valid @RequestBody CourierData d, Principal p) {
        users.editCourier(id, d.name(), d.idNumber(), d.phone(), p.getName());
        return Map.of("ok", true);
    }

    // ---------------- Vehículos (RF-M2-02) ----------------

    @PostMapping("/vehicles")
    public Vehicle vehicle(@Valid @RequestBody VehicleData v, Principal p) {
        return catalog.assignVehicle(v.courierId(), v.plate(), v.type(), v.capacity(), p.getName());
    }

    @DeleteMapping("/vehicles/{id}")
    public Map<String, Boolean> removeVehicle(@PathVariable long id, Principal p) { catalog.removeVehicle(id, p.getName()); return Map.of("ok", true); }

    // ---------------- Parámetros, tarifa, política y plantillas ----------------

    @GetMapping("/settings")
    public List<Map<String, String>> settings() { return settings.list(); }

    @PutMapping("/settings/{key}")
    public Map<String, Boolean> setting(@PathVariable String key, @Valid @RequestBody SettingData d, Principal p) {
        dispatch.changeSetting(key, d.value(), p.getName());
        return Map.of("ok", true);
    }

    @GetMapping("/rate")
    public Rate rate() { return dispatch.currentRate(); }

    @PostMapping("/rate")
    public Rate newRate(@Valid @RequestBody RateData t, Principal p) {
        return catalog.newRate(t.name(), t.base(), t.perKm(), t.mediumSurcharge(), t.largeSurcharge(), t.minimum(), t.courierPercentage(), p.getName());
    }

    @GetMapping("/policy")
    public Map<String, Object> policy() {
        return users.currentPolicy().<Map<String, Object>>map(x -> Map.of("version", x.getVersion(), "text", x.getText(),
                "effectiveFrom", x.getEffectiveFrom())).orElse(Map.of());
    }

    @PostMapping("/policy")
    public Map<String, String> publishPolicy(@Valid @RequestBody NewPolicy n, Principal p) {
        return Map.of("version", users.publishPolicy(n.version(), n.text(), p.getName()).getVersion());
    }

    @PostMapping("/templates")
    public MessageTemplate template(@Valid @RequestBody NewTemplate n) { return catalog.addTemplate(n.text(), n.forRole()); }

    @DeleteMapping("/templates/{id}")
    public Map<String, Boolean> removeTemplate(@PathVariable long id) { catalog.removeTemplate(id); return Map.of("ok", true); }

    @GetMapping("/audit")
    public List<AuditEntry> audit(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        DateRange.Minutes r = DateRange.of(from, to);
        return queries.audit(r.from(), r.to());
    }

    @GetMapping("/today")
    public Map<String, String> today() { return Map.of("today", LocalDate.now(DateRange.ZONE).toString()); }
}
