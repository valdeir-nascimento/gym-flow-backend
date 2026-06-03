package br.com.gym.flow.api.security;

import br.com.gym.flow.authentication.domain.spi.UserCredentialsLookupPort;
import br.com.gym.flow.authentication.domain.spi.UserCredentialsView;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class AppUserDetailsService implements UserDetailsService {

    private final UserCredentialsLookupPort lookup;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserCredentialsView v = lookup.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        return new AppUserDetails(v.userId(), v.email(), v.passwordHash(), v.role(), v.status());
    }
}
