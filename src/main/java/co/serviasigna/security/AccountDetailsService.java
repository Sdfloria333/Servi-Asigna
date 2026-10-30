package co.serviasigna.security;

import co.serviasigna.domain.AccountStatus;
import co.serviasigna.users.UserAccountRepository;
import co.serviasigna.users.UserAccount;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Le explica a Spring Security cómo buscar a alguien por su usuario cuando intenta entrar. */
@Service
public class AccountDetailsService implements UserDetailsService {
    private final UserAccountRepository repository;

    public AccountDetailsService(UserAccountRepository repository) { this.repository = repository; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount u = repository.findByUsername(username.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No existe " + username));
        return User.withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getRole().name())                                        // ROLE_ADMIN, ROLE_DESPACHADOR o ROLE_DOMICILIARIO
                .disabled(u.getAccountStatus() == AccountStatus.PENDING || u.getAccountStatus() == AccountStatus.REJECTED)
                .accountLocked(u.getAccountStatus() == AccountStatus.SUSPENDED || u.getAccountStatus() == AccountStatus.INACTIVE)
                .build();
    }
}
