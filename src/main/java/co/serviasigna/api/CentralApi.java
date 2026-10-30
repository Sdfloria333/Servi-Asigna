package co.serviasigna.api;

import co.serviasigna.api.Requests.*;
import co.serviasigna.catalog.CatalogService;
import co.serviasigna.domain.*;
import co.serviasigna.reports.IndicatorService;
import co.serviasigna.service.*;
import co.serviasigna.service.view.CentralView;
import co.serviasigna.service.view.MessageView;
import co.serviasigna.users.UserAccountService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** API de la central (ADMIN y DESPACHADOR; ver SecurityConfig). Todo responde JSON. */
@RestController
@RequestMapping("/api/central")
public class CentralApi {
    private final DispatchService dispatch;
    private final QueryService queries;
    private final CatalogService catalog;
    private final IndicatorService indicators;
    private final UserAccountService users;
    private final CurrentUser currentUser;

    public CentralApi(DispatchService dispatch, QueryService queries, CatalogService catalog, IndicatorService indicators,
                      UserAccountService users, CurrentUser currentUser) {
        this.dispatch = dispatch; this.queries = queries; this.catalog = catalog; this.indicators = indicators;
        this.users = users; this.currentUser = currentUser;
    }

    // ---------------- Operación en vivo ----------------

    /** Todo lo que pintan las pantallas (también llega solo por STOMP en /topic/central). */
    @GetMapping("/status")
    public CentralView status() { return dispatch.view(users.countPending()); }

    @PostMapping("/mode")
    public Map<String, String> mode(@Valid @RequestBody ModeData m, Principal p) {
        dispatch.changeMode(m.mode(), p.getName());
        return Map.of("mode", dispatch.mode().name());
    }

    // ---------------- Servicios ----------------

    @PostMapping("/deliveries")
    public Map<String, Object> create(@RequestBody DeliveryData d, Principal p) {
        Delivery s = dispatch.createDelivery(d, p.getName());
        return Map.of("id", s.getId(), "pin", s.getPin(), "value", s.getValue(), "status", s.getStatus().name());
    }

    /** Valor sugerido con la tarifa vigente (RF-M3-10). */
    @GetMapping("/deliveries/value")
    public Map<String, Long> value(@RequestParam double plat, @RequestParam double plng, @RequestParam double dlat, @RequestParam double dlng,
                                   @RequestParam(defaultValue = "SMALL") PackageSize size) {
        return Map.of("value", dispatch.suggestedValue(new GeoPoint(plat, plng), new GeoPoint(dlat, dlng), size));
    }

    @PutMapping("/deliveries/{id}")
    public Map<String, Boolean> edit(@PathVariable long id, @RequestBody EditData d, Principal p) {
        dispatch.editDelivery(id, d, p.getName());
        return Map.of("ok", true);
    }

    @PostMapping("/deliveries/{id}/assign")
    public Map<String, Boolean> assign(@PathVariable long id, @Valid @RequestBody AssignData a, Principal p) {
        dispatch.assignManually(id, a.courierId(), a.reason(), p.getName());
        return Map.of("ok", true);
    }

    @PostMapping("/deliveries/{id}/revoke")
    public Map<String, Boolean> revoke(@PathVariable long id, @Valid @RequestBody Reason m, Principal p) {
        dispatch.revoke(id, m.reason(), p.getName());
        return Map.of("ok", true);
    }

    @PostMapping("/deliveries/{id}/cancel")
    public Map<String, Boolean> cancel(@PathVariable long id, @Valid @RequestBody Reason m, Principal p) {
        dispatch.cancel(id, m.reason(), p.getName());
        return Map.of("ok", true);
    }

    /** Búsqueda histórica (RF-M3-06): fechas yyyy-MM-dd, estado, texto libre (cliente, teléfono, dirección o #id). */
    @GetMapping("/deliveries/search")
    public List<QueryService.DeliveryRow> search(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
                                                      @RequestParam(required = false) String status, @RequestParam(required = false) String text,
                                                      @RequestParam(required = false) Long courierId) {
        DateRange.Minutes r = DateRange.of(from, to);
        return queries.search(r.from(), r.to(), status, text, courierId);
    }

    /** Detalle con línea de tiempo, ofertas, evidencias, decisiones del motor, novedades y recorrido. */
    @GetMapping("/deliveries/{id}")
    public QueryService.Detail detail(@PathVariable long id) { return queries.detail(id); }

    // ---------------- Domiciliarios ----------------

    @GetMapping("/couriers/{id}/messages")
    public List<MessageView> messages(@PathVariable long id) { return dispatch.conversation(id, true); }

    @PostMapping("/couriers/{id}/messages")
    public MessageView write(@PathVariable long id, @Valid @RequestBody TextData t, Principal p) {
        ChatMessage m = dispatch.messageToCourier(id, t.text(), currentUser.of(p).name());
        return new MessageView(m.getId(), id, true, m.getAuthor(), m.getText(), m.getDate(), m.isRead());
    }

