package br.com.gym.flow.users.presentation;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Test Data Builder for {@link RegisterStudentRequest}: a valid student payload by
 * default; each test overrides only the field it exercises. Serialized via the
 * ObjectMapper in the test so the payload stays coupled to the real DTO (team rule 2).
 */
public final class RegisterStudentRequestBuilder {

    private String name = "Maria Silva";
    private String email = "maria@example.com";
    private String phone = "+5511912345678";
    private LocalDate birthDate = LocalDate.of(2000, 1, 1);
    private UUID createdBy = null;
    private String createdByRole = null;

    public static RegisterStudentRequestBuilder aRegisterStudentRequest() {
        return new RegisterStudentRequestBuilder();
    }

    public RegisterStudentRequestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public RegisterStudentRequestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public RegisterStudentRequestBuilder withCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public RegisterStudentRequestBuilder withCreatedByRole(String createdByRole) {
        this.createdByRole = createdByRole;
        return this;
    }

    public RegisterStudentRequest build() {
        return new RegisterStudentRequest(name, email, phone, birthDate, createdBy, createdByRole);
    }
}
