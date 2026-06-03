package br.com.gym.flow.authentication.infrastructure;

import br.com.gym.flow.authentication.domain.PwnedPasswordChecker;
import org.springframework.stereotype.Component;

@Component
class NoopPwnedPasswordChecker implements PwnedPasswordChecker {

    @Override
    public boolean isPwned(String rawPassword) {
        return false;
    }
}
