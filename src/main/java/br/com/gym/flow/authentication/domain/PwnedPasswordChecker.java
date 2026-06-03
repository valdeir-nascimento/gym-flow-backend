package br.com.gym.flow.authentication.domain;

public interface PwnedPasswordChecker {
    boolean isPwned(String rawPassword);
}
