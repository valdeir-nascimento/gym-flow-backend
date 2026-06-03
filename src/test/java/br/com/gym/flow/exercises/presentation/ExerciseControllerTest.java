package br.com.gym.flow.exercises.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.exercises.application.usecase.DeactivateExerciseUseCase;
import br.com.gym.flow.exercises.application.usecase.GetExerciseUseCase;
import br.com.gym.flow.exercises.application.usecase.ListExercisesQuery;
import br.com.gym.flow.exercises.application.usecase.ListExercisesUseCase;
import br.com.gym.flow.exercises.application.usecase.RegisterExerciseCommand;
import br.com.gym.flow.exercises.application.usecase.RegisterExerciseUseCase;
import br.com.gym.flow.exercises.application.usecase.UpdateExerciseUseCase;
import br.com.gym.flow.exercises.domain.DifficultyLevel;
import br.com.gym.flow.exercises.domain.MuscleGroup;
import br.com.gym.flow.exercises.domain.spi.ExerciseView;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.observability.CustomerContextFilter;
import br.com.gym.flow.shared.observability.MdcRequestFilter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExerciseController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class ExerciseControllerTest {

    private static final UUID EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterExerciseUseCase registerExercise;
    @MockitoBean
    private UpdateExerciseUseCase updateExercise;
    @MockitoBean
    private DeactivateExerciseUseCase deactivateExercise;
    @MockitoBean
    private ListExercisesUseCase listExercises;
    @MockitoBean
    private GetExerciseUseCase getExercise;

    private static ExerciseView view(String status) {
        return new ExerciseView(EXERCISE_ID, "Supino Reto", "CHEST", "desc", "Barra", "INTERMEDIATE",
            null, null, status, ACTOR_ID, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private String json(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Nested
    class Register {

        @Test
        void givenValidRequest_whenRegistering_thenReturns201BodyHeaderAndMapsCommand() throws Exception {
            // Given
            when(registerExercise.execute(any())).thenReturn(Result.success(view("ACTIVE")));
            var request = new RegisterExerciseRequest(
                "Supino Reto", MuscleGroup.CHEST, "desc", "Barra", DifficultyLevel.INTERMEDIATE, null, null);

            // When / Then — status + Location + relevant body fields
            mockMvc.perform(post("/exercises")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/exercises/" + EXERCISE_ID))
                .andExpect(jsonPath("$.id").value(EXERCISE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Supino Reto"))
                .andExpect(jsonPath("$.muscleGroup").value("CHEST"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

            // Then — DTO + headers mapped into the command
            var captor = ArgumentCaptor.forClass(RegisterExerciseCommand.class);
            verify(registerExercise).execute(captor.capture());
            assertThat(captor.getValue().name()).isEqualTo("Supino Reto");
            assertThat(captor.getValue().muscleGroup()).isEqualTo(MuscleGroup.CHEST);
            assertThat(captor.getValue().difficultyLevel()).isEqualTo(DifficultyLevel.INTERMEDIATE);
            assertThat(captor.getValue().createdBy()).isEqualTo(ACTOR_ID);
            assertThat(captor.getValue().actorRole()).isEqualTo("INSTRUCTOR");
            verifyNoMoreInteractions(registerExercise);
        }

        @Test
        void givenDuplicateName_whenRegistering_thenReturns409() throws Exception {
            // Given
            when(registerExercise.execute(any())).thenReturn(Result.failWith(ErrorCode.EXERCISE_NAME_TAKEN));
            var request = new RegisterExerciseRequest(
                "Supino Reto", MuscleGroup.CHEST, "desc", "Barra", DifficultyLevel.INTERMEDIATE, null, null);

            // When / Then — CONFLICT category -> 409
            mockMvc.perform(post("/exercises")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request)))
                .andExpect(status().isConflict());
        }

        @Test
        void givenBlankName_whenRegistering_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — @NotBlank name violated
            var request = new RegisterExerciseRequest(
                "", MuscleGroup.CHEST, "desc", "Barra", DifficultyLevel.INTERMEDIATE, null, null);

            // When / Then
            mockMvc.perform(post("/exercises")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(registerExercise);
        }

        @Test
        void givenInvalidMuscleGroup_whenRegistering_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — a muscle group outside the enum (rule 2 exception: no DTO represents it)
            var malformed = """
                {"name":"Supino Reto","muscleGroup":"WRONG","difficultyLevel":"INTERMEDIATE"}
                """;

            // When / Then — Jackson fails to bind the enum -> 400
            mockMvc.perform(post("/exercises")
                    .header("X-User-Id", ACTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformed))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(registerExercise);
        }
    }

    @Nested
    class Deactivate {

        @Test
        void givenInstructorActor_whenDeactivating_thenReturns403() throws Exception {
            // Given — the use case rejects a non-admin actor
            when(deactivateExercise.execute(any())).thenReturn(Result.failWith(ErrorCode.FORBIDDEN_ROLE));

            // When / Then — FORBIDDEN category -> 403
            mockMvc.perform(patch("/exercises/{id}/deactivate", EXERCISE_ID)
                    .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isForbidden());
        }

        @Test
        void givenAdminActor_whenDeactivating_thenReturns200WithInactiveStatus() throws Exception {
            // Given
            when(deactivateExercise.execute(any())).thenReturn(Result.success(view("INACTIVE")));

            // When / Then
            mockMvc.perform(patch("/exercises/{id}/deactivate", EXERCISE_ID)
                    .header("X-User-Role", "ADMINISTRATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        }
    }

    @Nested
    class GetById {

        @Test
        void givenUnknownExercise_whenGetById_thenReturns404() throws Exception {
            // Given
            when(getExercise.execute(any())).thenReturn(Result.failWith(ErrorCode.EXERCISE_NOT_FOUND));

            // When / Then — NOT_FOUND category -> 404
            mockMvc.perform(get("/exercises/{id}", EXERCISE_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ListCatalog {

        @Test
        void givenFilterParams_whenListing_thenBindsThemIntoTheQueryAndReturns200() throws Exception {
            // Given
            var page = new PageImpl<>(List.of(view("ACTIVE")), PageRequest.of(0, 20), 1);
            when(listExercises.execute(any())).thenReturn(Result.success(page));

            // When / Then
            mockMvc.perform(get("/exercises")
                    .param("muscleGroup", "CHEST")
                    .param("difficultyLevel", "BEGINNER")
                    .param("search", "supino"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(EXERCISE_ID.toString()));

            // Then — request params bound into the query
            var captor = ArgumentCaptor.forClass(ListExercisesQuery.class);
            verify(listExercises).execute(captor.capture());
            assertThat(captor.getValue().muscleGroup()).isEqualTo(MuscleGroup.CHEST);
            assertThat(captor.getValue().difficultyLevel()).isEqualTo(DifficultyLevel.BEGINNER);
            assertThat(captor.getValue().search()).isEqualTo("supino");
            verifyNoMoreInteractions(listExercises);
        }
    }
}
