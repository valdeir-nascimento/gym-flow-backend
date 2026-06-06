package br.com.gym.flow.shared.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Documentation-only schema describing the error envelope returned by the global
 * exception handler — an RFC 7807 {@code application/problem+json} body. Not a real
 * DTO; referenced from {@code @ApiResponse} so every endpoint documents the same
 * shape. Mirrors {@code br.com.gym.flow.api.error.ProblemDetails}.
 */
@Schema(name = "ApiError", description = "Envelope de erro (RFC 7807, application/problem+json).")
public record ApiErrorResponse(

    @Schema(description = "URI que identifica o tipo do erro.",
        example = "https://wsfitness/errors/validation-failed")
    String type,

    @Schema(description = "Título legível do problema.", example = "Operação não pôde ser concluída")
    String title,

    @Schema(description = "Código HTTP.", example = "422")
    int status,

    @Schema(description = "Descrição legível do problema.", example = "Há erros de validação")
    String detail,

    @Schema(description = "Erros de domínio/validação que causaram a falha (quando aplicável).")
    List<FieldError> errors
) {

    @Schema(name = "ApiFieldError", description = "Um erro individual de domínio ou de validação de campo.")
    public record FieldError(

        @Schema(description = "Campo relacionado (vazio para erros não atrelados a um campo).", example = "weightKg")
        String field,

        @Schema(description = "Código estável em UPPER_SNAKE_CASE, para o client decidir.",
            example = "ANAMNESIS_IMPLAUSIBLE_WEIGHT")
        String code,

        @Schema(description = "Mensagem legível em português.", example = "peso fora da faixa plausível")
        String message
    ) {}
}
