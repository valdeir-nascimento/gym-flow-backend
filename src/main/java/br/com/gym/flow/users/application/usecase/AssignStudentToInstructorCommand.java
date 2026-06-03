package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.UserId;

public record AssignStudentToInstructorCommand(UserId studentId, UserId instructorId, UserId createdBy) {}
