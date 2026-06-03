package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.DomainException;
import br.com.gym.flow.shared.domain.ErrorCategory;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.NotificationError;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex) {
        log.info("domain rule violated: code={} message={}", ex.code(), ex.getMessage());
        ProblemDetail pd = ProblemDetails.fromDomainException(ex);
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex) {
        log.info("not found: {}", ex.notification().errors());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        pd.setStatus(404);
        return ResponseEntity.status(404).body(pd);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorized(UnauthorizedException ex) {
        log.info("unauthorized: {}", ex.notification().errors());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        pd.setStatus(401);
        return ResponseEntity.status(401).body(pd);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.warn("upstream unavailable: {}", ex.notification().errors(), ex.getCause());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        pd.setStatus(503);
        return ResponseEntity.status(503).body(pd);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex) {
        log.info("conflict: {}", ex.notification().errors());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        pd.setStatus(409);
        return ResponseEntity.status(409).body(pd);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.info("optimistic lock conflict: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Conflito de concorrência");
        pd.setDetail("O registro foi modificado por outra operação. Recarregue e tente novamente.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        HttpStatus status = ex.notification().errors().stream()
            .findFirst()
            .map(NotificationError::code)
            .map(ErrorCode::valueOf)
            .map(ErrorCode::category)
            .map(ErrorCategoryHttpStatus::of)
            .orElse(HttpStatus.UNPROCESSABLE_ENTITY);
        log.info("business failure (status={}): {}", status.value(), ex.notification().errors());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        pd.setStatus(status.value());
        return ResponseEntity.status(status).body(pd);
    }

    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ProblemDetail> handleNotification(NotificationException ex) {
        log.info("notification errors: {}", ex.notification().errors());
        ProblemDetail pd = ProblemDetails.fromNotification(ex.notification());
        return ResponseEntity.unprocessableEntity().body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
            .<Map<String, Object>>map(fe -> {
                String code = fe.getCode() == null ? "INVALID" : fe.getCode().toUpperCase();
                return Map.of(
                    "field", fe.getField(),
                    "code", code,
                    "message", ValidationMessages.humanize(fe.getField(), code, fe.getArguments(), fe.getDefaultMessage()));
            })
            .toList();
        log.info("bean validation failed: {}", errors);
        return ResponseEntity.badRequest().body(ProblemDetails.validation(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> errors = ex.getConstraintViolations().stream()
            .<Map<String, Object>>map(v -> {
                String field = lastSegment(v.getPropertyPath().toString());
                String code = v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName().toUpperCase();
                return Map.of(
                    "field", field,
                    "code", code,
                    "message", ValidationMessages.humanize(field, code, null, v.getMessage()));
            })
            .toList();
        log.info("constraint violation: {}", errors);
        return ResponseEntity.badRequest().body(ProblemDetails.validation(errors));
    }

    private static String lastSegment(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> error = Map.of(
            "field", ex.getName(),
            "code", "TYPE_MISMATCH",
            "message", "valor inválido para o parâmetro");
        log.info("type mismatch on {}: {}", ex.getName(), ex.getValue());
        return ResponseEntity.badRequest().body(ProblemDetails.validation(List.of(error)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException ex) {
        log.info("body not readable: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Corpo da requisição inválido");
        pd.setDetail("JSON malformado ou ausente");
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("unexpected error", ex);
        return ResponseEntity.internalServerError().body(ProblemDetails.unexpected());
    }
}
