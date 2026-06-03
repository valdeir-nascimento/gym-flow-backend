package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.UserId;
import org.springframework.data.domain.Pageable;

public record ListBondsQuery(UserId instructorId, UserId studentId, Pageable pageable) {

    public static ListBondsQuery byInstructor(UserId instructorId, Pageable pageable) {
        return new ListBondsQuery(instructorId, null, pageable);
    }

    public static ListBondsQuery byStudent(UserId studentId, Pageable pageable) {
        return new ListBondsQuery(null, studentId, pageable);
    }
}
