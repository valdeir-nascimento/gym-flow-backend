package br.com.gym.flow.shared.application;

import br.com.gym.flow.shared.domain.Result;

/**
 * Side-effect-free use case. Receives a query and returns a {@link Result}
 * with either the projected view or a notification (e.g. {@code NOT_FOUND}).
 * Queries never mutate persistent state.
 *
 * @param <Q> query input type (immutable record)
 * @param <R> view returned on success
 */
public interface QueryUseCase<Q, R> {
    Result<R> execute(Q query);
}
