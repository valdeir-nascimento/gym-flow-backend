package br.com.gym.flow.evolution.presentation;

import br.com.gym.flow.evolution.domain.Granularity;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.openapi.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.util.UUID;

/**
 * OpenAPI documentation for the student-evolution report (RF-008). Kept separate
 * from {@link EvolutionController}; error bodies are RFC 7807 ({@link ApiErrorResponse}).
 */
@Tag(name = "Evolution", description = "Indicadores e séries temporais de evolução do aluno (RF-008).")
interface EvolutionApi {

    String PROBLEM_JSON = "application/problem+json";

    String EVOLUTION_EXAMPLE = """
        {
          "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
          "granularity": "WEEKLY",
          "from": "2026-01-01T00:00:00Z",
          "to": "2026-06-30T23:59:59Z",
          "frequency": [ { "bucket": "2026-W23", "workouts": 3 }, { "bucket": "2026-W24", "workouts": 4 } ],
          "volume": [ { "bucket": "2026-W23", "totalVolume": 5200.00 }, { "bucket": "2026-W24", "totalVolume": 6100.00 } ],
          "oneRepMaxByExercise": [
            { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1",
              "points": [ { "bucket": "2026-W23", "estimatedOneRepMax": 92.50 }, { "bucket": "2026-W24", "estimatedOneRepMax": 95.00 } ] }
          ],
          "bodyweight": [ { "bucket": "2026-W23", "weightKg": 82.50 }, { "bucket": "2026-W24", "weightKg": 81.80 } ]
        }""";

    @Operation(summary = "Relatório de evolução",
        description = "Indicadores e séries temporais (volume, frequência, cargas) de um aluno num período, "
            + "agregados pela granularidade escolhida. O aluno vê os seus; o professor vê os de seus alunos; "
            + "Administrador vê todos. Período máximo: 24 meses.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relatório de evolução",
            content = @Content(schema = @Schema(implementation = EvolutionReport.class),
                examples = @ExampleObject(value = EVOLUTION_EXAMPLE))),
        @ApiResponse(responseCode = "403", description = "Evolução pertence a outro aluno / ator sem permissão",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Período de consulta maior que 24 meses",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 422, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "EVOLUTION_PERIOD_TOO_LONG", "message": "período máximo de consulta é de 24 meses" } ]
                    }""")))
    })
    Result<EvolutionReport> getEvolution(
        @Parameter(description = "Id do aluno.", required = true) UUID studentId,
        @Parameter(description = "Granularidade da agregação.", example = "WEEKLY") Granularity granularity,
        @Parameter(description = "Início do período (ISO-8601 UTC). Default: 6 meses atrás.",
            example = "2026-01-01T00:00:00Z") Instant from,
        @Parameter(description = "Fim do período (ISO-8601 UTC). Default: agora.",
            example = "2026-06-30T23:59:59Z") Instant to,
        @Parameter(description = "Id do ator autenticado (header).", required = true) UUID actorId,
        @Parameter(description = "Perfil do ator (header).", required = true, example = "STUDENT") String actorRole);
}
