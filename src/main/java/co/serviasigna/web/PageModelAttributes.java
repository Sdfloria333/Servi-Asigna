package co.serviasigna.web;

import co.serviasigna.users.UserAccountService;
import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Pone en TODAS las páginas quién está conectado (nombre y rol) para el menú. */
@ControllerAdvice(basePackages = "co.serviasigna.web")
public class PageModelAttributes {
    private final UserAccountService users;

    public PageModelAttributes(UserAccountService users) { this.users = users; }

    @ModelAttribute("usuarioActual")
    public UserAccountService.Access currentUser(Principal principal) {
        return principal == null ? null : users.access(principal.getName());
    }

    @ModelAttribute("esAdmin")
    public boolean isAdmin(Principal principal) {
        if (principal == null) return false;
        UserAccountService.Access a = users.access(principal.getName());
        return a.role() != null && a.role().name().equals("ADMIN");
    }
}
