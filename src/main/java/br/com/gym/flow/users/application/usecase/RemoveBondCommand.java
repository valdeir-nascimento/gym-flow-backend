package br.com.gym.flow.users.application.usecase;

import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.bond.BondId;

public record RemoveBondCommand(BondId bondId, UserId actor, boolean restrictToInstructor) {}
