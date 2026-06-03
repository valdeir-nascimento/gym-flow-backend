package br.com.gym.flow.users.presentation;

import java.util.UUID;

/**
 * Test Data Builder for {@link TransferBondRequest}: a valid transfer payload by
 * default; override only the field under test. Serialized via the ObjectMapper so
 * the payload stays coupled to the real DTO (team rule 2).
 */
public final class TransferBondRequestBuilder {

    private UUID studentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private UUID newInstructorId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private UUID actor = UUID.fromString("33333333-3333-3333-3333-333333333333");

    public static TransferBondRequestBuilder aTransferBondRequest() {
        return new TransferBondRequestBuilder();
    }

    public TransferBondRequestBuilder withStudentId(UUID studentId) {
        this.studentId = studentId;
        return this;
    }

    public TransferBondRequestBuilder withNewInstructorId(UUID newInstructorId) {
        this.newInstructorId = newInstructorId;
        return this;
    }

    public TransferBondRequestBuilder withActor(UUID actor) {
        this.actor = actor;
        return this;
    }

    public TransferBondRequest build() {
        return new TransferBondRequest(studentId, newInstructorId, actor);
    }
}
