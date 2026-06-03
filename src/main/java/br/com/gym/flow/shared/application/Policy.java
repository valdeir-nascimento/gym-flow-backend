package br.com.gym.flow.shared.application;

import br.com.gym.flow.shared.domain.Result;

/**
 * A pure business rule applied to a candidate. Returns the candidate on
 * success or a {@link Result.Failure} carrying the violation. Stateless
 * by convention; any collaborator a policy needs is injected explicitly,
 * never derived from ambient state.
 *
 * <p>Composable with {@link Result#flatMap(java.util.function.Function)}:
 * {@code result.flatMap(policy::enforce)}.
 *
 * @param <T> the candidate type the policy decides upon
 */
public interface Policy<T> {
    Result<T> enforce(T candidate);
}
