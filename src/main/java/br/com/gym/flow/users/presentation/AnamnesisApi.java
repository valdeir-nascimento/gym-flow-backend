package br.com.gym.flow.users.presentation;

import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.openapi.ApiErrorResponse;
import br.com.gym.flow.users.domain.spi.AnamnesisView;
import br.com.gym.flow.users.domain.spi.HealthConsentView;
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

import java.util.List;
import java.util.UUID;

/**
 * OpenAPI documentation for the anamnesis / health-assessment endpoints (RF-017,
 * RNF-008/LGPD). Kept separate from {@link AnamnesisController}; error bodies are
 * RFC 7807 ({@link ApiErrorResponse}).
 */
@Tag(name = "Anamnesis", description = "Consentimento LGPD e avaliação física (anamnese) do aluno.")
interface AnamnesisApi {

    String PROBLEM_JSON = "application/problem+json";

    String CONSENT_EXAMPLE = """
        {
          "id": "c0c0c0c0-0000-0000-0000-00000000c0c0",
          "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
          "grantedBy": "22222222-2222-2222-2222-222222222222",
          "grantedAt": "2026-06-01T12:00:00Z"
        }""";

    String ANAMNESIS_EXAMPLE = """
        {
          "id": "d0d0d0d0-0000-0000-0000-00000000d0d0",
          "studentId": "a1f0c3d4-1111-2222-3333-444455556666",
          "version": 1,
          "weightKg": 82.5,
          "heightCm": 178,
          "objectives": "Hipertrofia e perda de gordura.",
          "conditioningHistory": "Sedentário há 2 anos; pratica caminhada leve.",
          "injuries": [ "Lesão prévia no ombro direito (2024)" ],
          "medicalRestrictions": [ "Hipertensão controlada" ],
          "contraindications": [ "e1e1e1e1-0000-0000-0000-0000000000e1" ],
          "observations": "Liberado para atividade de baixo impacto.",
          "createdBy": "22222222-2222-2222-2222-222222222222",
          "createdAt": "2026-06-01T12:00:00Z"
        }""";

    String STUDENT_PARAM = "Id do aluno (path).";
    String ACTOR_ID = "Id do ator autenticado (header).";
    String ACTOR_ROLE = "Perfil do ator (header).";

    @Operation(summary = "Registrar consentimento de dados de saúde (LGPD)",
        description = "Registra o consentimento explícito para tratamento de dados sensíveis de saúde (RNF-008). "
            + "Pré-requisito para registrar anamnese.")
    @ApiResponse(responseCode = "201", description = "Consentimento registrado",
        content = @Content(schema = @Schema(implementation = HealthConsentView.class),
            examples = @ExampleObject(value = CONSENT_EXAMPLE)))
    Result<HealthConsentView> grantConsent(
        @Parameter(description = STUDENT_PARAM) UUID studentId,
        @Parameter(description = ACTOR_ID, required = true) UUID actorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "INSTRUCTOR") String actorRole);

    @Operation(summary = "Registrar anamnese",
        description = "Cria uma revisão de anamnese para o aluno (RF-017). Exige consentimento LGPD prévio e que o ator "
            + "seja o próprio aluno ou o professor responsável. Exercícios contraindicados aqui não poderão compor treinos.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Anamnese registrada",
            content = @Content(schema = @Schema(implementation = AnamnesisView.class),
                examples = @ExampleObject(value = ANAMNESIS_EXAMPLE))),
        @ApiResponse(responseCode = "400", description = "Corpo inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Sem consentimento LGPD ou ator não autorizado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(name = "Consentimento ausente", value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 403, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "LGPD_CONSENT_REQUIRED", "message": "consentimento de tratamento de dados de saúde necessário" } ]
                    }"""))),
        @ApiResponse(responseCode = "422", description = "Medida implausível (peso/altura)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<AnamnesisView> register(
        @Parameter(description = STUDENT_PARAM) UUID studentId,
        @RequestBody(required = true, content = @Content(examples = @ExampleObject(name = "Anamnese inicial", value = """
            {
              "weightKg": 82.5,
              "heightCm": 178,
              "objectives": "Hipertrofia e perda de gordura.",
              "conditioningHistory": "Sedentário há 2 anos; pratica caminhada leve.",
              "injuries": [ "Lesão prévia no ombro direito (2024)" ],
              "medicalRestrictions": [ "Hipertensão controlada" ],
              "contraindications": [ "e1e1e1e1-0000-0000-0000-0000000000e1" ],
              "observations": "Liberado para atividade de baixo impacto."
            }""")))
        RegisterAnamnesisRequest req,
        @Parameter(description = ACTOR_ID, required = true) UUID actorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "INSTRUCTOR") String actorRole,
        @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "Obter anamnese mais recente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Anamnese atual",
            content = @Content(schema = @Schema(implementation = AnamnesisView.class),
                examples = @ExampleObject(value = ANAMNESIS_EXAMPLE))),
        @ApiResponse(responseCode = "403", description = "Anamnese de outro aluno / ator não autorizado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Aluno sem anamnese",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "type": "https://wsfitness/errors/validation-failed",
                      "title": "Operação não pôde ser concluída", "status": 404, "detail": "Há erros de validação",
                      "errors": [ { "field": "", "code": "ANAMNESIS_NOT_FOUND", "message": "anamnese não encontrada" } ]
                    }""")))
    })
    Result<AnamnesisView> latest(
        @Parameter(description = STUDENT_PARAM) UUID studentId,
        @Parameter(description = ACTOR_ID, required = true) UUID actorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "STUDENT") String actorRole);

    @Operation(summary = "Histórico de anamneses",
        description = "Lista as revisões de anamnese do aluno, da mais recente para a mais antiga.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Histórico (pode ser vazio)",
            content = @Content(array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                schema = @Schema(implementation = AnamnesisView.class)),
                examples = @ExampleObject(name = "Histórico", value = "[ " + ANAMNESIS_EXAMPLE + " ]"))),
        @ApiResponse(responseCode = "403", description = "Ator não autorizado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    Result<List<AnamnesisView>> history(
        @Parameter(description = STUDENT_PARAM) UUID studentId,
        @Parameter(description = ACTOR_ID, required = true) UUID actorId,
        @Parameter(description = ACTOR_ROLE, required = true, example = "INSTRUCTOR") String actorRole);
}
