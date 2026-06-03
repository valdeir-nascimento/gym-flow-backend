package br.com.gym.flow.shared.application;

import br.com.gym.flow.shared.domain.Result;

/**
 * State-mutating use case. Receives a command (intention) and returns a
 * {@link Result} carrying either the produced view or a domain notification.
 * The "command" name follows CQS (Bertrand Meyer): commands change state,
 * queries do not.
 *
 * @param <C> command input type (immutable record)
 * @param <R> view returned on success
 */
public interface CommandUseCase<C, R> {
    Result<R> execute(C command);
}
