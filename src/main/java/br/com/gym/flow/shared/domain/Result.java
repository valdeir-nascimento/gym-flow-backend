package br.com.gym.flow.shared.domain;

import java.util.Optional;
import java.util.function.Function;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {
    }

    record Failure<T>(Notification notification) implements Result<T> {
    }

    @SuppressWarnings("rawtypes")
    Success VOID_SUCCESS = new Success<>(null);

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(Notification notification) {
        return new Failure<>(notification);
    }

    static <T> Result<T> failWith(ErrorCode code) {
        return failure(Notification.ofSingle(code));
    }

    static <T> Result<T> failWith(ErrorCode code, String message) {
        return failure(Notification.ofSingle(code, message));
    }

    static <T> Result<T> failWith(String code, String message) {
        return failure(Notification.ofSingle(code, message));
    }

    @SuppressWarnings("unchecked")
    static Result<Void> ok() {
        return VOID_SUCCESS;
    }

    static <T> Result<T> ofOptional(Optional<T> opt, ErrorCode code) {
        return opt.<Result<T>>map(Result::success).orElseGet(() -> failWith(code));
    }

    static <T> Result<T> ofOptional(Optional<T> opt, ErrorCode code, String message) {
        return opt.<Result<T>>map(Result::success).orElseGet(() -> failWith(code, message));
    }

    default boolean isSuccess() {
        return this instanceof Success<T>;
    }

    default T getOrThrow() {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw new IllegalStateException(
                "Result is a Failure: " + f.notification().errors());
        };
    }

    default <X extends RuntimeException> T orElseThrow(Function<Notification, X> exceptionFactory) {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw exceptionFactory.apply(f.notification());
        };
    }

    default <R> Result<R> map(Function<? super T, ? extends R> fn) {
        return switch (this) {
            case Success<T> s -> Result.success(fn.apply(s.value()));
            case Failure<T> f -> Result.failure(f.notification());
        };
    }

    default <R> Result<R> flatMap(Function<? super T, Result<R>> fn) {
        return switch (this) {
            case Success<T> s -> fn.apply(s.value());
            case Failure<T> f -> Result.failure(f.notification());
        };
    }
}
