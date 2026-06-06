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

@Tag(name = "Authentication", description = "Login, renovação de token, primeiro acesso e recuperação/troca de senha.")
interface AuthApi {

    @Operation(
        summary = "Autenticar (login)",
        description = "Valida e-mail e senha e devolve um par de tokens (access + refresh). Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticado",
            content = @Content(schema = @Schema(implementation = TokenPairView.class))),
        @ApiResponse(responseCode = "400", description = "Corpo inválido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas, conta bloqueada ou limite de tentativas",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirements
    Result<TokenPairView> login(
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
            { "email": "admin@wsfitness.local", "password": "AdminSenha@2026" }""")))
        LoginRequest req,
        @Parameter(hidden = true) HttpServletRequest http);

    @Operation(
        summary = "Renovar tokens (refresh)",
        description = "Rotaciona o refresh token, retornando um novo par. Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Novo par de tokens",
            content = @Content(schema = @Schema(implementation = TokenPairView.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirements
    Result<TokenPairView> refresh(RefreshRequest req);

    @Operation(summary = "Sair (logout)", description = "Revoga o refresh token informado.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Refresh token revogado"),
        @ApiResponse(responseCode = "400", description = "Corpo inválido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<Void> logout(RefreshRequest req);

    @Operation(
        summary = "Definir senha no primeiro acesso (consumir convite)",
        description = "Consome o convite de uso único, define a senha e ativa o usuário. NÃO faz login: "
            + "retorna 204 e o usuário autentica em seguida. Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha definida; usuário ativo"),
        @ApiResponse(responseCode = "400", description = "Confirmação divergente ou corpo inválido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "410", description = "Convite inválido, expirado ou já consumido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Senha não atende à política",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirements
    Result<Void> consumeInvite(
        @Parameter(description = "Token do convite recebido por e-mail.", example = "WS_FITNESS_BOOTSTRAP_ADMIN_2026")
        String token,
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(value = """
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
            { "email": "admin@wsfitness.local" }""")))
        RecoveryRequest req);

    @Operation(
        summary = "Redefinir senha com token",
        description = "Define uma nova senha usando o token de recuperação (uso único). Invalida os refresh tokens "
            + "ativos. Endpoint público.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha redefinida"),
        @ApiResponse(responseCode = "400", description = "Confirmação divergente ou corpo inválido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "410", description = "Token inválido, expirado ou consumido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Senha fraca ou igual à anterior",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirements
    Result<Void> resetPassword(
        @Parameter(description = "Token de redefinição recebido por e-mail.") String token,
        ResetPasswordRequest req);

    @Operation(
        summary = "Trocar a própria senha",
        description = "Troca a senha do usuário autenticado. Exige a senha atual e revoga as demais sessões, "
            + "preservando a atual.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Senha trocada"),
        @ApiResponse(responseCode = "400", description = "Confirmação divergente ou corpo inválido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Senha atual incorreta",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "422", description = "Nova senha fraca ou igual à anterior",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<Void> changePassword(
        ChangePasswordRequest req,
        @Parameter(description = "Id do usuário autenticado.", required = true,
            example = "11111111-1111-1111-1111-111111111111")
        UUID userId);
}
