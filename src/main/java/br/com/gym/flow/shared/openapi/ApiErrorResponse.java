package br.com.gym.flow.shared.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Documentation-only schema describing the standard error envelope returned by
 * the global exception handler. Not used as a real DTO — kept here so every
 * endpoint can reference the same shape via `@Content(schema = @Schema(...))`.
 */
@Schema(description = "Envelope padrão de erro retornado pelo backend.", name = "ApiError")
public record ApiErrorResponse(

    @Schema(
        description = "Código de erro estável, em UPPER_SNAKE_CASE. Use no client para tomar decisões.",
        example = "INVALID_CREDENTIALS")
    String code,

    @Schema(
        description = "Mensagem legível em português, pronta para exibição ao usuário final.",
        example = "E-mail ou senha inválidos.")
    String message,

    @Schema(
        description = "Detalhes adicionais opcionais (ex: campos inválidos em uma validação).",
        example = "{\"email\": \"deve ser um e-mail válido\"}",
        nullable = true)
    Map<String, Object> details
) {}
