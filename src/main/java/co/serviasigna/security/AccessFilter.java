package co.serviasigna.security;

import co.serviasigna.users.UserAccountService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Antes de usar el sistema, la persona debe:
 *   1. cambiar la contraseña temporal (si se la restablecieron o es una cuenta nueva de la central);
 *   2. aceptar la política de tratamiento de datos y ubicación vigente (RF-M1-06, Ley 1581).
 * Si falta algo, las páginas redirigen a /cambiar-clave o /politica y la API responde 403 con el motivo.
 */
public class AccessFilter extends OncePerRequestFilter {
    private final UserAccountService users;

    public AccessFilter(UserAccountService users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain text) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String route = req.getRequestURI().substring(req.getContextPath().length());
        if (auth instanceof JwtAuthenticationToken && !SecurityConfig.isPublic(route) && !route.equals("/logout") && !route.startsWith("/api/auth/")) {
            UserAccountService.Access a = users.access(auth.getName());
            String destination = null, reason = null;
            if (a.mustChangePassword() && !route.equals("/change-password")) { destination = "/change-password"; reason = "Debes cambiar tu contraseña temporal"; }
            else if (!a.mustChangePassword() && a.mustAcceptPolicy() && !route.equals("/policy")) { destination = "/policy"; reason = "Debes aceptar la política de datos"; }
            if (destination != null) {
                if (route.startsWith("/api/") || route.startsWith("/ws/")) {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"error\":\"" + reason + "\"}");
                } else {
                    res.sendRedirect(req.getContextPath() + destination);
                }
                return;
            }
        }
        text.doFilter(req, res);
    }
}
