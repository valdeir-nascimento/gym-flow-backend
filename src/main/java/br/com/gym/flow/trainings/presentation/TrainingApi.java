package br.com.gym.flow.trainings.presentation;

import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.openapi.ApiErrorResponse;
import br.com.gym.flow.trainings.domain.spi.TrainingView;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * OpenAPI documentation for the training-prescription endpoints (RF-004, RF-005,
 * RF-006). Kept separate from {@link TrainingController}; error bodies are RFC 7807
 * ({@link ApiErrorResponse}). O ator vem dos headers {@code X-User-Id}/{@code X-User-Role}.
 */
@Tag(name = "Trainings", description = "Prescrição de treinos: criação, atualização e consulta.")
interface TrainingApi {

    String PROBLEM_JSON = "application/problem+json";
    String ACTOR_ID = "Id do ator autenticado (header).";
    String ACTOR_ROLE = "Perfil do ator (header).";

    String TRAINING_EXAMPLE = """
        {
          "id": "70000000-0000-0000-0000-000000000010",
          "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
          "instructorId": "22222222-2222-2222-2222-222222222222",
          "name": "Treino A — Peito e Tríceps",
          "objective": "Hipertrofia",
          "startDate": "2026-06-01",
          "endDate": "2026-08-31",
          "status": "ACTIVE",
          "items": [
            { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1", "sets": 4, "repetitions": 10, "load": 40.00, "restSeconds": 60 }
          ],
          "createdAt": "2026-06-01T12:00:00Z",
          "updatedAt": "2026-06-01T12:00:00Z"
        }""";

    String TRAINING_PAGE = """
        {
          "content": [
            {
              "id": "70000000-0000-0000-0000-000000000010",
              "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
              "instructorId": "22222222-2222-2222-2222-222222222222",
              "name": "Treino A — Peito e Tríceps", "objective": "Hipertrofia",
              "startDate": "2026-06-01", "endDate": "2026-08-31", "status": "ACTIVE",
              "items": [ { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1", "sets": 4, "repetitions": 10, "load": 40.00, "restSeconds": 60 } ],
              "createdAt": "2026-06-01T12:00:00Z", "updatedAt": "2026-06-01T12:00:00Z"
            }
          ],
          "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
          "first": true, "last": true, "numberOfElements": 1, "empty": false
        }""";

    @Operation(summary = "Criar treino",
        description = "Prescreve um treino para o aluno (RF-004). O professor deve estar vinculado ao aluno; "
            + "exercícios devem existir e estar ativos; exercícios contraindicados na anamnese (RF-017) são rejeitados; "
            + "não pode haver outro treino ativo com vigência sobreposta.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Treino criado",
            content = @Content(schema = @Schema(implementation = TrainingView.class),
                examples = @ExampleObject(value = TRAINING_EXAMPLE))),
        @ApiResponse(responseCode = "400", description = "Corpo inválido ou exercício inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Professor não vinculado ao aluno",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 403, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "TRAINING_INSTRUCTOR_NOT_LINKED", "message": "professor não vinculado ao aluno" } ]
                    }"""))),
        @ApiResponse(responseCode = "422", description = "Aluno inativo, exercício inativo/contraindicado ou vigência sobreposta",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Exercício contraindicado", value = """
                        {
                          "type": "https://wsfitness/errors/validation-failed",
                          "title": "Operação não pôde ser concluída", "status": 422, "detail": "Há erros de validação",
                          "errors": [ { "field": "", "code": "TRAINING_CONTRAINDICATED_EXERCISE", "message": "exercício contraindicado para o aluno não pode compor o treino" } ]
                        }"""),
                    @ExampleObject(name = "Vigência sobreposta", value = """
                        {
                          "type": "https://wsfitness/errors/validation-failed",
                          "title": "Operação não pôde ser concluída", "status": 422, "detail": "Há erros de validação",
                          "errors": [ { "field": "", "code": "TRAINING_OVERLAPPING_PERIOD", "message": "já existe treino ativo com vigência sobreposta" } ]
                        }""")
                }))
    })
    Result<TrainingView> create(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(name = "Novo treino", value = """
            {
              "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
              "name": "Treino A — Peito e Tríceps",
              "objective": "Hipertrofia",
              "startDate": "2026-06-01",
              "endDate": "2026-08-31",
              "items": [
                { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1", "sets": 4, "repetitions": 10, "load": 40.00, "restSeconds": 60 }
              ]
            }""")))
        CreateTrainingRequest req,
        @Parameter(description = ACTOR_ID, required = true) UUID instructorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "INSTRUCTOR") String actorRole,
        @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "Atualizar treino",
        description = "Atualiza um treino (RF-005). Só o professor responsável ou Administrador. Treino ARQUIVADO não "
            + "pode ser editado (409). As mesmas regras de exercício ativo/contraindicado se aplicam.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Treino atualizado",
            content = @Content(schema = @Schema(implementation = TrainingView.class),
                examples = @ExampleObject(value = TRAINING_EXAMPLE))),
        @ApiResponse(responseCode = "403", description = "Treino não pertence ao professor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Treino inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Treino arquivado não pode ser editado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 409, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "TRAINING_ARCHIVED", "message": "treino arquivado não pode ser editado" } ]
                    }"""))),
        @ApiResponse(responseCode = "422", description = "Exercício inativo/contraindicado ou vigência sobreposta",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<TrainingView> update(
        @Parameter(description = "Id do treino.") UUID id,
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            {
              "name": "Treino A — Peito e Tríceps",
              "objective": "Hipertrofia + resistência",
              "startDate": "2026-06-01",
              "endDate": "2026-09-30",
              "items": [
                { "exerciseId": "e1e1e1e1-0000-0000-0000-0000000000e1", "sets": 4, "repetitions": 12, "load": 42.50, "restSeconds": 60 }
              ],
              "status": "ACTIVE"
            }""")))
        UpdateTrainingRequest req,
        @Parameter(description = ACTOR_ID, required = true) UUID actorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "INSTRUCTOR") String actorRole);

    @Operation(summary = "Listar treinos do aluno",
        description = "Treinos de um aluno (RF-006). O aluno vê os seus; o professor vê os de seus alunos; Administrador vê todos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de treinos",
            content = @Content(schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(value = TRAINING_PAGE))),
        @ApiResponse(responseCode = "403", description = "Sem permissão sobre os treinos desse aluno",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<Page<TrainingView>> listByStudent(
        @Parameter(description = "Id do aluno.", required = true) UUID studentId,
        @Parameter(description = ACTOR_ID, required = true) UUID actorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "STUDENT") String actorRole,
        @ParameterObject Pageable pageable);

    @Operation(summary = "Obter treino por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Treino",
            content = @Content(schema = @Schema(implementation = TrainingView.class),
                examples = @ExampleObject(value = TRAINING_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Treino inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<TrainingView> getById(@Parameter(description = "Id do treino.") UUID id);
}