    /** La central recibe el dinero que cobró el domiciliario (RF-M8-03). */
    @PostMapping("/couriers/{id}/collection")
    public Map<String, Object> collected(@PathVariable long id, @Valid @RequestBody AmountData v, Principal p) {
        Shift t = dispatch.receiveCollection(id, v.value(), p.getName());
        return Map.of("pending", t.getPending(), "delivered", t.getCollectedHandedOver());
    }

    /** Por dónde anduvo en las últimas horas (máximo 24). */
    @GetMapping("/couriers/{id}/track")
    public List<GeoPoint> track(@PathVariable long id, @RequestParam(defaultValue = "2") double hours) {
        double now = dispatch.now();
        return queries.track(id, now - Math.min(24, Math.max(0.1, hours)) * 60, now);
    }

    @GetMapping("/shifts")
    public List<Shift> shifts(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        DateRange.Minutes r = DateRange.of(from, to);
        return queries.shifts(r.from(), r.to());
    }

    // ---------------- Novedades ----------------

    @GetMapping("/incidents")
    public List<QueryService.IncidentReport> incidents(@RequestParam(defaultValue = "true") boolean open,
                                                    @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        DateRange.Minutes r = DateRange.of(from, to);
        return queries.incidents(open, r.from(), r.to());
    }

    @GetMapping("/incidents/{id}/photo")
    public Map<String, String> photo(@PathVariable long id) {
        String f = queries.incidentPhoto(id);
        return Map.of("photo", f == null ? "" : f);
    }

    @PostMapping("/incidents")
    public Map<String, Long> report(@Valid @RequestBody NewIncident n, Principal p) {
        Incident i = dispatch.reportIncident(null, p.getName(), n.type(), n.deliveryId(), n.description(), n.photo());
        return Map.of("id", i.getId());
    }

    @PostMapping("/incidents/{id}/comment")
    public Map<String, Boolean> comment(@PathVariable long id, @Valid @RequestBody TextData t, Principal p) {
        dispatch.commentIncident(id, t.text(), p.getName());
        return Map.of("ok", true);
    }

    @PostMapping("/incidents/{id}/decision")
    public Map<String, Boolean> decide(@PathVariable long id, @Valid @RequestBody DecisionData d, Principal p) {
        dispatch.decideIncident(id, d.decision(), d.resolution(), p.getName());
        return Map.of("ok", true);
    }

    // ---------------- Clientes (RF-M3-09) ----------------

    @GetMapping("/customers")
    public List<Customer> customers(@RequestParam(defaultValue = "") String q) { return catalog.searchCustomers(q); }

    @GetMapping("/customers/phone/{phone}")
    public ResponseEntity<CatalogService.CustomerWithAddresses> byPhone(@PathVariable String phone) {
        return catalog.customerByPhone(phone).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/customers/{id}")
    public Map<String, Object> customer(@PathVariable long id) {
        return Map.of("customer", catalog.customer(id), "deliveries", queries.byCustomer(id));
    }

    @PostMapping("/customers")
    public Customer saveCustomer(@Valid @RequestBody CustomerData c, Principal p) {
        return catalog.saveCustomer(c.id(), c.name(), c.phone(), p.getName());
    }

    @DeleteMapping("/customers/{id}")
    public Map<String, Boolean> removeCustomer(@PathVariable long id, Principal p) {
        catalog.deactivateCustomer(id, p.getName());
        return Map.of("ok", true);
    }

    @PostMapping("/customers/{id}/addresses")
    public Address addAddress(@PathVariable long id, @Valid @RequestBody AddressData d) {
        return catalog.addAddress(id, d.text(), d.reference(), new GeoPoint(d.lat(), d.lng()));
    }

    @DeleteMapping("/addresses/{id}")
    public Map<String, Boolean> removeAddress(@PathVariable long id) {
        catalog.removeAddress(id);
        return Map.of("ok", true);
    }

    @GetMapping("/templates")
    public List<MessageTemplate> templates() { return catalog.templates(); }

    // ---------------- Indicadores y reportes (M10) ----------------

    @GetMapping("/indicators")
    public IndicatorService.Report indicators(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        DateRange.Minutes r = DateRange.of(from, to);
        return indicators.calculate(r.from(), r.to());
    }

    @GetMapping("/reports/deliveries.csv")
    public ResponseEntity<byte[]> deliveriesCsv(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        DateRange.Minutes r = DateRange.of(from, to);
        return csv("servicios", from, indicators.deliveriesCsv(r.from(), r.to()));
    }

    @GetMapping("/reports/indicators.csv")
    public ResponseEntity<byte[]> indicatorsCsv(@RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        DateRange.Minutes r = DateRange.of(from, to);
        return csv("indicadores", from, indicators.indicatorsCsv(r.from(), r.to()));
    }

    private static ResponseEntity<byte[]> csv(String name, String from, String content) {
        String day = from == null || from.isBlank() ? LocalDate.now(DateRange.ZONE).toString() : from;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "-" + day.replaceAll("[^0-9-]", "") + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}
