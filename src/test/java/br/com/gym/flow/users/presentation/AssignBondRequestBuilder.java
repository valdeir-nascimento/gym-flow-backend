package br.com.gym.flow.users.presentation;

import java.util.UUID;

/**
 * Test Data Builder for {@link AssignBondRequest}: a valid assignment payload by
 * default; override only the field under test. Serialized via the ObjectMapper so
 * the payload stays coupled to the real DTO (team rule 2).
 */
public final class AssignBondRequestBuilder {

    private UUID studentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private UUID instructorId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private UUID createdBy = UUID.fromString("33333333-3333-3333-3333-333333333333");

    public static AssignBondRequestBuilder anAssignBondRequest() {
        return new AssignBondRequestBuilder();
    }

    public AssignBondRequestBuilder withStudentId(UUID studentId) {
        this.studentId = studentId;
        return this;
    }

    public AssignBondRequestBuilder withInstructorId(UUID instructorId) {
        this.instructorId = instructorId;
        return this;
    }

    public AssignBondRequestBuilder withCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public AssignBondRequest build() {
        return new AssignBondRequest(studentId, instructorId, createdBy);
    }
}
