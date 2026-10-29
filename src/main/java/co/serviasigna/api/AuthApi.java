package co.serviasigna.api;

import co.serviasigna.api.Requests.PasswordChange;
import co.serviasigna.api.Requests.Login;
import co.serviasigna.security.TokenService;
import co.serviasigna.users.UserAccountService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

/**
 * Token para apps que no usan la cookie del navegador (POST /api/auth/token → "Authorization: Bearer ...").
 * También: quién soy y cambio de contraseña por API.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApi {
    private final AuthenticationConfiguration authentication;
    private final TokenService tokens;
    private final UserAccountService users;

    public AuthApi(AuthenticationConfiguration authentication, TokenService tokens, UserAccountService users) {
        this.authentication = authentication; this.tokens = tokens; this.users = users;
    }

    @PostMapping("/token")
    public Map<String, Object> token(@Valid @RequestBody Login i) throws Exception {
        AuthenticationManager am = authentication.getAuthenticationManager();
        Authentication auth;
        try {
            auth = am.authenticate(new UsernamePasswordAuthenticationToken(i.username().trim().toLowerCase(), i.password()));
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos, o la cuenta no está activa");
        }
        TokenService.Token t = tokens.issue(auth);
        return Map.of("token", t.value(), "expires", t.expires().toString(), "type", "Bearer");
    }

    @GetMapping("/me")
    public Map<String, Object> me(Principal p) {
        UserAccountService.Access a = users.access(p.getName());
        return Map.of("username", p.getName(), "name", a.name() == null ? "" : a.name(), "role", a.role() == null ? "" : a.role().name());
    }

    /** Política vigente y si ya la aceptó. */
    @GetMapping("/policy")
    public Map<String, Object> policy(Principal p) {
        Map<String, Object> m = new java.util.HashMap<>();
        users.currentPolicy().ifPresent(x -> { m.put("version", x.getVersion()); m.put("text", x.getText()); });
        m.put("mustAccept", users.access(p.getName()).mustAcceptPolicy());
        return m;
    }

    @PostMapping("/policy")
    public Map<String, Boolean> acceptPolicy(Principal p, jakarta.servlet.http.HttpServletRequest req) {
        users.acceptPolicy(p.getName(), req.getRemoteAddr());
        return Map.of("ok", true);
    }

    @PostMapping("/password")
    public Map<String, Boolean> password(@Valid @RequestBody PasswordChange c, Principal p) {
        users.changePassword(p.getName(), c.current(), c.newPassword(), c.confirmation());
        return Map.of("ok", true);
    }
}
