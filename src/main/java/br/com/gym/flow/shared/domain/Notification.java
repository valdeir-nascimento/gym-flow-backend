package br.com.gym.flow.shared.domain;

import java.util.ArrayList;
import java.util.List;

public final class Notification {

    private final List<NotificationError> errors = new ArrayList<>();

    private Notification() {
    }

    public static Notification empty() {
        return new Notification();
    }

    public static Notification ofSingle(ErrorCode code) {
        return ofSingle(code, code.defaultMessage());
    }

    public static Notification ofSingle(ErrorCode code, String message) {
        Notification n = new Notification();
        n.addError(code, message);
        return n;
    }

    public static Notification ofSingle(String code, String message) {
        Notification n = new Notification();
        n.addError(code, message);
        return n;
    }

    public void addError(String code, String message) {
        errors.add(new NotificationError(null, code, message));
    }

    public void addError(String field, String code, String message) {
        errors.add(new NotificationError(field, code, message));
    }

    public void addError(ErrorCode code) {
        addError(code.name(), code.defaultMessage());
    }

    public void addError(ErrorCode code, String message) {
        addError(code.name(), message);
    }

    public void addError(String field, ErrorCode code) {
        addError(field, code.name(), code.defaultMessage());
    }

    public void addError(String field, ErrorCode code, String message) {
        addError(field, code.name(), message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasAnyCode(ErrorCode... candidates) {
        for (NotificationError error : errors) {
            for (ErrorCode candidate : candidates) {
                if (error.code().equals(candidate.name())) return true;
            }
        }
        return false;
    }

    public List<NotificationError> errors() {
        return List.copyOf(errors);
    }

    public void merge(Notification other) {
        errors.addAll(other.errors);
    }
}
