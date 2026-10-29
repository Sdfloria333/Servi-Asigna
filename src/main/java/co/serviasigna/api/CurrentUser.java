package co.serviasigna.api;

import co.serviasigna.users.UserAccountService;
import java.security.Principal;
import org.springframework.stereotype.Component;

/** Quién hace la petición. Siempre sale del token, nunca de la URL ni del cuerpo (un domiciliario no puede hacerse pasar por otro). */
@Component
public class CurrentUser {
    private final UserAccountService users;

    public CurrentUser(UserAccountService users) { this.users = users; }

    public UserAccountService.Access of(Principal p) {
        if (p == null) throw new IllegalStateException("Debes iniciar sesión");
        UserAccountService.Access a = users.access(p.getName());
        if (a.id() == null || !a.active()) throw new IllegalStateException("Tu cuenta no está activa");
        return a;
    }

    public long id(Principal p) { return of(p).id(); }
}
