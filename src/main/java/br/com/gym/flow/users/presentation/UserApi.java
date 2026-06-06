package br.com.gym.flow.users.presentation;

import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.openapi.ApiErrorResponse;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.domain.spi.UserView;
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
 * OpenAPI documentation for the user-management endpoints (RF-001, RF-002, RF-012,
 * RF-015). Kept separate from {@link UserController} so the controller stays a thin
 * delegation layer. Error bodies are RFC 7807 ({@link ApiErrorResponse}).
 */
@Tag(name = "Users", description = "Cadastro, consulta e gestão de usuários (alunos, professores, administradores).")
interface UserApi {

    String PROBLEM_JSON = "application/problem+json";

    String USER_EXAMPLE = """
        {
          "id": "a1f0c3d4-1111-2222-3333-444455556666",
          "name": "Maria Silva",
          "email": "maria@wsfitness.local",
          "phone": "+5511988887777",
          "birthDate": "2000-05-20",
          "role": "STUDENT",
          "status": "PENDING_FIRST_ACCESS",
          "createdBy": "11111111-1111-1111-1111-111111111111",
          "createdAt": "2026-06-01T12:00:00Z",
          "updatedAt": "2026-06-01T12:00:00Z"
        }""";

    String USER_PAGE = """
        {
          "content": [
            {
              "id": "a1f0c3d4-1111-2222-3333-444455556666", "name": "Maria Silva",
              "email": "maria@wsfitness.local", "phone": "+5511988887777", "birthDate": "2000-05-20",
              "role": "STUDENT", "status": "ACTIVE", "createdBy": "11111111-1111-1111-1111-111111111111",
              "createdAt": "2026-06-01T12:00:00Z", "updatedAt": "2026-06-01T12:00:00Z"
            }
          ],
          "number": 0, "size": 20, "totalElements": 1, "totalPages": 1,
          "first": true, "last": true, "numberOfElements": 1, "empty": false
        }""";

