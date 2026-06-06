package br.com.gym.flow.coaching.presentation;

import br.com.gym.flow.coaching.domain.spi.ManagedStudentView;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * OpenAPI documentation for the instructor's coaching view of managed students
 * (RF-010). Kept separate from {@link InstructorController}; error bodies are
 * RFC 7807 ({@link ApiErrorResponse}).
 */
@Tag(name = "Coaching", description = "Visão do professor sobre seus alunos e a evolução de cada um (RF-010).")
interface InstructorApi {

    String PROBLEM_JSON = "application/problem+json";
    String INSTRUCTOR_ID = "Id do professor autenticado (header).";
    String ACTOR_ROLE = "Perfil do ator (header).";

    String STUDENTS_PAGE = """
        {
          "content": [
            {
              "studentId": "a1f0c3d4-1111-2222-3333-444455556666", "name": "Maria Silva",
              "status": "ACTIVE", "lastExecutionAt": "2026-06-04T08:25:00Z", "totalExecutions": 27
            }
          ],
          "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
          "first": true, "last": true, "numberOfElements": 1, "empty": false
        }""";

    String EVOLUTION_EXAMPLE = """
        {
          "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
          "granularity": "WEEKLY", "from": "2026-01-01T00:00:00Z", "to": "2026-06-30T23:59:59Z",
          "frequency": [ { "bucket": "2026-W24", "workouts": 4 } ],
          "volume": [ { "bucket": "2026-W24", "totalVolume": 6100.00 } ],
          "oneRepMaxByExercise": [ { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1", "points": [ { "bucket": "2026-W24", "estimatedOneRepMax": 95.00 } ] } ],
          "bodyweight": [ { "bucket": "2026-W24", "weightKg": 81.80 } ]
        }""";

    @Operation(summary = "Listar meus alunos",
        description = "Alunos ativamente vinculados ao professor autenticado, paginados (RF-010).")
    @ApiResponse(responseCode = "200", description = "Página de alunos gerenciados",
        content = @Content(schema = @Schema(implementation = Page.class), examples = @ExampleObject(value = STUDENTS_PAGE)))
    Result<Page<ManagedStudentView>> list(
        @Parameter(description = "Página (0-based).", example = "0") @Min(0) int page,
        @Parameter(description = "Tamanho da página (1–100).", example = "20") @Min(1) @Max(100) int size,
        @Parameter(description = INSTRUCTOR_ID, required = true) UUID instructorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "INSTRUCTOR") String actorRole);

    @Operation(summary = "Evolução de um aluno meu",
        description = "Relatório de evolução de um aluno vinculado ao professor (RF-010/RF-008).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Relatório de evolução do aluno",
            content = @Content(schema = @Schema(implementation = EvolutionReport.class),
                examples = @ExampleObject(value = EVOLUTION_EXAMPLE))),
        @ApiResponse(responseCode = "403", description = "Aluno não vinculado ao professor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 403, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "STUDENT_NOT_MANAGED", "message": "aluno não vinculado ao professor" } ]
                    }""")))
    })
    Result<EvolutionReport> getOne(
        @Parameter(description = "Id do aluno.") UUID studentId,
        @Parameter(description = INSTRUCTOR_ID, required = true) UUID instructorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "INSTRUCTOR") String actorRole);
}
