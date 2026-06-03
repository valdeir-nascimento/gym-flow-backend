package br.com.gym.flow.shared.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI wsFitnessOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("WS Fitness — API")
                .version("v1")
                .description("""
                    Backend de gerenciamento de treinos de academia, organizado em módulos de negócio:

                    - **authentication** — login, refresh, definição e recuperação de senha.
                    - **users** — alunos, professores, administradores, vínculo aluno↔professor e perfil próprio.
                    - **exercises** — catálogo central de exercícios.
                    - **workouts** — criação, atualização, consulta e acompanhamento de treinos.
                    - **history** — registro e consulta de execuções de treino.
                    - **progress** — indicadores e séries temporais de evolução do aluno.
                    - **anamnesis** — avaliação física inicial e medidas periódicas.

                    ### Autenticação

                    A maior parte das chamadas exige um JWT no header `Authorization: Bearer <token>`,
                    obtido em `/api/v1/auth/login`. O refresh token é renovado em `/api/v1/auth/refresh`.

                    ### Convenções

                    - Datas e instantes em ISO-8601 (datas locais: `2026-05-07`; instantes UTC: `2026-05-07T20:30:00Z`).
                    - Telefones em E.164 (`+5511999998888`).
                    - IDs públicos são UUID v4.
                    - Paginação: `page` (0-based), `size` (default 20, máx 100), `sort`.
                    - Status HTTP: `400` para JSON malformado / campo ausente; `422` para semântica de negócio.
                    - Respostas de erro seguem RFC 7807 (`application/problem+json`).
                    """)
                .contact(new Contact()
                    .name("WS Fitness Backend Team")
                    .email("dev@wsfitness.local"))
                .license(new License().name("Proprietary")))
            .servers(List.of(
                new Server().url("http://localhost:8080/api/v1").description("Local"),
                new Server().url("https://api.wsfitness.example.com/api/v1").description("Produção")
            ))
            .components(new Components()
                .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT obtido em `/auth/login`.")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