    @Operation(summary = "Cadastrar aluno",
        description = "Cria um aluno (status PENDENTE_PRIMEIRO_ACESSO) e dispara o convite por e-mail. "
            + "Administrador ou Professor podem cadastrar.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Aluno criado (header Location aponta para o recurso)",
            content = @Content(schema = @Schema(implementation = UserView.class),
                examples = @ExampleObject(value = USER_EXAMPLE))),
        @ApiResponse(responseCode = "400", description = "Campo obrigatório ausente / inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 409, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "USER_EMAIL_TAKEN", "message": "e-mail já cadastrado" } ]
                    }""")))
    })
    Result<UserView> registerStudent(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(name = "Novo aluno", value = """
            {
              "name": "Maria Silva", "email": "maria@wsfitness.local", "phone": "+5511988887777",
              "birthDate": "2000-05-20",
              "createdBy": "11111111-1111-1111-1111-111111111111", "createdByRole": "ADMINISTRATOR"
            }""")))
        RegisterStudentRequest req,
        @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "Cadastrar professor", description = "Cria um professor. Apenas Administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Professor criado",
            content = @Content(schema = @Schema(implementation = UserView.class),
                examples = @ExampleObject(value = """
                    {
                      "id": "22222222-2222-2222-2222-222222222222", "name": "Carlos Treinador",
                      "email": "professor@wsfitness.local", "phone": "+5511977776666", "birthDate": "1992-03-10",
                      "role": "INSTRUCTOR", "status": "PENDING_FIRST_ACCESS",
                      "createdBy": "11111111-1111-1111-1111-111111111111",
                      "createdAt": "2026-06-01T12:00:00Z", "updatedAt": "2026-06-01T12:00:00Z"
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Campo obrigatório ausente / inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<UserView> registerInstructor(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(name = "Novo professor", value = """
            {
              "name": "Carlos Treinador", "email": "professor@wsfitness.local", "phone": "+5511977776666",
              "birthDate": "1992-03-10", "createdBy": "11111111-1111-1111-1111-111111111111"
            }""")))
        RegisterInstructorRequest req,
        @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "Listar usuários", description = "Lista paginada com filtros opcionais (RF-012).")
    @ApiResponse(responseCode = "200", description = "Página de usuários",
        content = @Content(schema = @Schema(implementation = Page.class), examples = @ExampleObject(value = USER_PAGE)))
    Result<Page<UserView>> list(
        @Parameter(description = "Filtra por perfil.", example = "STUDENT") Role role,
        @Parameter(description = "Filtra por status.", example = "ACTIVE") UserStatus status,
        @Parameter(description = "Busca por nome ou e-mail.", example = "maria") String search,
        @ParameterObject Pageable pageable);

    @Operation(summary = "Obter usuário por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário",
            content = @Content(schema = @Schema(implementation = UserView.class), examples = @ExampleObject(value = USER_EXAMPLE))),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<UserView> getById(
        @Parameter(description = "Id do usuário.", example = "a1f0c3d4-1111-2222-3333-444455556666") UUID id);

    @Operation(summary = "Alterar status do usuário",
        description = "Ativa/inativa/bloqueia (RF-012). O administrador não pode alterar a própria conta, e o último "
            + "administrador ativo não pode ser inativado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status alterado",
            content = @Content(schema = @Schema(implementation = UserView.class),
                examples = @ExampleObject(value = USER_EXAMPLE))),
        @ApiResponse(responseCode = "400", description = "Status inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Inativaria o último administrador ativo",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 409, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "USER_LAST_ADMINISTRATOR", "message": "deve existir ao menos um administrador ativo" } ]
                    }"""))),
        @ApiResponse(responseCode = "422", description = "Auto-alteração de status",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 422, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "USER_SELF_MANAGEMENT", "message": "não é permitido alterar status ou perfil da própria conta" } ]
                    }""")))
    })
    Result<UserView> changeStatus(
        @Parameter(description = "Id do usuário alvo.") UUID id,
        @RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Ativar", value = "{ \"status\": \"ACTIVE\" }"),
            @ExampleObject(name = "Inativar", value = "{ \"status\": \"INACTIVE\" }"),
            @ExampleObject(name = "Bloquear", value = "{ \"status\": \"BLOCKED\" }")
        }))
        ChangeStatusRequest req,
        @Parameter(description = "Id do administrador autenticado (header).", required = true) UUID actorId);

    @Operation(summary = "Alterar perfil do usuário",
        description = "Promove/rebaixa o perfil (RF-012). Auto-alteração -> 422; rebaixar o último administrador -> 409.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil alterado",
            content = @Content(schema = @Schema(implementation = UserView.class),
                examples = @ExampleObject(value = USER_EXAMPLE))),
        @ApiResponse(responseCode = "400", description = "Perfil inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Rebaixaria o último administrador ativo",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Auto-alteração de perfil",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<UserView> changeRole(
        @Parameter(description = "Id do usuário alvo.") UUID id,
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = "{ \"role\": \"INSTRUCTOR\" }")))
        ChangeRoleRequest req,
        @Parameter(description = "Id do administrador autenticado (header).", required = true) UUID actorId);

    @Operation(summary = "Meu perfil", description = "Dados do usuário autenticado (RF-015).")
    @ApiResponse(responseCode = "200", description = "Perfil",
        content = @Content(schema = @Schema(implementation = UserView.class), examples = @ExampleObject(value = USER_EXAMPLE)))
    Result<UserView> me(
        @Parameter(description = "Id do usuário autenticado (header).", required = true) UUID currentUserId);

    @Operation(summary = "Atualizar meu perfil",
        description = "Atualiza nome/telefone/nascimento do próprio usuário (RF-015). E-mail/perfil/status são imutáveis aqui.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil atualizado",
            content = @Content(schema = @Schema(implementation = UserView.class),
                examples = @ExampleObject(value = USER_EXAMPLE))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<UserView> updateMe(
        @Parameter(description = "Id do usuário autenticado (header).", required = true) UUID currentUserId,
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            { "name": "Maria Silva Santos", "phone": "+5511900001111", "birthDate": "2000-05-20" }""")))
        UpdateOwnProfileRequest req);
}
