package br.com.gym.flow.trainings.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.observability.CustomerContextFilter;
import br.com.gym.flow.shared.observability.MdcRequestFilter;
import br.com.gym.flow.trainings.application.usecase.CreateTrainingCommand;
import br.com.gym.flow.trainings.application.usecase.CreateTrainingUseCase;
import br.com.gym.flow.trainings.application.usecase.GetTrainingUseCase;
import br.com.gym.flow.trainings.application.usecase.UpdateTrainingCommand;
import br.com.gym.flow.trainings.application.usecase.UpdateTrainingUseCase;
import br.com.gym.flow.trainings.domain.TrainingStatus;
import br.com.gym.flow.trainings.domain.spi.TrainingItemView;
import br.com.gym.flow.trainings.domain.spi.TrainingView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TrainingController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class TrainingControllerTest {

    private static final UUID TRAINING_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INSTRUCTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final LocalDate START = LocalDate.of(2026, 6, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 31);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateTrainingUseCase createTraining;
    @MockitoBean
    private UpdateTrainingUseCase updateTraining;
    @MockitoBean
    private GetTrainingUseCase getTraining;

    private static TrainingView view() {
        return new TrainingView(TRAINING_ID, STUDENT_ID, INSTRUCTOR_ID, "Treino A", "Hipertrofia", START, END,
            "ACTIVE", List.of(new TrainingItemView(EXERCISE_ID, 4, 10, new BigDecimal("40.00"), 60)),
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static CreateTrainingRequest request(List<TrainingItemRequest> items) {
        return new CreateTrainingRequest(STUDENT_ID, "Treino A", "Hipertrofia", START, END, items);
    }

    private static TrainingItemRequest validItem() {
        return new TrainingItemRequest(EXERCISE_ID, 4, 10, new BigDecimal("40.00"), 60);
    }

    private String json(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Nested
    class Create {

        @Test
        void givenValidRequest_whenCreating_thenReturns201BodyHeaderAndMapsCommand() throws Exception {
            // Given
            when(createTraining.execute(any())).thenReturn(Result.success(view()));

            // When / Then — status + Location + relevant body fields
            mockMvc.perform(post("/trainings")
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(List.of(validItem())))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/trainings/" + TRAINING_ID))
                .andExpect(jsonPath("$.id").value(TRAINING_ID.toString()))
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].exerciseId").value(EXERCISE_ID.toString()));

            // Then — DTO + headers mapped into the command
            var captor = ArgumentCaptor.forClass(CreateTrainingCommand.class);
            verify(createTraining).execute(captor.capture());
            assertThat(captor.getValue().studentId()).isEqualTo(STUDENT_ID);
            assertThat(captor.getValue().name()).isEqualTo("Treino A");
            assertThat(captor.getValue().instructorId()).isEqualTo(INSTRUCTOR_ID);
            assertThat(captor.getValue().actorRole()).isEqualTo("INSTRUCTOR");
            assertThat(captor.getValue().items()).singleElement()
                .satisfies(item -> assertThat(item.exerciseId()).isEqualTo(EXERCISE_ID));
            verifyNoMoreInteractions(createTraining);
        }

        @Test
        void givenNoItems_whenCreating_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — @NotEmpty items violated (RF-004: a training needs ≥1 exercise)
            // When / Then
            mockMvc.perform(post("/trainings")
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(List.of()))))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(createTraining);
        }

        @Test
        void givenUnknownStudent_whenCreating_thenReturns404() throws Exception {
            // Given
            when(createTraining.execute(any())).thenReturn(Result.failWith(ErrorCode.USER_NOT_FOUND));

            // When / Then
            mockMvc.perform(post("/trainings")
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(List.of(validItem())))))
                .andExpect(status().isNotFound());
        }

        @Test
        void givenInstructorNotLinked_whenCreating_thenReturns403() throws Exception {
            // Given
            when(createTraining.execute(any())).thenReturn(Result.failWith(ErrorCode.TRAINING_INSTRUCTOR_NOT_LINKED));

            // When / Then
            mockMvc.perform(post("/trainings")
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(List.of(validItem())))))
                .andExpect(status().isForbidden());
        }

        @Test
        void givenInactiveExercise_whenCreating_thenReturns422() throws Exception {
            // Given
            when(createTraining.execute(any())).thenReturn(Result.failWith(ErrorCode.TRAINING_INACTIVE_EXERCISE));

            // When / Then — BUSINESS_RULE category -> 422
            mockMvc.perform(post("/trainings")
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(List.of(validItem())))))
                .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void givenOverlappingPeriod_whenCreating_thenReturns422() throws Exception {
            // Given
            when(createTraining.execute(any())).thenReturn(Result.failWith(ErrorCode.TRAINING_OVERLAPPING_PERIOD));

            // When / Then
            mockMvc.perform(post("/trainings")
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(List.of(validItem())))))
                .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    class Update {

        private UpdateTrainingRequest updateRequest() {
            return new UpdateTrainingRequest("Treino B", "Hipertrofia", START, END, List.of(validItem()), null);
        }

        @Test
        void givenValidRequest_whenUpdating_thenReturns200AndMapsCommand() throws Exception {
            // Given
            when(updateTraining.execute(any())).thenReturn(Result.success(view()));

            // When / Then
            mockMvc.perform(put("/trainings/{id}", TRAINING_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TRAINING_ID.toString()));

            // Then — path id + headers + body mapped into the command
            var captor = ArgumentCaptor.forClass(UpdateTrainingCommand.class);
            verify(updateTraining).execute(captor.capture());
            assertThat(captor.getValue().trainingId().value()).isEqualTo(TRAINING_ID);
            assertThat(captor.getValue().name()).isEqualTo("Treino B");
            assertThat(captor.getValue().actorId()).isEqualTo(INSTRUCTOR_ID);
            assertThat(captor.getValue().actorRole()).isEqualTo("INSTRUCTOR");
            assertThat(captor.getValue().items()).singleElement()
                .satisfies(item -> assertThat(item.exerciseId()).isEqualTo(EXERCISE_ID));
            verifyNoMoreInteractions(updateTraining);
        }

        @Test
        void givenUnknownTraining_whenUpdating_thenReturns404() throws Exception {
            // Given
            when(updateTraining.execute(any())).thenReturn(Result.failWith(ErrorCode.TRAINING_NOT_FOUND));

            // When / Then
            mockMvc.perform(put("/trainings/{id}", TRAINING_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(updateRequest())))
                .andExpect(status().isNotFound());
        }

        @Test
        void givenActorWithoutPermission_whenUpdating_thenReturns403() throws Exception {
            // Given
            when(updateTraining.execute(any())).thenReturn(Result.failWith(ErrorCode.TRAINING_NOT_OWNED));

            // When / Then
            mockMvc.perform(put("/trainings/{id}", TRAINING_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(updateRequest())))
                .andExpect(status().isForbidden());
        }

        @Test
        void givenArchivedTraining_whenUpdating_thenReturns409() throws Exception {
            // Given
            when(updateTraining.execute(any())).thenReturn(Result.failWith(ErrorCode.TRAINING_ARCHIVED));

            // When / Then — CONFLICT category -> 409
            mockMvc.perform(put("/trainings/{id}", TRAINING_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(updateRequest())))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    class GetById {

        @Test
        void givenUnknownTraining_whenGetById_thenReturns404() throws Exception {
            // Given
            when(getTraining.execute(any())).thenReturn(Result.failWith(ErrorCode.TRAINING_NOT_FOUND));

            // When / Then
            mockMvc.perform(get("/trainings/{id}", TRAINING_ID))
                .andExpect(status().isNotFound());
        }
    }
}
