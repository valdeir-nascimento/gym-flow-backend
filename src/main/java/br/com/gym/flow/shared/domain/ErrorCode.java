package br.com.gym.flow.shared.domain;

public enum ErrorCode {

    // Generic / shared
    INVALID_INPUT(ErrorCategory.VALIDATION, "entrada inválida"),

    // Shared value objects
    INVALID_EMAIL(ErrorCategory.VALIDATION, "e-mail inválido"),
    INVALID_PHONE(ErrorCategory.VALIDATION, "telefone inválido"),

    // User (RF-001, RF-002, RF-012, RF-015)
    BLANK_NAME(ErrorCategory.VALIDATION, "nome obrigatório"),
    INVALID_BIRTH_DATE(ErrorCategory.VALIDATION, "data de nascimento inválida"),
    USER_NOT_FOUND(ErrorCategory.NOT_FOUND, "usuário não encontrado"),
    USER_EMAIL_TAKEN(ErrorCategory.CONFLICT, "e-mail já cadastrado"),
    USER_INACTIVE(ErrorCategory.UNAUTHORIZED, "usuário inativo"),
    USER_BLOCKED(ErrorCategory.UNAUTHORIZED, "usuário bloqueado"),
    USER_NOT_PENDING_FIRST_ACCESS(ErrorCategory.UNAUTHORIZED, "usuário não está aguardando primeiro acesso"),
    INVALID_USER_STATUS_TRANSITION(ErrorCategory.BUSINESS_RULE, "transição de status inválida"),
    FORBIDDEN_ROLE(ErrorCategory.FORBIDDEN, "operação não permitida para o perfil"),

    // Bond — Aluno↔Professor (RF-016)
    BOND_NOT_FOUND(ErrorCategory.NOT_FOUND, "vínculo não encontrado"),
    BOND_ALREADY_EXISTS(ErrorCategory.CONFLICT, "vínculo já existe"),
    BOND_STUDENT_HAS_ACTIVE_INSTRUCTOR(ErrorCategory.CONFLICT, "aluno já possui professor vinculado"),
    BOND_INACTIVE_PARTICIPANT(ErrorCategory.BUSINESS_RULE, "participante do vínculo inativo"),
    BOND_NOT_OWNED_BY_INSTRUCTOR(ErrorCategory.FORBIDDEN, "vínculo não pertence ao professor"),

    // Exercise catalog (RF-011)
    EXERCISE_NOT_FOUND(ErrorCategory.NOT_FOUND, "exercício não encontrado"),
    EXERCISE_NAME_TAKEN(ErrorCategory.CONFLICT, "nome de exercício já cadastrado"),
    EXERCISE_ALREADY_INACTIVE(ErrorCategory.BUSINESS_RULE, "exercício já inativado"),

    // Training (RF-004)
    TRAINING_NOT_FOUND(ErrorCategory.NOT_FOUND, "treino não encontrado"),
    TRAINING_INSTRUCTOR_NOT_LINKED(ErrorCategory.FORBIDDEN, "professor não vinculado ao aluno"),
    TRAINING_NOT_OWNED(ErrorCategory.FORBIDDEN, "treino não pertence ao professor"),
    TRAINING_NOT_STUDENT_OWNER(ErrorCategory.FORBIDDEN, "treinos pertencem a outro aluno"),
    TRAINING_ARCHIVED(ErrorCategory.CONFLICT, "treino arquivado não pode ser editado"),
    TRAINING_STUDENT_INACTIVE(ErrorCategory.BUSINESS_RULE, "aluno inativo não pode receber treino"),
    TRAINING_INACTIVE_EXERCISE(ErrorCategory.BUSINESS_RULE, "exercício inativo não pode compor o treino"),
    TRAINING_OVERLAPPING_PERIOD(ErrorCategory.BUSINESS_RULE, "já existe treino ativo com vigência sobreposta"),

    // Workout execution / history (RF-007, RF-009)
    EXECUTION_NOT_FOUND(ErrorCategory.NOT_FOUND, "execução não encontrada"),
    EXECUTION_NOT_OWNED(ErrorCategory.FORBIDDEN, "histórico pertence a outro aluno"),
    EXECUTION_TRAINING_NOT_FOUND(ErrorCategory.NOT_FOUND, "treino da execução não encontrado"),
    EXECUTION_TRAINING_NOT_OWNED(ErrorCategory.FORBIDDEN, "treino não pertence ao aluno"),
    EXECUTION_FUTURE_DATETIME(ErrorCategory.VALIDATION, "data/hora da execução não pode ser futura"),
    EXECUTION_END_BEFORE_START(ErrorCategory.BUSINESS_RULE, "fim da execução anterior ao início"),
    EXECUTION_TRAINING_INACTIVE_OUT_OF_WINDOW(ErrorCategory.BUSINESS_RULE, "treino inativado fora da janela de tolerância"),
    EXECUTION_ALREADY_REGISTERED(ErrorCategory.CONFLICT, "execução já registrada"),

    // Evolution / progress (RF-008)
    EVOLUTION_NOT_OWNED(ErrorCategory.FORBIDDEN, "evolução pertence a outro aluno"),
    EVOLUTION_PERIOD_TOO_LONG(ErrorCategory.BUSINESS_RULE, "período máximo de consulta é de 24 meses"),

    // Authentication (RF-003)
    INVALID_CREDENTIALS(ErrorCategory.UNAUTHORIZED, "credenciais inválidas"),
    ACCOUNT_LOCKED(ErrorCategory.UNAUTHORIZED, "conta temporariamente bloqueada"),
    LOGIN_THROTTLED(ErrorCategory.UNAUTHORIZED, "limite de tentativas atingido"),

    // Tokens (RF-003, RF-013, RF-014)
    INVALID_REFRESH_TOKEN(ErrorCategory.UNAUTHORIZED, "refresh token inválido"),
    REFRESH_TOKEN_EXPIRED(ErrorCategory.UNAUTHORIZED, "refresh token expirado"),
    INVALID_INVITE_TOKEN(ErrorCategory.UNAUTHORIZED, "convite inválido"),
    INVITE_TOKEN_EXPIRED(ErrorCategory.UNAUTHORIZED, "convite expirado"),
    INVITE_TOKEN_CONSUMED(ErrorCategory.UNAUTHORIZED, "convite já utilizado"),
    INVALID_PASSWORD_RESET_TOKEN(ErrorCategory.UNAUTHORIZED, "token de redefinição inválido"),
    PASSWORD_RESET_TOKEN_EXPIRED(ErrorCategory.UNAUTHORIZED, "token de redefinição expirado"),

    // Password policy (RF-013, RF-014, RF-015, RNF-001)
    WEAK_PASSWORD(ErrorCategory.BUSINESS_RULE, "senha fraca"),
    PASSWORD_PWNED(ErrorCategory.BUSINESS_RULE, "senha presente em lista de vazadas"),
    PASSWORD_MISMATCH(ErrorCategory.VALIDATION, "confirmação de senha não confere"),

    // Mail
    MAIL_DELIVERY_FAILED(ErrorCategory.INFRASTRUCTURE, "falha ao enviar e-mail");

    private final ErrorCategory category;
    private final String defaultMessage;

    ErrorCode(ErrorCategory category, String defaultMessage) {
        this.category = category;
        this.defaultMessage = defaultMessage;
    }

    public ErrorCategory category() {
        return category;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
