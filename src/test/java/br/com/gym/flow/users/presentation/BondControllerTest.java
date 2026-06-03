package br.com.gym.flow.users.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.observability.CustomerContextFilter;
import br.com.gym.flow.shared.observability.MdcRequestFilter;
import br.com.gym.flow.users.application.usecase.*;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.bond.BondId;
import br.com.gym.flow.users.domain.spi.BondView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static br.com.gym.flow.users.presentation.AssignBondRequestBuilder.anAssignBondRequest;
import static br.com.gym.flow.users.presentation.TransferBondRequestBuilder.aTransferBondRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// See UserControllerTest: exclude the app's custom OncePerRequestFilter @Components so the
// slice context starts without their (missing) dependencies; addFilters = false skips auth.
@WebMvcTest(controllers = BondController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class BondControllerTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTRUCTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID BOND_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssignStudentToInstructorUseCase assignStudentToInstructor;
    @MockitoBean
    private TransferBondUseCase transferBond;
    @MockitoBean
    private RemoveBondUseCase removeBond;
    @MockitoBean
    private ListBondsUseCase listBonds;

    private static BondView activeBond() {
        return new BondView(BOND_ID, STUDENT_ID, INSTRUCTOR_ID, Instant.parse("2026-01-01T00:00:00Z"), null, ACTOR_ID, true);
    }

    @Nested
    class Assign {

        @Test
        void givenValidRequest_whenAssigning_thenReturns201BodyAndMapsCommand() throws Exception {
            // Given
            when(assignStudentToInstructor.execute(any())).thenReturn(Result.success(activeBond()));
            var request = anAssignBondRequest()
                .withStudentId(STUDENT_ID)
                .withInstructorId(INSTRUCTOR_ID)
                .withCreatedBy(ACTOR_ID)
                .build();

            // When / Then — status + every relevant response field
            mockMvc.perform(post("/users/bonds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/users/bonds/" + BOND_ID))
                .andExpect(jsonPath("$.id").value(BOND_ID.toString()))
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID.toString()))
                .andExpect(jsonPath("$.instructorId").value(INSTRUCTOR_ID.toString()))
                .andExpect(jsonPath("$.active").value(true));

            // Then — the DTO was mapped into the command, field by field
            var captor = ArgumentCaptor.forClass(AssignStudentToInstructorCommand.class);
            verify(assignStudentToInstructor).execute(captor.capture());
            assertThat(captor.getValue().studentId()).isEqualTo(UserId.of(STUDENT_ID));
            assertThat(captor.getValue().instructorId()).isEqualTo(UserId.of(INSTRUCTOR_ID));
            assertThat(captor.getValue().createdBy()).isEqualTo(UserId.of(ACTOR_ID));
            verifyNoMoreInteractions(assignStudentToInstructor);
        }

        @Test
        void givenStudentAlreadyBonded_whenAssigning_thenReturns409() throws Exception {
            // Given — valid payload, but the use case rejects it as a business conflict
            when(assignStudentToInstructor.execute(any()))
                .thenReturn(Result.failWith(ErrorCode.BOND_STUDENT_HAS_ACTIVE_INSTRUCTOR));
            var request = anAssignBondRequest().build();

            // When / Then — CONFLICT category -> 409
            mockMvc.perform(post("/users/bonds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }

        @Test
        void givenMissingStudentId_whenAssigning_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — @NotNull studentId violated (null is representable, so build it)
            var request = anAssignBondRequest().withStudentId(null).build();

            // When / Then
            mockMvc.perform(post("/users/bonds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());

            verifyNoInteractions(assignStudentToInstructor);
        }

        @Test
        void givenInvalidUuid_whenAssigning_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — a UUID field carrying a non-UUID string (rule 2 exception: no DTO represents it)
            var malformed = """
                {"studentId":"not-a-uuid","instructorId":"%s","createdBy":"%s"}
                """.formatted(INSTRUCTOR_ID, ACTOR_ID);

            // When / Then — Jackson fails to bind -> 400
            mockMvc.perform(post("/users/bonds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformed))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(assignStudentToInstructor);
        }

        @Test
        void givenMalformedJson_whenAssigning_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — syntactically broken body
            // When / Then
            mockMvc.perform(post("/users/bonds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{"))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(assignStudentToInstructor);
        }
    }

    @Nested
    class Transfer {

        @Test
        void givenValidRequest_whenTransferring_thenReturns200BodyAndMapsCommand() throws Exception {
            // Given
            when(transferBond.execute(any())).thenReturn(Result.success(activeBond()));
            var request = aTransferBondRequest()
                .withStudentId(STUDENT_ID)
                .withNewInstructorId(INSTRUCTOR_ID)
                .withActor(ACTOR_ID)
                .build();

            // When / Then
            mockMvc.perform(put("/users/bonds/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BOND_ID.toString()))
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID.toString()))
                .andExpect(jsonPath("$.active").value(true));

            // Then — DTO mapped into the command
            var captor = ArgumentCaptor.forClass(TransferBondCommand.class);
            verify(transferBond).execute(captor.capture());
            assertThat(captor.getValue().studentId()).isEqualTo(UserId.of(STUDENT_ID));
            assertThat(captor.getValue().newInstructorId()).isEqualTo(UserId.of(INSTRUCTOR_ID));
            assertThat(captor.getValue().actor()).isEqualTo(UserId.of(ACTOR_ID));
            verifyNoMoreInteractions(transferBond);
        }

        @Test
        void givenMissingActor_whenTransferring_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — @NotNull actor violated
            var request = aTransferBondRequest().withActor(null).build();

            // When / Then
            mockMvc.perform(put("/users/bonds/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(transferBond);
        }
    }

    @Nested
    class Remove {

        @Test
        void givenInstructorRole_whenRemoving_thenRestrictsToInstructorAndMapsCommand() throws Exception {
            // Given
            when(removeBond.execute(any())).thenReturn(Result.success(activeBond()));

            // When / Then
            mockMvc.perform(delete("/users/bonds/{id}", BOND_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BOND_ID.toString()));

            // Then — path id + headers mapped into the command, restriction on
            var captor = ArgumentCaptor.forClass(RemoveBondCommand.class);
            verify(removeBond).execute(captor.capture());
            assertThat(captor.getValue().bondId()).isEqualTo(BondId.of(BOND_ID));
            assertThat(captor.getValue().actor()).isEqualTo(UserId.of(ACTOR_ID));
            assertThat(captor.getValue().restrictToInstructor()).isTrue();
            verifyNoMoreInteractions(removeBond);
        }

        @Test
        void givenAdministratorRole_whenRemoving_thenDoesNotRestrict() throws Exception {
            // Given
            when(removeBond.execute(any())).thenReturn(Result.success(activeBond()));

            // When / Then
            mockMvc.perform(delete("/users/bonds/{id}", BOND_ID)
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk());

            // Then — only an instructor actor triggers the restriction
            var captor = ArgumentCaptor.forClass(RemoveBondCommand.class);
            verify(removeBond).execute(captor.capture());
            assertThat(captor.getValue().restrictToInstructor()).isFalse();
            verifyNoMoreInteractions(removeBond);
        }
    }

    @Nested
    class ListBonds {

        @Test
        void givenInstructorIdParam_whenListing_thenBindsItIntoTheQueryAndReturns200() throws Exception {
            // Given
            var page = new PageImpl<>(List.of(activeBond()), PageRequest.of(0, 20), 1);
            when(listBonds.execute(any())).thenReturn(Result.success(page));

            // When / Then — status + page content
            mockMvc.perform(get("/users/bonds").param("instructorId", INSTRUCTOR_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(BOND_ID.toString()));

            // Then — request params bound into the query
            var captor = ArgumentCaptor.forClass(ListBondsQuery.class);
            verify(listBonds).execute(captor.capture());
            assertThat(captor.getValue().instructorId()).isEqualTo(UserId.of(INSTRUCTOR_ID));
            assertThat(captor.getValue().studentId()).isNull();
            verifyNoMoreInteractions(listBonds);
        }
    }
}
