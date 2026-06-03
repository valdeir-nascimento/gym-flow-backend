package br.com.gym.flow.users.domain;

public enum Role {
    STUDENT,
    INSTRUCTOR,
    ADMINISTRATOR;

    public boolean isStudent() {
        return this == STUDENT;
    }

    public boolean isInstructor() {
        return this == INSTRUCTOR;
    }

    public boolean isAdministrator() {
        return this == ADMINISTRATOR;
    }
}
