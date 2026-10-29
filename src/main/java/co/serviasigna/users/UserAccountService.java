package co.serviasigna.users;

import co.serviasigna.domain.*;
import co.serviasigna.persistence.PolicyAcceptanceRepository;
import co.serviasigna.persistence.AuditEntryRepository;
import co.serviasigna.persistence.DataPolicyRepository;
import co.serviasigna.service.DispatchService;
import co.serviasigna.service.Clock;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Cuentas y política de datos:
 *   - registro de domiciliarios (quedan PENDIENTES) y aprobación una sola vez por la central;
 *   - cuentas de Administrador y Despachador (las crea el administrador, RF-M1-03);
 *   - activar, desactivar o suspender (nunca se borran, RN-16) y restablecer contraseña con clave temporal;
 *   - una sola sesión activa por domiciliario (RF-M1-04);
 *   - aceptación de la política de tratamiento de datos y ubicación con versión y fecha (RF-M1-06, Ley 1581 de 2012).
 * Las validaciones lanzan IllegalArgumentException con un mensaje que se le muestra tal cual al usuario.
 */
@Service
public class UserAccountService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{3,30}$");
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("^[0-9A-Za-z-]{5,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+() -]{7,20}$");
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UserAccountRepository repository;
    private final PasswordEncoder encoder;
    private final DispatchService dispatch;
    private final DataPolicyRepository policies;
    private final PolicyAcceptanceRepository acceptances;
    private final AuditEntryRepository audit;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Access> accesses = new java.util.concurrent.ConcurrentHashMap<>();

    /** Lo que la seguridad revisa en cada petición (guardado en memoria 30 s para no consultar la base cada vez). */
    public record Access(Long id, String name, Role role, boolean active, String session, boolean mustChangePassword,
                         boolean mustAcceptPolicy, long expires) {}

    public UserAccountService(UserAccountRepository repository, PasswordEncoder encoder, DispatchService dispatch,
                          DataPolicyRepository policies, PolicyAcceptanceRepository acceptances, AuditEntryRepository audit, Clock clock) {
        this.repository = repository; this.encoder = encoder; this.dispatch = dispatch;
        this.policies = policies; this.acceptances = acceptances; this.audit = audit; this.clock = clock;
    }

    // ---------------- Registro y creación ----------------

    /** Registro público: crea un DOMICILIARIO pendiente de aprobación con su documento y teléfono. */
    public UserAccount registerCourier(String name, String username, String password, String confirmation, String idNumber, String phone) {
        if (!Objects.equals(password, confirmation)) throw new IllegalArgumentException("Las contraseñas no coinciden");
        String doc = idNumber == null ? "" : idNumber.trim();
        String tel = phone == null ? "" : phone.trim();
        if (!DOCUMENT_PATTERN.matcher(doc).matches()) throw new IllegalArgumentException("Escribe tu número de documento (5 a 20 caracteres)");
        if (!PHONE_PATTERN.matcher(tel).matches()) throw new IllegalArgumentException("Escribe un teléfono válido");
        UserAccount u = create(name, username, password, Role.COURIER, AccountStatus.PENDING);
        dispatch.registerCourier(u.getId(), u.getName(), u.getUsername(), doc, tel);
        return u;
    }

    /** El administrador crea una cuenta de la central (Administrador o Despachador). */
    public UserAccount createCentralAccount(String name, String username, String password, Role role, String currentUser) {
        if (role == null || !role.isCentral()) throw new IllegalArgumentException("El rol debe ser Administrador o Despachador");
        UserAccount u = create(name, username, password, role, AccountStatus.ACTIVE);
        u.changePassword(u.getPassword(), true);                // debe cambiarla al entrar
        repository.save(u);
        audit(currentUser, "CREAR_CUENTA", u.getId(), role.name());
        return u;
    }

    public UserAccount create(String name, String username, String password, Role role, AccountStatus status) {
        name = name == null ? "" : name.trim();
        username = normalize(username);
        if (name.length() < 2 || name.length() > 80) throw new IllegalArgumentException("El nombre debe tener entre 2 y 80 letras");
        if (!USERNAME_PATTERN.matcher(username).matches())
            throw new IllegalArgumentException("El usuario debe tener de 3 a 30 caracteres: letras, números, punto, guion o guion bajo (sin espacios)");
        validatePassword(password);
        if (repository.existsByUsername(username)) throw new IllegalArgumentException("El usuario \"" + username + "\" ya existe");
        return repository.save(new UserAccount(name, username, encoder.encode(password), role, status));
    }

    // ---------------- Aprobación y estado de la cuenta ----------------

    /** La central acepta a un domiciliario nuevo (una sola vez). */
    public void approve(long id, String currentUser) {
        UserAccount u = search(id);
        if (u.getAccountStatus() != AccountStatus.PENDING) throw new IllegalArgumentException("Esa cuenta ya fue revisada");
        u.setAccountStatus(AccountStatus.ACTIVE);
        repository.save(u);
        forget(u.getUsername());
        dispatch.refreshCourier(u.getId());
        audit(currentUser, "APROBAR_CUENTA", id, u.getUsername());
    }

    /** Rechazo de una solicitud: queda RECHAZADA (no se borra, RN-16). */
    public void reject(long id, String currentUser) {
        UserAccount u = search(id);
        if (u.getAccountStatus() != AccountStatus.PENDING) throw new IllegalArgumentException("Esa cuenta ya fue revisada");
        u.setAccountStatus(AccountStatus.REJECTED);
        repository.save(u);
        audit(currentUser, "RECHAZAR_CUENTA", id, u.getUsername());
    }

    /** Activar, desactivar o suspender (RF-M1-03, RF-M2-01). */
    public void changeAccountStatus(long id, AccountStatus status, String currentUser) {
        if (status != AccountStatus.ACTIVE && status != AccountStatus.INACTIVE && status != AccountStatus.SUSPENDED)
            throw new IllegalArgumentException("Estado de cuenta inválido");
        UserAccount u = search(id);
        if (u.getUsername().equals(currentUser)) throw new IllegalArgumentException("No puedes cambiar el estado de tu propia cuenta");
        if (u.getAccountStatus() == AccountStatus.PENDING || u.getAccountStatus() == AccountStatus.REJECTED)
            throw new IllegalArgumentException("Primero aprueba la solicitud");
        if (u.getRole() == Role.ADMIN && status != AccountStatus.ACTIVE
                && repository.findByRole(Role.ADMIN).stream().filter(UserAccount::isActive).count() <= 1)
            throw new IllegalArgumentException("Debe quedar al menos un administrador activo");
        if (u.getRole() == Role.COURIER && status != AccountStatus.ACTIVE) dispatch.validateNoDeliveries(id);
        u.setAccountStatus(status);
        if (status != AccountStatus.ACTIVE) u.login(null);      // cierra su sesión
        repository.save(u);
        forget(u.getUsername());
        if (u.getRole() == Role.COURIER) dispatch.refreshCourier(id);
        audit(currentUser, "ESTADO_CUENTA", id, status.name());
    }

    /** Restablece la contraseña con una clave temporal que el usuario debe cambiar al entrar. Devuelve la clave temporal. */
    public String resetPassword(long id, String currentUser) {
        UserAccount u = search(id);
        StringBuilder t = new StringBuilder();
        for (int i = 0; i < 8; i++) t.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        u.changePassword(encoder.encode(t.toString()), true);
        u.login(null);
        repository.save(u);
        forget(u.getUsername());
        audit(currentUser, "RESTABLECER_CLAVE", id, u.getUsername());
        return t.toString();
    }

    public void changePassword(String username, String current, String newPassword, String confirmation) {
        UserAccount u = byUsername(username).orElseThrow(() -> new IllegalArgumentException("Tu cuenta no existe"));
        if (!encoder.matches(current == null ? "" : current, u.getPassword())) throw new IllegalArgumentException("La contraseña actual no es correcta");
        if (!Objects.equals(newPassword, confirmation)) throw new IllegalArgumentException("Las contraseñas nuevas no coinciden");
        validatePassword(newPassword);
        if (encoder.matches(newPassword, u.getPassword())) throw new IllegalArgumentException("La nueva contraseña debe ser distinta");
        u.changePassword(encoder.encode(newPassword), false);
        repository.save(u);
        forget(username);
        audit(username, "CAMBIAR_CLAVE", u.getId(), null);
    }

    /** Datos del domiciliario editados por la central (RF-M2-01). */
    public void editCourier(long id, String name, String idNumber, String phone, String currentUser) {
        UserAccount u = search(id);
        if (u.getRole() != Role.COURIER) throw new IllegalArgumentException("Esa cuenta no es de un domiciliario");
        String n = name == null ? "" : name.trim();
        if (n.length() < 2 || n.length() > 80) throw new IllegalArgumentException("El nombre debe tener entre 2 y 80 letras");
        if (idNumber != null && !idNumber.isBlank() && !DOCUMENT_PATTERN.matcher(idNumber.trim()).matches()) throw new IllegalArgumentException("Documento inválido");
        if (phone != null && !phone.isBlank() && !PHONE_PATTERN.matcher(phone.trim()).matches()) throw new IllegalArgumentException("Teléfono inválido");
        u.setName(n);
        repository.save(u);
        dispatch.registerCourier(id, n, u.getUsername(), white(idNumber), white(phone));
        audit(currentUser, "EDITAR_DOMICILIARIO", id, null);
    }

    // ---------------- Sesión única (RF-M1-04) ----------------

    /** Al entrar: nueva sesión. Para los domiciliarios, la sesión anterior deja de servir. */
    public String newSession(String username) {
        UserAccount u = byUsername(username).orElseThrow();
        String sid = UUID.randomUUID().toString();
        u.login(sid);
        repository.save(u);
        forget(username);
        return sid;
    }

    /** ¿Sigue siendo válida esta sesión? (cuenta activa y, si es domiciliario, que sea su última sesión) */
    public boolean validSession(String username, String sid) {
        Access a = access(username);
        if (!a.active()) return false;
        return a.role() != Role.COURIER || Objects.equals(a.session(), sid);
    }

    public Access access(String username) {
        String u = normalize(username);
        Access a = accesses.get(u);
        if (a != null && a.expires() > System.currentTimeMillis()) return a;
        UserAccount x = repository.findByUsername(u).orElse(null);
        a = x == null ? new Access(null, null, null, false, null, false, false, System.currentTimeMillis() + 30_000)
                : new Access(x.getId(), x.getName(), x.getRole(), x.isActive(), x.getCurrentSession(), x.isMustChangePassword(),
                             mustAcceptPolicy(x), System.currentTimeMillis() + 30_000);
        accesses.put(u, a);
        return a;
    }

    private void forget(String username) { if (username != null) accesses.remove(normalize(username)); }

    public void logout(String username) {
        byUsername(username).ifPresent(u -> { u.login(null); repository.save(u); });
        forget(username);
    }

    // ---------------- Política de datos (RF-M1-06, RF-M6-01) ----------------

    public Optional<DataPolicy> currentPolicy() { return policies.findFirstByCurrentTrueOrderByIdDesc(); }

    public boolean mustAcceptPolicy(UserAccount u) {
        Optional<DataPolicy> p = currentPolicy();
        return p.isPresent() && !acceptances.existsByUserIdAndPolicyId(u.getId(), p.get().getId());
    }

    public void acceptPolicy(String username, String ip) {
        UserAccount u = byUsername(username).orElseThrow(() -> new IllegalArgumentException("Tu cuenta no existe"));
        DataPolicy p = currentPolicy().orElseThrow(() -> new IllegalArgumentException("No hay política publicada"));
        if (!acceptances.existsByUserIdAndPolicyId(u.getId(), p.getId()))
            acceptances.save(new PolicyAcceptance(u.getId(), p.getId(), p.getVersion(), ip == null || ip.length() > 45 ? null : ip));
        forget(username);
    }

    /** El administrador publica una nueva versión: todos deben aceptarla de nuevo. */
    public DataPolicy publishPolicy(String version, String text, String currentUser) {
        String v = version == null ? "" : version.trim();
        String t = text == null ? "" : text.trim();
        if (v.isEmpty() || v.length() > 10) throw new IllegalArgumentException("Escribe la versión (máx. 10 caracteres)");
        if (t.length() < 50) throw new IllegalArgumentException("El texto de la política es muy corto");
        for (DataPolicy p : policies.findByCurrentTrue()) { p.withdraw(); policies.save(p); }
        DataPolicy newPolicy = policies.save(new DataPolicy(v, t));
        accesses.clear();
        audit(currentUser, "PUBLICAR_POLITICA", newPolicy.getId(), v);
        return newPolicy;
    }

    // ---------------- Consultas ----------------

    public Optional<UserAccount> byUsername(String username) { return repository.findByUsername(normalize(username)); }
    public Optional<UserAccount> byId(long id) { return repository.findById(id); }
    public List<UserAccount> all() { return repository.findAllByOrderByIdAsc(); }
    public int countPending() { return (int) repository.countByAccountStatus(AccountStatus.PENDING); }
    public boolean hasRole(Role role) { return repository.existsByRole(role); }

    /** Al arrancar: mete a la operación a todos los domiciliarios (los no activos no reciben servicios). */
    public void loadCouriers() {
        for (UserAccount u : repository.findByRole(Role.COURIER))
            dispatch.registerCourier(u.getId(), u.getName(), u.getUsername(), null, null);
    }

    private UserAccount search(long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe esa cuenta"));
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8) throw new IllegalArgumentException("La contraseña debe tener mínimo 8 caracteres");
        if (!password.matches(".*[0-9].*") || !password.matches(".*[A-Za-z].*")) throw new IllegalArgumentException("La contraseña debe tener letras y números");
    }

    private void audit(String currentUser, String action, Long id, String detail) {
        audit.save(new AuditEntry(clock.now(), currentUser == null ? "sistema" : currentUser, action, "USUARIO", id, detail, false));
    }

    private static String white(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static String normalize(String username) { return username == null ? "" : username.trim().toLowerCase(); }
}
