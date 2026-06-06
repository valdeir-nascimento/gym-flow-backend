package br.com.gym.flow.exercises.presentation;

import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.ExerciseStatus;
import br.com.gym.flow.exercises.domain.MuscleGroup;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * OpenAPI documentation for the exercise-catalog endpoints (RF-011). Kept separate
 * from {@link ExerciseController}; error bodies are RFC 7807 ({@link ApiErrorResponse}).
 * Mutations exigem perfil ADMINISTRATOR (header {@code X-User-Role}).
 */
@Tag(name = "Exercises", description = "Catálogo central de exercícios: cadastro, edição, inativação e consulta.")
interface ExerciseApi {

    String PROBLEM_JSON = "application/problem+json";
    String ROLE_DESC = "Perfil do ator (header). Mutações exigem ADMINISTRATOR.";

    String EXERCISE_EXAMPLE = """
        {
          "id": "e1e1e1e1-0000-0000-0000-0000000000e1",
          "name": "Supino reto com barra",
          "muscleGroup": "CHEST",
          "description": "Empurrar a barra a partir do peito, com escápulas retraídas.",
          "equipment": "Barra e banco",
          "difficultyLevel": "INTERMEDIATE",
          "videoUrl": "https://videos.wsfitness.local/supino-reto.mp4",
          "imageUrl": "https://img.wsfitness.local/supino-reto.png",
          "status": "ACTIVE",
          "createdBy": "11111111-1111-1111-1111-111111111111",
          "createdAt": "2026-06-01T12:00:00Z",
          "updatedAt": "2026-06-01T12:00:00Z"
        }""";

    String EXERCISE_PAGE = """
        {
          "content": [
            {
              "id": "e1e1e1e1-0000-0000-0000-0000000000e1", "name": "Supino reto com barra",
              "muscleGroup": "CHEST", "description": "Empurrar a barra a partir do peito.",
              "equipment": "Barra e banco", "difficultyLevel": "INTERMEDIATE",
              "videoUrl": "https://videos.wsfitness.local/supino-reto.mp4",
              "imageUrl": "https://img.wsfitness.local/supino-reto.png", "status": "ACTIVE",
              "createdBy": "11111111-1111-1111-1111-111111111111",
              "createdAt": "2026-06-01T12:00:00Z", "updatedAt": "2026-06-01T12:00:00Z"
            }
          ],
          "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
          "first": true, "last": true, "numberOfElements": 1, "empty": false
        }""";

    @Operation(summary = "Cadastrar exercício", description = "Adiciona um exercício ao catálogo (RF-011). Apenas Administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Exercício criado",
            content = @Content(schema = @Schema(implementation = ExerciseView.class),
                examples = @ExampleObject(value = EXERCISE_EXAMPLE))),
        @ApiResponse(responseCode = "400", description = "Corpo inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nome de exercício já cadastrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 409, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "EXERCISE_NAME_TAKEN", "message": "nome de exercício já cadastrado" } ]
                    }""")))
    })
    Result<ExerciseView> register(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(name = "Novo exercício", value = """
            {
              "name": "Supino reto com barra",
              "muscleGroup": "CHEST",
              "description": "Empurrar a barra a partir do peito, com escápulas retraídas.",
              "equipment": "Barra e banco",
              "difficultyLevel": "INTERMEDIATE",
              "videoUrl": "https://videos.wsfitness.local/supino-reto.mp4",
              "imageUrl": "https://img.wsfitness.local/supino-reto.png"
            }""")))
        RegisterExerciseRequest req,
        @Parameter(description = "Id do ator autenticado (header).", required = true) UUID actorId,
        @Parameter(description = ROLE_DESC, required = true, example = "ADMINISTRATOR") String actorRole,
        @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "Atualizar exercício", description = "Edita os dados descritivos do exercício. Apenas Administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Exercício atualizado",
            content = @Content(schema = @Schema(implementation = ExerciseView.class),
                examples = @ExampleObject(value = EXERCISE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Exercício inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nome de exercício já cadastrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<ExerciseView> update(
        @Parameter(description = "Id do exercício.") UUID id,
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            {
              "name": "Supino reto com barra",
              "muscleGroup": "CHEST",
              "description": "Variação com pegada fechada para tríceps.",
              "equipment": "Barra W",
              "difficultyLevel": "ADVANCED",
              "videoUrl": "https://videos.wsfitness.local/supino-fechado.mp4",
              "imageUrl": "https://img.wsfitness.local/supino-fechado.png"
            }""")))
        UpdateExerciseRequest req,
        @Parameter(description = ROLE_DESC, required = true, example = "ADMINISTRATOR") String actorRole);

    @Operation(summary = "Inativar exercício",
        description = "Inativa o exercício (soft-delete). Inativos não podem compor novos treinos. Apenas Administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Exercício inativado",
            content = @Content(schema = @Schema(implementation = ExerciseView.class),
                examples = @ExampleObject(name = "Inativado", value = """
                    {
                      "id": "e1e1e1e1-0000-0000-0000-0000000000e1", "name": "Supino reto com barra",
                      "muscleGroup": "CHEST", "description": "Empurrar a barra a partir do peito.",
                      "equipment": "Barra e banco", "difficultyLevel": "INTERMEDIATE",
                      "videoUrl": "https://videos.wsfitness.local/supino-reto.mp4",
                      "imageUrl": "https://img.wsfitness.local/supino-reto.png", "status": "INACTIVE",
                      "createdBy": "11111111-1111-1111-1111-111111111111",
                      "createdAt": "2026-06-01T12:00:00Z", "updatedAt": "2026-06-05T18:00:00Z"
                    }"""))),
        @ApiResponse(responseCode = "404", description = "Exercício inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Exercício já inativado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 422, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "EXERCISE_ALREADY_INACTIVE", "message": "exercício já inativado" } ]
                    }""")))
    })
    Result<ExerciseView> deactivate(
        @Parameter(description = "Id do exercício.") UUID id,
        @Parameter(description = ROLE_DESC, required = true, example = "ADMINISTRATOR") String actorRole);

    @Operation(summary = "Listar exercícios", description = "Lista paginada com filtros opcionais.")
    @ApiResponse(responseCode = "200", description = "Página de exercícios",
        content = @Content(schema = @Schema(implementation = Page.class), examples = @ExampleObject(value = EXERCISE_PAGE)))
    Result<Page<ExerciseView>> list(
        @Parameter(description = "Filtra por grupo muscular.", example = "CHEST") MuscleGroup muscleGroup,
        @Parameter(description = "Filtra por nível de dificuldade.", example = "INTERMEDIATE") DifficultyLevel difficultyLevel,
        @Parameter(description = "Filtra por equipamento.", example = "Barra") String equipment,
        @Parameter(description = "Busca por nome.", example = "supino") String search,
        @Parameter(description = "Filtra por status.", example = "ACTIVE") ExerciseStatus status,
        @ParameterObject Pageable pageable);

    @Operation(summary = "Obter exercício por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Exercício",
            content = @Content(schema = @Schema(implementation = ExerciseView.class),
                examples = @ExampleObject(value = EXERCISE_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Exercício inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<ExerciseView> getById(@Parameter(description = "Id do exercício.") UUID id);
}
