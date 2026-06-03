# gym-flow-backend

Backend do WS Fitness — gerenciamento de treinos de academia.

Stack: **Java 21**, **Spring Boot 3.5**, **Spring Modulith 1.4**, **PostgreSQL 16**, **Flyway**, **Spring Security + JWT**, **Thymeleaf** (e-mail), **DDD + Clean Architecture**.

Módulos Modulith implementados (Fase 0 + Fase 1):

- `users` — `User` (Aluno/Professor/Administrador), `TeacherStudentBond`, perfil próprio, gestão admin (RF-001, RF-002, RF-012, RF-015, RF-016).
- `authentication` — autenticação, refresh, primeiro acesso e recuperação de senha (RF-003, RF-013, RF-014).
- `shared` / `api` — kernel compartilhado (Result/Notification/ErrorCode/VOs, GlobalExceptionHandler, observabilidade MDC, rate-limit, OpenAPI, Spring Security/JWT).

## Pré-requisitos

- JDK 21
- Docker (para PostgreSQL e MailHog em dev)
- Maven Wrapper já incluído (`./mvnw`)

## Subir a infraestrutura local

```bash
docker compose up -d
```

Sobe:
- PostgreSQL em `localhost:5432` (db/user/pass: `gymflow`)
- MailHog SMTP em `localhost:1025` e UI em `http://localhost:8025`

## Rodar a aplicação

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A aplicação sobe em `http://localhost:8080/api/v1`. Swagger UI em `http://localhost:8080/api/v1/swagger-ui`.

## Validar com testes

```bash
./mvnw test
```

Inclui `ArchitectureTests` que valida fronteiras Modulith via `ApplicationModules.verify()`.

## Bootstrap do primeiro administrador

A migração `V8__seed_bootstrap_admin.sql` cria automaticamente:
- Admin `admin@wsfitness.local` (status `PENDING_FIRST_ACCESS`)
- Convite com token raw fixo **`WS_FITNESS_BOOTSTRAP_ADMIN_2026`** (válido por 365 dias)

> **Remover ou substituir esta migração antes de promover a produção.**

## Fluxo end-to-end (HTTPie)

Use [HTTPie](https://httpie.io) ou curl. As variáveis `$ADMIN_TOK`, `$INSTRUCTOR_TOK`, etc. são os `accessToken` recebidos em cada login.

### 1. Admin consome o convite de bootstrap e autentica

```bash
http POST :8080/api/v1/auth/invites/WS_FITNESS_BOOTSTRAP_ADMIN_2026/consume \
     newPassword='AdminPass!9' passwordConfirmation='AdminPass!9'
# resposta inclui accessToken e refreshToken; copie-os para $ADMIN_TOK / $ADMIN_REFRESH

http POST :8080/api/v1/auth/login email=admin@wsfitness.local password='AdminPass!9'
```

### 2. Admin cadastra um Professor

```bash
http POST :8080/api/v1/users/instructors \
     "Authorization: Bearer $ADMIN_TOK" \
     name='Carla Treinadora' \
     email='carla@gym.local' \
     phone='+5511999990001' \
     birthDate='1990-05-10' \
     createdBy='11111111-1111-1111-1111-111111111111'
```

Abra `http://localhost:8025` — o convite enviado para `carla@gym.local` aparece no MailHog. O link contém o token raw que ela usa para definir senha.

### 3. Professor consome o convite, autentica e cadastra um Aluno

```bash
http POST :8080/api/v1/auth/invites/$CARLA_TOKEN/consume \
     newPassword='C@rla123!' passwordConfirmation='C@rla123!'
# guarde os tokens em $CARLA_TOK / $CARLA_REFRESH

http POST :8080/api/v1/users/students \
     "Authorization: Bearer $CARLA_TOK" \
     name='João Aluno' \
     email='joao@gym.local' \
     phone='+5511999990002' \
     birthDate='2002-08-20' \
     createdBy=$CARLA_USER_ID \
     createdByRole=INSTRUCTOR
# auto-bond entre Carla e João criado pelo RegisterStudentService
```

### 4. Aluno consome convite e atualiza próprio perfil

```bash
http POST :8080/api/v1/auth/invites/$JOAO_TOKEN/consume \
     newPassword='J0aoForte!' passwordConfirmation='J0aoForte!'

http PATCH :8080/api/v1/users/me \
     "Authorization: Bearer $JOAO_TOK" \
     "X-User-Id: $JOAO_USER_ID" \
     phone='+5511999990099'
```

### 5. Admin lista vínculos e gerencia status

```bash
http GET ':8080/api/v1/users/bonds?instructorId='$CARLA_USER_ID \
     "Authorization: Bearer $ADMIN_TOK"

http PATCH :8080/api/v1/users/$JOAO_USER_ID/status \
     "Authorization: Bearer $ADMIN_TOK" \
     status=INACTIVE
```

### 6. Fluxo de recuperação de senha

```bash
http POST :8080/api/v1/auth/password-recovery email='joao@gym.local'
# 202 Accepted, MailHog mostra o e-mail com link

http POST :8080/api/v1/auth/password-reset/$RESET_TOKEN \
     newPassword='OutraSenha9$' passwordConfirmation='OutraSenha9$'
```

## Convenções de erro

Respostas de erro seguem **RFC 7807** (`application/problem+json`):

| Status | Significado |
|---|---|
| 400 | JSON malformado ou campo obrigatório ausente |
| 401 | Credenciais/token inválidos ou expirados |
| 403 | Falta autorização / política de propriedade violada |
| 404 | Recurso não encontrado |
| 409 | Conflito (e-mail duplicado, vínculo existente) |
| 422 | Semântica de negócio inválida |
| 429 | Rate limit excedido |

## Decisões transversais aplicadas

Refletem o [README de requisitos](../requisitos/README.md):

- Termo único: `treino ativo` (`status = ATIVO` + vigência).
- Status do usuário: `ACTIVE`, `INACTIVE`, `BLOCKED`, `PENDING_FIRST_ACCESS`.
- Bloqueio: 5 tentativas falhas em 15 min → 15 min `BLOCKED`.
- TTL JWT: access 15 min, refresh 7 dias.
- Senha: ≥ 10 chars, 3 de 4 classes, verificação opcional contra HIBP.
- Paginação: `page` (0-based), `size` (default 20, máx 100), `sort`.
- Convite: token de uso único, TTL 72h. Recuperação: TTL 1h.

## Próximas iterações

Módulos pendentes (não implementados nesta fase):

- `exercises` — catálogo (RF-011).
- `workouts` — criação/atualização/visualização (RF-004, RF-005, RF-006, RF-010).
- `history` — execução de treino (RF-007, RF-009).
- `progress` — indicadores de evolução (RF-008).
- `anamnesis` — avaliação inicial (RF-017).
- RNF operacionais: LGPD (RNF-008), conteinerização produtiva (RNF-009 — Dockerfile multi-stage além do compose dev).
