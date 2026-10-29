package co.serviasigna.security;

import co.serviasigna.users.UserAccountService;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Además de la firma y la expiración, el token debe ser de una cuenta activa y, si es domiciliario,
 * de su última sesión: si entra desde otro celular, el anterior queda fuera (RF-M1-04).
 */
public class SessionValidator implements OAuth2TokenValidator<Jwt> {
    private final UserAccountService users;

    public SessionValidator(UserAccountService users) { this.users = users; }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (users.validSession(jwt.getSubject(), jwt.getClaimAsString("sid"))) return OAuth2TokenValidatorResult.success();
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "La sesión terminó o se abrió en otro dispositivo", null));
    }
}
