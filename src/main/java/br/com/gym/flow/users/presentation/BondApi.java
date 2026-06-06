package br.com.gym.flow.users.presentation;

import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.openapi.ApiErrorResponse;
import br.com.gym.flow.users.domain.spi.BondView;
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
 * OpenAPI documentation for the student↔instructor bond endpoints (RF-016).
 * Kept separate from {@link BondController}; error bodies are RFC 7807
 * ({@link ApiErrorResponse}).
 */
@Tag(name = "Bonds", description = "Vínculo aluno↔professor: atribuição, transferência, remoção e consulta.")
interface BondApi {

    String PROBLEM_JSON = "application/problem+json";

    String BOND_EXAMPLE = """
        {
          "id": "b0b0b0b0-0000-0000-0000-00000000b0b0",
          "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
          "instructorId": "22222222-2222-2222-2222-222222222222",
          "startedAt": "2026-06-01T12:00:00Z",
          "endedAt": null,
          "createdBy": "11111111-1111-1111-1111-111111111111",
          "active": true
        }""";

    String BOND_PAGE = """
        {
          "content": [
            {
              "id": "b0b0b0b0-0000-0000-0000-00000000b0b0",
              "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
              "instructorId": "22222222-2222-2222-2222-222222222222",
              "startedAt": "2026-06-01T12:00:00Z", "endedAt": null,
              "createdBy": "11111111-1111-1111-1111-111111111111", "active": true
            }
          ],
          "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
          "first": true, "last": true, "numberOfElements": 1, "empty": false
        }""";

    @Operation(summary = "Vincular aluno a professor",
        description = "Cria o vínculo ativo entre um aluno e um professor (RF-016). O aluno só pode ter um professor ativo.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Vínculo criado",
            content = @Content(schema = @Schema(implementation = BondView.class),
                examples = @ExampleObject(value = BOND_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Aluno ou professor inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Aluno já possui professor ativo / vínculo já existe",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 409, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "BOND_STUDENT_HAS_ACTIVE_INSTRUCTOR", "message": "aluno já possui professor vinculado" } ]
                    }"""))),
        @ApiResponse(responseCode = "422", description = "Aluno ou professor inativo",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<BondView> assign(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            {
              "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
              "instructorId": "22222222-2222-2222-2222-222222222222",
              "createdBy": "11111111-1111-1111-1111-111111111111"
            }""")))
        AssignBondRequest req,
        @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "Transferir aluno para outro professor",
        description = "Encerra o vínculo ativo atual e cria um novo com o professor de destino (RF-016).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Aluno transferido",
            content = @Content(schema = @Schema(implementation = BondView.class),
                examples = @ExampleObject(value = BOND_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Aluno/professor/vínculo inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Aluno ou professor de destino inativo",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<BondView> transfer(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            {
              "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
              "newInstructorId": "22222222-2222-2222-2222-222222222222",
              "actor": "11111111-1111-1111-1111-111111111111"
            }""")))
        TransferBondRequest req);

    @Operation(summary = "Remover vínculo",
        description = "Desativa o vínculo. O professor só remove os próprios vínculos; Administrador remove qualquer um.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vínculo removido",
            content = @Content(schema = @Schema(implementation = BondView.class),
                examples = @ExampleObject(name = "Vínculo desativado", value = """
                    {
                      "id": "b0b0b0b0-0000-0000-0000-00000000b0b0",
                      "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
                      "instructorId": "22222222-2222-2222-2222-222222222222",
                      "startedAt": "2026-06-01T12:00:00Z", "endedAt": "2026-06-05T18:00:00Z",
                      "createdBy": "11111111-1111-1111-1111-111111111111", "active": false
                    }"""))),
        @ApiResponse(responseCode = "403", description = "Vínculo não pertence ao professor",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 403, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "BOND_NOT_OWNED_BY_INSTRUCTOR", "message": "vínculo não pertence ao professor" } ]
                    }"""))),
        @ApiResponse(responseCode = "404", description = "Vínculo inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<BondView> remove(
        @Parameter(description = "Id do vínculo.") UUID id,
        @Parameter(description = "Id do ator autenticado (header).", required = true) UUID actorId,
        @Parameter(description = "Perfil do ator (header). ADMINISTRATOR ignora a checagem de posse.",
            example = "INSTRUCTOR") String actorRole);

    @Operation(summary = "Listar vínculos", description = "Lista paginada, opcionalmente filtrada por professor ou aluno.")
    @ApiResponse(responseCode = "200", description = "Página de vínculos",
        content = @Content(schema = @Schema(implementation = Page.class), examples = @ExampleObject(value = BOND_PAGE)))
    Result<Page<BondView>> list(
        @Parameter(description = "Filtra pelos vínculos de um professor.") UUID instructorId,
        @Parameter(description = "Filtra pelos vínculos de um aluno.") UUID studentId,
        @ParameterObject Pageable pageable);
}
