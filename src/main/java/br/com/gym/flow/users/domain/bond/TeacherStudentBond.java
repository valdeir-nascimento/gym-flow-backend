package br.com.gym.flow.users.domain.bond;

import br.com.gym.flow.shared.domain.AggregateRoot;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.events.BondCreated;
import br.com.gym.flow.users.events.BondEnded;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Clock;
import java.time.Instant;

@Getter
@Accessors(fluent = true)
public class TeacherStudentBond extends AggregateRoot<BondId> {

    private final UserId studentId;
    private final UserId instructorId;
    private final Instant startedAt;
    private Instant endedAt;
    private final UserId createdBy;

    private TeacherStudentBond(BondId id,
                               UserId studentId,
                               UserId instructorId,
                               Instant startedAt,
                               Instant endedAt,
                               UserId createdBy) {
        super(id);
        this.studentId = studentId;
        this.instructorId = instructorId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.createdBy = createdBy;
    }

    public static TeacherStudentBond hydrate(BondId id,
                                             UserId studentId,
                                             UserId instructorId,
                                             Instant startedAt,
                                             Instant endedAt,
                                             UserId createdBy) {
        return new TeacherStudentBond(id, studentId, instructorId, startedAt, endedAt, createdBy);
    }

    public static Result<TeacherStudentBond> open(UserId studentId,
                                                  UserId instructorId,
                                                  UserId createdBy,
                                                  Clock clock) {
        if (studentId == null || instructorId == null || createdBy == null) {
            return Result.failWith(ErrorCode.INVALID_INPUT);
        }
        if (studentId.equals(instructorId)) {
            return Result.failWith(ErrorCode.INVALID_INPUT,
                "student e instructor não podem ser o mesmo usuário");
        }
        Instant now = Instant.now(clock);
        TeacherStudentBond bond = new TeacherStudentBond(
            BondId.newId(), studentId, instructorId, now, null, createdBy);
        bond.registerEvent(BondCreated.of(
            bond.id().value(), studentId.value(), instructorId.value(), now));
        return Result.success(bond);
    }

    public Result<Void> close(Clock clock) {
        if (this.endedAt != null) {
            return Result.failWith(ErrorCode.BOND_NOT_FOUND, "vínculo já encerrado");
        }
        Instant now = Instant.now(clock);
        this.endedAt = now;
        registerEvent(BondEnded.of(id().value(), studentId.value(), instructorId.value(), now));
        return Result.ok();
    }

    public boolean isActive() { return endedAt == null; }
}
