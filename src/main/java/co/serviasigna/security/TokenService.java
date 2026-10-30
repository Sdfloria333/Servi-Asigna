package co.serviasigna.security;

import co.serviasigna.users.UserAccountService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite el token JWT con expiración (RF-M1-01, RNF-07). Lleva el usuario, sus roles y el identificador de sesión
 * (para la sesión única del domiciliario, RF-M1-04). En el navegador viaja en una cookie HttpOnly;
 * una app externa puede pedirlo en /api/auth/token y mandarlo como "Authorization: Bearer ...".
 */
@Service
public class TokenService {
    public static final String COOKIE = "token";
    private final JwtEncoder encoder;
    private final UserAccountService users;
    private final Duration duration;

    public TokenService(JwtEncoder encoder, UserAccountService users, @Value("${serviasigna.jwt.hours:10}") long hours) {
        this.encoder = encoder; this.users = users; this.duration = Duration.ofHours(hours);
    }

    public record Token(String value, Instant expires) {}

    public Token issue(Authentication auth) {
        String session = users.newSession(auth.getName());
        Instant now = Instant.now();
        List<String> roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("serviasigna").subject(auth.getName()).issuedAt(now).expiresAt(now.plus(duration))
                .claim("roles", roles).claim("sid", session).build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Token(value, now.plus(duration));
    }

    /** Deja el token en una cookie HttpOnly (JavaScript no puede leerla: protege contra XSS). */
    public void setCookie(HttpServletRequest req, HttpServletResponse res, Token t) {
        Cookie c = new Cookie(COOKIE, t.value());
        c.setHttpOnly(true);
        c.setSecure(req.isSecure());
        c.setPath("/");
        c.setMaxAge((int) duration.toSeconds());
        c.setAttribute("SameSite", "Lax");
        res.addCookie(c);
    }

    public static void clearCookie(HttpServletResponse res) {
        Cookie c = new Cookie(COOKIE, "");
        c.setPath("/");
        c.setMaxAge(0);
        c.setHttpOnly(true);
        res.addCookie(c);
    }
}
