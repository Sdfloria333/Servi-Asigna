package co.serviasigna.security;

import co.serviasigna.users.UserAccountService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Quién puede entrar a qué (RF-M1-02). Es el único lugar donde se definen los permisos:
 *   - /login, /registro y archivos estáticos: cualquiera.
 *   - /central/configuracion y /api/central/admin/**: solo ADMIN.
 *   - /central/** y /api/central/**: ADMIN o DESPACHADOR.
 *   - /domi/** y /api/domi/**: solo DOMICILIARIO.
 *   - lo demás (radio, tiempo real, cambio de clave, política): cualquier cuenta que haya entrado.
 * Sin sesión en el servidor: cada petición trae un token JWT con expiración (cookie HttpOnly o cabecera Bearer).
 * Los formularios y las llamadas que modifican datos llevan token CSRF (cookie XSRF-TOKEN).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String[] PUBLIC_PATHS = { "/login", "/register", "/error", "/forbidden", "/favicon.ico", "/css/", "/js/", "/vendor/",
            "/manifest.webmanifest", "/sw.js", "/icons/", "/api/auth/token" };

    static boolean isPublic(String route) {
        for (String p : PUBLIC_PATHS) if (p.endsWith("/") ? route.startsWith(p) : route.equals(p)) return true;
        return false;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecretKey jwtKey(@Value("${serviasigna.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("serviasigna.jwt.secret debe tener mínimo 32 caracteres");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey password) { return new NimbusJwtEncoder(new ImmutableSecret<>(password)); }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey password, UserAccountService users) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(password).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("serviasigna"), new SessionValidator(users)));
        return decoder;
    }

    /** Los roles del token ("roles": ["ADMIN"]) se vuelven ROLE_ADMIN para Spring. */
    private JwtAuthenticationConverter converter() {
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> list = new ArrayList<>();
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) for (String r : roles) list.add(new SimpleGrantedAuthority("ROLE_" + r));
            return list;
        });
        return c;
    }

    /** Sin token válido: las páginas van al login (y se borra la cookie vieja); la API responde 401. */
    private AuthenticationEntryPoint input() {
        return (req, res, error) -> {
            String route = req.getRequestURI().substring(req.getContextPath().length());
            boolean hadToken = req.getCookies() != null && java.util.Arrays.stream(req.getCookies()).anyMatch(c -> TokenService.COOKIE.equals(c.getName()));
            TokenService.clearCookie(res);
            if (route.startsWith("/api/") || route.startsWith("/ws/")) {
                res.setStatus(401);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Tu sesión terminó: vuelve a entrar\"}");
            } else {
                res.sendRedirect(req.getContextPath() + (hadToken ? "/login?expired" : "/login"));
            }
        };
    }

    /** Con sesión pero sin permiso: la API responde 403 en JSON y las páginas muestran "No tienes permiso". */
    private org.springframework.security.web.access.AccessDeniedHandler forbidden() {
        return (req, res, error) -> {
            String route = req.getRequestURI().substring(req.getContextPath().length());
            res.setStatus(403);
            if (route.startsWith("/api/") || route.startsWith("/ws/")) {
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"No tienes permiso para esto\"}");
            } else {
                req.getRequestDispatcher("/forbidden").forward(req, res);
            }
        };
    }

    @Bean
    public SecurityFilterChain text(HttpSecurity http, TokenService tokens, UserAccountService users) throws Exception {
        AuthenticationEntryPoint input = input();
        http
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(c -> c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers("/api/auth/token")
                    .ignoringRequestMatchers(req -> req.getHeader("Authorization") != null))     // apps con Bearer no usan cookies
            .authorizeHttpRequests(a -> a
                .requestMatchers(PUBLIC_MATCHERS).permitAll()
                .requestMatchers("/central/settings", "/central/accounts", "/api/central/admin/**").hasRole("ADMIN")
                .requestMatchers("/central", "/central/**", "/api/central/**").hasAnyRole("ADMIN", "DISPATCHER")
                .requestMatchers("/courier", "/courier/**", "/api/courier/**").hasRole("COURIER")
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .failureHandler((req, res, error) -> res.sendRedirect(req.getContextPath() + (
                        error instanceof DisabledException ? "/login?pending"
                        : error instanceof LockedException ? "/login?locked" : "/login?error")))
                .successHandler((req, res, auth) -> {
                    tokens.setCookie(req, res, tokens.issue(auth));
                    boolean courier = auth.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_COURIER"));
                    res.sendRedirect(req.getContextPath() + (courier ? "/courier" : "/central"));
                }))
            .oauth2ResourceServer(o -> o
                .bearerTokenResolver(new TokenResolver())
                .authenticationEntryPoint(input)
                .accessDeniedHandler(forbidden())
                .jwt(j -> j.jwtAuthenticationConverter(converter())))
            .exceptionHandling(e -> e.authenticationEntryPoint(input).accessDeniedHandler(forbidden()))
            .logout(l -> l
                .logoutUrl("/logout")
                .addLogoutHandler((req, res, auth) -> { if (auth != null) users.logout(auth.getName()); })
                .logoutSuccessHandler((req, res, auth) -> { TokenService.clearCookie(res); res.sendRedirect(req.getContextPath() + "/login?loggedOut"); }))
            .addFilterAfter(new AccessFilter(users), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    private static final String[] PUBLIC_MATCHERS = { "/login", "/register", "/error", "/forbidden", "/favicon.ico", "/css/**", "/js/**", "/vendor/**",
            "/manifest.webmanifest", "/sw.js", "/icons/**", "/api/auth/token" };
}
