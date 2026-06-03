package br.com.gym.flow.api.support;

import br.com.gym.flow.api.error.ProblemDetails;
import br.com.gym.flow.shared.domain.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class ApiResponses {

    public static final MediaType PROBLEM_JSON =
        MediaType.parseMediaType("application/problem+json");

    private ApiResponses() {
    }

    public static ResponseEntity<ProblemDetail> unprocessable(Result.Failure<?> f) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, f);
    }

    public static ResponseEntity<ProblemDetail> notFound(Result.Failure<?> f) {
        return problem(HttpStatus.NOT_FOUND, f);
    }

    public static ResponseEntity<ProblemDetail> unauthorized(Result.Failure<?> f) {
        return problem(HttpStatus.UNAUTHORIZED, f);
    }

    public static ResponseEntity<ProblemDetail> problem(HttpStatus status, Result.Failure<?> f) {
        return ResponseEntity.status(status)
            .contentType(PROBLEM_JSON)
            .body(ProblemDetails.fromNotification(f.notification()));
    }
}
