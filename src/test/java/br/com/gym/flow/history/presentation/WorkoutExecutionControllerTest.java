package br.com.gym.flow.history.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.history.application.usecase.RegisterWorkoutExecutionCommand;
import br.com.gym.flow.history.application.usecase.RegisterWorkoutExecutionUseCase;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionItemView;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.observability.CustomerContextFilter;
import br.com.gym.flow.shared.observability.MdcRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkoutExecutionController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class WorkoutExecutionControllerTest {

    private static final UUID EXECUTION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a0");
    private static final UUID TRAINING_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final Instant START = Instant.parse("2026-06-01T11:00:00Z");
    private static final Instant END = Instant.parse("2026-06-01T11:45:00Z");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterWorkoutExecutionUseCase registerExecution;

    private static WorkoutExecutionView view() {
        return new WorkoutExecutionView(EXECUTION_ID, STUDENT_ID, TRAINING_ID, START, END, "felt good",
            List.of(new WorkoutExecutionItemView(EXERCISE_ID, 4, 10, new BigDecimal("40.00"), "ok")),
            Instant.parse("2026-06-01T12:00:00Z"));
    }

    private static ExecutedExerciseRequest validItem() {
        return new ExecutedExerciseRequest(EXERCISE_ID, 4, 10, new BigDecimal("40.00"), "ok");
    }

    private static RegisterWorkoutExecutionRequest request(final List<ExecutedExerciseRequest> items) {
        return new RegisterWorkoutExecutionRequest(START, END, "felt good", items);
    }

    private String json(final Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void givenValidRequest_whenRegistering_thenReturns201BodyHeaderAndMapsCommand() throws Exception {
        // Given
        when(registerExecution.execute(any())).thenReturn(Result.success(view()));

        // When / Then — status + Location + relevant body fields
        mockMvc.perform(post("/trainings/{trainingId}/executions", TRAINING_ID)
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(List.of(validItem())))))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/workout-executions/" + EXECUTION_ID))
            .andExpect(jsonPath("$.id").value(EXECUTION_ID.toString()))
            .andExpect(jsonPath("$.trainingId").value(TRAINING_ID.toString()))
            .andExpect(jsonPath("$.items[0].exerciseId").value(EXERCISE_ID.toString()));

        // Then — path trainingId + header actor + body mapped into the command
        var captor = ArgumentCaptor.forClass(RegisterWorkoutExecutionCommand.class);
        verify(registerExecution).execute(captor.capture());
        assertThat(captor.getValue().trainingId()).isEqualTo(TRAINING_ID);
        assertThat(captor.getValue().actorId()).isEqualTo(STUDENT_ID);
        assertThat(captor.getValue().startedAt()).isEqualTo(START);
        assertThat(captor.getValue().items()).singleElement()
            .satisfies(item -> assertThat(item.exerciseId()).isEqualTo(EXERCISE_ID));
        verifyNoMoreInteractions(registerExecution);
    }

    @Test
    void givenNoItems_whenRegistering_thenReturns400AndSkipsUseCase() throws Exception {
        // Given — @NotEmpty items violated
        // When / Then
        mockMvc.perform(post("/trainings/{trainingId}/executions", TRAINING_ID)
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(List.of()))))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(registerExecution);
    }

    @Test
    void givenTrainingOfAnotherStudent_whenRegistering_thenReturns403() throws Exception {
        // Given
        when(registerExecution.execute(any())).thenReturn(Result.failWith(ErrorCode.EXECUTION_TRAINING_NOT_OWNED));

        // When / Then
        mockMvc.perform(post("/trainings/{trainingId}/executions", TRAINING_ID)
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(List.of(validItem())))))
            .andExpect(status().isForbidden());
    }

    @Test
    void givenFutureDatetime_whenRegistering_thenReturns400() throws Exception {
        // Given — VALIDATION category -> 400
        when(registerExecution.execute(any())).thenReturn(Result.failWith(ErrorCode.EXECUTION_FUTURE_DATETIME));

        // When / Then
        mockMvc.perform(post("/trainings/{trainingId}/executions", TRAINING_ID)
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(List.of(validItem())))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void givenEndBeforeStart_whenRegistering_thenReturns422() throws Exception {
        // Given — BUSINESS_RULE category -> 422
        when(registerExecution.execute(any())).thenReturn(Result.failWith(ErrorCode.EXECUTION_END_BEFORE_START));

        // When / Then
        mockMvc.perform(post("/trainings/{trainingId}/executions", TRAINING_ID)
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(List.of(validItem())))))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void givenDuplicateExecution_whenRegistering_thenReturns409() throws Exception {
        // Given — CONFLICT category -> 409 (idempotency)
        when(registerExecution.execute(any())).thenReturn(Result.failWith(ErrorCode.EXECUTION_ALREADY_REGISTERED));

        // When / Then
        mockMvc.perform(post("/trainings/{trainingId}/executions", TRAINING_ID)
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(List.of(validItem())))))
            .andExpect(status().isConflict());
    }

    @Test
    void givenConcurrentDuplicate_whenUniqueConstraintRejectsInsert_thenReturns409() throws Exception {
        // Given — the idempotency exists-check passed, but the DB UNIQUE rejected
        // the insert at flush (the check-then-insert race). The global handler must
        // translate the integrity violation to 409, not 500.
        when(registerExecution.execute(any()))
            .thenThrow(new DataIntegrityViolationException("uq_workout_executions_key"));

        // When / Then
        mockMvc.perform(post("/trainings/{trainingId}/executions", TRAINING_ID)
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request(List.of(validItem())))))
            .andExpect(status().isConflict());
    }
}
