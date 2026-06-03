package br.com.gym.flow.api.error;

import br.com.gym.flow.shared.domain.DomainException;
import br.com.gym.flow.shared.domain.Notification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.List;
import java.util.Map;

public final class ProblemDetails {

    private static final String TYPE_BASE = "https://cardapio/errors/";

    private ProblemDetails() {
    }

    public static ProblemDetail fromDomainException(DomainException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setType(URI.create(TYPE_BASE + ex.code().toLowerCase().replace('_', '-')));
        pd.setTitle("Regra de negócio violada");
        pd.setDetail(ex.getMessage());
        pd.setProperty("code", ex.code());
        return pd;
    }

    public static ProblemDetail fromNotification(Notification notification) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setType(URI.create(TYPE_BASE + "validation-failed"));
        pd.setTitle("Operação não pôde ser concluída");
        pd.setDetail("Há erros de validação");
        List<Map<String, Object>> errors = notification.errors().stream()
            .<Map<String, Object>>map(e -> Map.of(
                "field", e.field() == null ? "" : e.field(),
                "code", e.code(),
                "message", e.message()))
            .toList();
        pd.setProperty("errors", errors);
        return pd;
    }

    public static ProblemDetail validation(List<Map<String, Object>> errors) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(TYPE_BASE + "validation-failed"));
        pd.setTitle("Campos inválidos");
        pd.setDetail("Um ou mais campos da requisição não passaram na validação");
        pd.setProperty("errors", errors);
        return pd;
    }

    public static ProblemDetail unexpected() {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create(TYPE_BASE + "internal"));
        pd.setTitle("Erro interno");
        pd.setDetail("Algo deu errado processando sua requisição");
        return pd;
    }
}
