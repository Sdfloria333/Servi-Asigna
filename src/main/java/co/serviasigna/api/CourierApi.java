package co.serviasigna.api;

import co.serviasigna.api.Requests.*;
import co.serviasigna.domain.Incident;
import co.serviasigna.domain.ChatMessage;
import co.serviasigna.domain.GeoPoint;
import co.serviasigna.service.Confirmation;
import co.serviasigna.service.DispatchService;
import co.serviasigna.service.view.MessageView;
import co.serviasigna.service.view.MyShiftView;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.web.bind.annotation.*;

/** API de la app del domiciliario (rol DOMICILIARIO). Quién es sale del token: nunca de la URL. */
@RestController
@RequestMapping("/api/courier")
public class CourierApi {
    private final DispatchService dispatch;
    private final CurrentUser currentUser;

    public CourierApi(DispatchService dispatch, CurrentUser currentUser) { this.dispatch = dispatch; this.currentUser = currentUser; }

    /** Mi estado, mi ruta, mi oferta, mis mensajes y mi turno. Cuenta como señal de vida. */
    @GetMapping("/me")
    public MyShiftView me(Principal p) {
        MyShiftView v = dispatch.shiftOf(currentUser.id(p));
        if (v == null) throw new NoSuchElementException("Tu cuenta aún no está lista: habla con la central");
        return v;
    }

    /** Disponible / Ocupado / Fuera de servicio (RF-M6-02). Al ponerse disponible manda su ubicación. */
    @PostMapping("/status")
    public MyShiftView status(@Valid @RequestBody StatusUpdate c, Principal p) {
        long id = currentUser.id(p);
        GeoPoint pos = c.lat() == null || c.lng() == null ? null : new GeoPoint(c.lat(), c.lng());
        dispatch.changeStatus(id, c.status(), pos, c.precision());
        return dispatch.shiftOf(id);
    }

    /** Posición del GPS (RF-M6-10). capturedMs = cuándo la tomó el celular (si se mandó tarde por falta de señal). */
    @PostMapping("/location")
    public Map<String, Boolean> location(@Valid @RequestBody Location u, Principal p) {
        Double capture = u.capturedMs() == null ? null : u.capturedMs() / 60000.0;
        dispatch.recordLocation(currentUser.id(p), new GeoPoint(u.lat(), u.lng()), u.precision(), capture);
        return Map.of("ok", true);
    }


    @PostMapping("/offers/{id}")
    public MyShiftView reply(@PathVariable long id, @Valid @RequestBody OfferReply r, Principal p) {
        long courier = currentUser.id(p);
        dispatch.replyToOffer(courier, id, r.accept(), r.reason());
        return dispatch.shiftOf(courier);
    }

    /** Recogí / Entregué con sus pruebas. Sirve también para los eventos guardados sin conexión (eventId evita duplicados). */
    @PostMapping("/confirm")
    public Map<String, String> confirm(@RequestBody Confirmation c, Principal p) {
        String r = dispatch.confirmStop(currentUser.id(p), c);
        return Map.of("message", r == null ? "Listo" : r);
    }

    @PostMapping("/incident")
    public Map<String, Long> incident(@Valid @RequestBody NewIncident n, Principal p) {
        Incident i = dispatch.reportIncident(currentUser.id(p), p.getName(), n.type(), n.deliveryId(), n.description(), n.photo());
        return Map.of("id", i.getId());
    }

    @GetMapping("/messages")
    public List<MessageView> messages(Principal p) { return dispatch.conversation(currentUser.id(p), false); }

    @PostMapping("/messages")
    public MessageView write(@Valid @RequestBody TextData t, Principal p) {
        long id = currentUser.id(p);
        ChatMessage m = dispatch.messageToCentral(id, t.text());
        return new MessageView(m.getId(), id, false, m.getAuthor(), m.getText(), m.getDate(), m.isRead());
    }
}
