package br.com.gym.flow.exercises.application.usecase;

final class CatalogRoles {

    static final String ADMINISTRATOR = "ADMINISTRATOR";
    static final String INSTRUCTOR = "INSTRUCTOR";

    private CatalogRoles() {
    }

    static boolean canManage(String role) {
        return ADMINISTRATOR.equals(role) || INSTRUCTOR.equals(role);
    }

    static boolean canDeactivate(String role) {
        return ADMINISTRATOR.equals(role);
    }
}
