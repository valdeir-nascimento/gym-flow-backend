package br.com.gym.flow.history.presentation;

import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.openapi.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

/**
 * OpenAPI documentation for registering a workout execution (RF-007). Kept separate
 * from {@link WorkoutExecutionController}; error bodies are RFC 7807 ({@link ApiErrorResponse}).
 */
@Tag(name = "Workout Executions", description = "Registro de execuções de treino pelo aluno (RF-007).")
interface WorkoutExecutionApi {

    String PROBLEM_JSON = "application/problem+json";

    String EXECUTION_EXAMPLE = """
        {
          "id": "90000000-0000-0000-0000-000000000099",
          "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
          "trainingId": "70000000-0000-0000-0000-000000000010",
          "startedAt": "2026-06-04T07:30:00Z",
          "finishedAt": "2026-06-04T08:25:00Z",
          "notes": "Boa sessão; aumentei a carga no supino.",
          "items": [
            { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1", "sets": 4, "repetitions": 10, "load": 42.50, "notes": "última série até a falha" }
          ],
          "registeredAt": "2026-06-04T08:26:10Z"
        }""";

    @Operation(summary = "Registrar execução de treino",
        description = "O aluno registra a realização de um treino (RF-007): início/fim, observações e as séries "
            + "executadas por exercício. Datas não podem ser futuras; o treino deve pertencer ao aluno.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Execução registrada",
            content = @Content(schema = @Schema(implementation = WorkoutExecutionView.class),
                examples = @ExampleObject(value = EXECUTION_EXAMPLE))),
        @ApiResponse(responseCode = "400", description = "Corpo inválido ou data futura",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Data futura", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 400, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "EXECUTION_FUTURE_DATETIME", "message": "data/hora da execução não pode ser futura" } ]
                    }"""))),
        @ApiResponse(responseCode = "403", description = "Treino não pertence ao aluno",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Treino inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Execução já registrada para o mesmo período",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Fim antes do início ou treino inativado fora da janela",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<WorkoutExecutionView> register(
        @Parameter(description = "Id do treino executado (path).") UUID trainingId,
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(name = "Execução", value = """
            {
              "startedAt": "2026-06-04T07:30:00Z",
              "finishedAt": "2026-06-04T08:25:00Z",
              "notes": "Boa sessão; aumentei a carga no supino.",
              "items": [
                { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1", "sets": 4, "repetitions": 10, "load": 42.50, "notes": "última série até a falha" }
              ]
            }""")))
        RegisterWorkoutExecutionRequest req,
        @Parameter(description = "Id do aluno autenticado (header).", required = true) UUID actorId,
        @Parameter(hidden = true) HttpServletResponse response);
}
