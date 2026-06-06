package br.com.gym.flow.authentication.presentation;

import br.com.gym.flow.authentication.application.usecase.TokenPairView;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * OpenAPI documentation for the authentication endpoints (RF-003, RF-013, RF-014,
 * RF-015). Kept separate from {@link AuthController} so the controller stays a thin
 * delegation layer while the (dense) Swagger metadata — with real request/response
 * examples — lives here. The error envelope is the RFC 7807 {@link ApiErrorResponse}
 * ({@code application/problem+json}).
 */
@Tag(name = "Authentication", description = "Login, renovação de token, primeiro acesso e recuperação/troca de senha.")
interface AuthApi {

    String PROBLEM_JSON = "application/problem+json";

    @Operation(
        summary = "Autenticar (login)",
        description = "Valida e-mail e senha e devolve um par de tokens (access + refresh). Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticado",
            content = @Content(schema = @Schema(implementation = TokenPairView.class),
                examples = @ExampleObject(name = "Par de tokens", value = """
                    {
                      "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMTExMTExMS4uLiJ9.sig",
                      "refreshToken": "f3a1c2e4-9b8d-4a76-b1c2-0e9f8a7b6c5d",
                      "accessTokenExpiresAt": "2026-06-05T20:45:00Z",
                      "refreshTokenExpiresAt": "2026-06-12T20:30:00Z",
                      "userId": "11111111-1111-1111-1111-111111111111",
                      "role": "ADMINISTRATOR"
                    }"""))),
        @ApiResponse(responseCode = "400", description = "Corpo inválido (campo ausente / JSON malformado)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "E-mail em branco", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Campos inválidos",
                      "status": 400,
                      "detail": "Um ou mais campos da requisição não passaram na validação",
                      "errors": [ { "field": "email", "code": "NOTBLANK", "message": "O campo 'e-mail' não pode ficar vazio." } ]
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas, conta bloqueada ou limite de tentativas",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(name = "Credenciais inválidas", value = """
                        {
                          "type": "https://wsfitness/errors/validation-failed",
                          "title": "Operação não pôde ser concluída",
                          "status": 401,
                          "detail": "Há erros de validação",
                          "errors": [ { "field": "", "code": "INVALID_CREDENTIALS", "message": "credenciais inválidas" } ]
                        }"""),
                    @ExampleObject(name = "Limite de tentativas", value = """
                        {
                          "type": "https://wsfitness/errors/validation-failed",
                          "title": "Operação não pôde ser concluída",
                          "status": 401,
                          "detail": "Há erros de validação",
                          "errors": [ { "field": "", "code": "LOGIN_THROTTLED", "message": "limite de tentativas atingido" } ]
                        }""")
                }))
    })
    @SecurityRequirements
    Result<TokenPairView> login(
        @RequestBody(required = true, content = @Content(examples = {
            @ExampleObject(name = "Administrador", value = """
                { "email": "admin@wsfitness.local", "password": "AdminSenha@2026" }"""),
            @ExampleObject(name = "Aluno", value = """
                { "email": "aluno@wsfitness.local", "password": "AlunoSenha@2026" }""")
        }))
        LoginRequest req,
        @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
        summary = "Renovar tokens (refresh)",
        description = "Rotaciona o refresh token, retornando um novo par. Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Novo par de tokens",
            content = @Content(schema = @Schema(implementation = TokenPairView.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Refresh inválido", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída",
                      "status": 401,
                      "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "INVALID_REFRESH_TOKEN", "message": "refresh token inválido" } ]
                    }""")))
    })
    @SecurityRequirements
    Result<TokenPairView> refresh(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            { "refreshToken": "f3a1c2e4-9b8d-4a76-b1c2-0e9f8a7b6c5d" }""")))
        RefreshRequest req);

    @Operation(summary = "Sair (logout)", description = "Revoga o refresh token informado.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Refresh token revogado"),
        @ApiResponse(responseCode = "400", description = "Corpo inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<Void> logout(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            { "refreshToken": "f3a1c2e4-9b8d-4a76-b1c2-0e9f8a7b6c5d" }""")))
        RefreshRequest req);

    @Operation(
        summary = "Definir senha no primeiro acesso (consumir convite)",
        description = "Consome o convite de uso único, define a senha e ativa o usuário. NÃO faz login: "
            + "retorna 204 e o usuário autentica em seguida. Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha definida; usuário ativo"),
        @ApiResponse(responseCode = "400", description = "Confirmação divergente ou corpo inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Confirmação divergente", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída",
                      "status": 400,
                      "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "PASSWORD_MISMATCH", "message": "confirmação de senha não confere" } ]
                    }"""))),
        @ApiResponse(responseCode = "410", description = "Convite inválido, expirado ou já consumido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Convite expirado", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída",
                      "status": 410,
                      "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "INVITE_TOKEN_EXPIRED", "message": "convite expirado" } ]
                    }"""))),
        @ApiResponse(responseCode = "422", description = "Senha não atende à política",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Senha fraca", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída",
                      "status": 422,
                      "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "WEAK_PASSWORD", "message": "senha fraca" } ]
                    }""")))
    })
    @SecurityRequirements
    Result<Void> consumeInvite(
        @Parameter(description = "Token do convite recebido por e-mail.", example = "WS_FITNESS_BOOTSTRAP_ADMIN_2026")
        String token,
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(name = "Definir senha", value = """
            { "newPassword": "NovaSenha@2026", "passwordConfirmation": "NovaSenha@2026" }""")))
        ConsumeInviteRequest req,
        @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
        summary = "Solicitar recuperação de senha",
        description = "Inicia a recuperação por e-mail. Resposta neutra (sempre 202) para não revelar a existência "
            + "da conta. Endpoint público.")
    @ApiResponse(responseCode = "202", description = "Solicitação aceita (e-mail enviado se a conta for elegível)")
    @SecurityRequirements
    Result<Void> requestRecovery(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            { "email": "aluno@wsfitness.local" }""")))
        RecoveryRequest req);

    @Operation(
        summary = "Redefinir senha com token",
        description = "Define uma nova senha usando o token de recuperação (uso único). Invalida os refresh tokens "
            + "ativos. Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha redefinida"),
        @ApiResponse(responseCode = "400", description = "Confirmação divergente ou corpo inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "410", description = "Token inválido, expirado ou consumido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Token de reset inválido", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída",
                      "status": 410,
                      "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "INVALID_PASSWORD_RESET_TOKEN", "message": "token de redefinição inválido" } ]
                    }"""))),
        @ApiResponse(responseCode = "422", description = "Senha fraca ou igual à anterior",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Senha igual à anterior", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída",
                      "status": 422,
                      "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "WEAK_PASSWORD", "message": "a nova senha não pode ser igual à anterior" } ]
                    }""")))
    })
    @SecurityRequirements
    Result<Void> resetPassword(
        @Parameter(description = "Token de redefinição recebido por e-mail.",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String token,
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            { "newPassword": "OutraSenha@2026", "passwordConfirmation": "OutraSenha@2026" }""")))
        ResetPasswordRequest req);

    @Operation(
        summary = "Trocar a própria senha",
        description = "Troca a senha do usuário autenticado. Exige a senha atual e revoga as demais sessões, "
            + "preservando a atual.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha trocada"),
        @ApiResponse(responseCode = "400", description = "Confirmação divergente ou corpo inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Senha atual incorreta",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Senha atual incorreta", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída",
                      "status": 401,
                      "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "INVALID_CREDENTIALS", "message": "credenciais inválidas" } ]
                    }"""))),
        @ApiResponse(responseCode = "422", description = "Nova senha fraca ou igual à anterior",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<Void> changePassword(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(name = "Trocar senha", value = """
            {
              "currentPassword": "AdminSenha@2026",
              "newPassword": "AdminSenha@2027",
              "passwordConfirmation": "AdminSenha@2027",
              "currentRefreshToken": "f3a1c2e4-9b8d-4a76-b1c2-0e9f8a7b6c5d"
            }""")))
        ChangePasswordRequest req,
        @Parameter(description = "Id do usuário autenticado.", required = true,
            example = "11111111-1111-1111-1111-111111111111")
        UUID userId);
}
