package br.com.gym.flow.history.presentation;

import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.openapi.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.UUID;

/**
 * OpenAPI documentation for querying workout-execution history (RF-009). Kept
 * separate from {@link WorkoutExecutionQueryController}; error bodies are RFC 7807
 * ({@link ApiErrorResponse}).
 */
@Tag(name = "Workout Executions", description = "Consulta do histórico de execuções de treino (RF-009).")
interface WorkoutExecutionQueryApi {

    String PROBLEM_JSON = "application/problem+json";

    String EXECUTION_EXAMPLE = """
        {
          "id": "90000000-0000-0000-0000-000000000099",
          "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
          "trainingId": "70000000-0000-0000-0000-000000000010",
          "startedAt": "2026-06-04T07:30:00Z", "finishedAt": "2026-06-04T08:25:00Z",
          "notes": "Boa sessão.",
          "items": [ { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1", "sets": 4, "repetitions": 10, "load": 42.50, "notes": null } ],
          "registeredAt": "2026-06-04T08:26:10Z"
        }""";

    String EXECUTION_PAGE = """
        {
          "content": [
            {
              "id": "90000000-0000-0000-0000-000000000099",
              "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
              "trainingId": "70000000-0000-0000-0000-000000000010",
              "startedAt": "2026-06-04T07:30:00Z", "finishedAt": "2026-06-04T08:25:00Z", "notes": "Boa sessão.",
              "items": [ { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1", "sets": 4, "repetitions": 10, "load": 42.50, "notes": null } ],
              "registeredAt": "2026-06-04T08:26:10Z"
            }
          ],
          "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
          "first": true, "last": true, "numberOfElements": 1, "empty": false
        }""";

    String ACTOR_ID = "Id do ator autenticado (header).";
    String ACTOR_ROLE = "Perfil do ator (header). Administrador consulta qualquer aluno.";

    @Operation(summary = "Listar execuções",
        description = "Histórico paginado de execuções de um aluno, com filtros por treino, exercício e janela de tempo "
            + "(RF-009). O aluno só consulta a si próprio; Administrador consulta qualquer um.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de execuções",
            content = @Content(schema = @Schema(implementation = Page.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = EXECUTION_PAGE))),
        @ApiResponse(responseCode = "400", description = "Parâmetro de paginação fora do limite (size > 100)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Histórico pertence a outro aluno",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 403, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "EXECUTION_NOT_OWNED", "message": "histórico pertence a outro aluno" } ]
                    }""")))
    })
    Result<Page<WorkoutExecutionView>> list(
        @Parameter(description = "Id do aluno.", required = true) UUID studentId,
        @Parameter(description = "Filtra por treino.") UUID trainingId,
        @Parameter(description = "Filtra por exercício.") UUID exerciseId,
        @Parameter(description = "Início da janela (ISO-8601 UTC).", example = "2026-05-01T00:00:00Z") Instant startedFrom,
        @Parameter(description = "Fim da janela (ISO-8601 UTC).", example = "2026-06-30T23:59:59Z") Instant startedTo,
        @Parameter(description = "Página (0-based).", example = "0") @Min(0) int page,
        @Parameter(description = "Tamanho da página (1–100).", example = "20") @Min(1) @Max(100) int size,
        @Parameter(description = ACTOR_ID, required = true) UUID actorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "STUDENT") String actorRole);

    @Operation(summary = "Obter execução por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Execução",
            content = @Content(schema = @Schema(implementation = WorkoutExecutionView.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = EXECUTION_EXAMPLE))),
        @ApiResponse(responseCode = "403", description = "Execução de outro aluno",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Execução inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<WorkoutExecutionView> getById(
        @Parameter(description = "Id da execução.") UUID id,
        @Parameter(description = ACTOR_ID, required = true) UUID actorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "STUDENT") String actorRole);
}
