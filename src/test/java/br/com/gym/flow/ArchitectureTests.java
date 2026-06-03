package br.com.gym.flow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ArchitectureTests {

    private static final ApplicationModules MODULES =
        ApplicationModules.of(GymFlowBackendApplication.class);

    @Test
    void verifyModuleBoundaries() {
        MODULES.verify();
    }

    @Test
    void writeModuleDocumentation() {
        new Documenter(MODULES)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
    }
}
