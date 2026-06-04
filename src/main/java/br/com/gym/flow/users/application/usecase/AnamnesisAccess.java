package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.bond.BondRepository;

import java.util.UUID;

/**
 * Read-access policy for a student's anamnesis (RF-017, RNF-001 §Propriedade):
 * the student themselves, their currently-bonded instructor, or an Administrator.
 */
final class AnamnesisAccess {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    private AnamnesisAccess() {
    }

    static boolean canRead(final BondRepository bonds, final UUID studentId, final UUID actorId, final String actorRole) {
        if (actorId.equals(studentId) || ADMINISTRATOR.equals(actorRole)) {
            return true;
        }
        return bonds.findActiveByStudent(UserId.of(studentId))
            .map(bond -> bond.instructorId().equals(UserId.of(actorId)))
            .orElse(false);
    }
}
