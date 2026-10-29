package co.serviasigna.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * De dónde sale el token: la cabecera "Authorization: Bearer ..." o, en el navegador, la cookie "token".
 * En login, registro y archivos estáticos se ignora la cookie (un token viejo no debe estorbar al entrar de nuevo).
 */
public class TokenResolver implements BearerTokenResolver {
    private final DefaultBearerTokenResolver header = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest req) {
        String fromHeader = header.resolve(req);
        if (fromHeader != null) return fromHeader;
        String route = req.getRequestURI().substring(req.getContextPath().length());
        if (SecurityConfig.isPublic(route)) return null;
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) if (TokenService.COOKIE.equals(c.getName()) && !c.getValue().isBlank()) return c.getValue();
        return null;
    }
}
