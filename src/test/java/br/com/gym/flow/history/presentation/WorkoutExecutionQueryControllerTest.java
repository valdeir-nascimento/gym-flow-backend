package br.com.gym.flow.history.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.history.application.usecase.GetWorkoutExecutionQuery;
import br.com.gym.flow.history.application.usecase.GetWorkoutExecutionUseCase;
import br.com.gym.flow.history.application.usecase.ListWorkoutExecutionsQuery;
import br.com.gym.flow.history.application.usecase.ListWorkoutExecutionsUseCase;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionItemView;
import br.com.gym.flow.history.domain.spi.WorkoutExecutionView;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.observability.CustomerContextFilter;
import br.com.gym.flow.shared.observability.MdcRequestFilter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkoutExecutionQueryController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class WorkoutExecutionQueryControllerTest {

    private static final UUID EXECUTION_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a0");
    private static final UUID TRAINING_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    private static final Instant START = Instant.parse("2026-06-01T11:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListWorkoutExecutionsUseCase listExecutions;
    @MockitoBean
    private GetWorkoutExecutionUseCase getExecution;

    private static WorkoutExecutionView view() {
        return new WorkoutExecutionView(EXECUTION_ID, STUDENT_ID, TRAINING_ID, START, START.plusSeconds(2700),
            "felt good", List.of(new WorkoutExecutionItemView(EXERCISE_ID, 4, 10, new BigDecimal("40.00"), "ok")),
            Instant.parse("2026-06-01T12:00:00Z"));
    }

    @Nested
    class ListHistory {

        @Test
        void givenOwnHistory_whenListing_thenReturns200PagedFixedSortAndMapsQuery() throws Exception {
            // Given
            Page<WorkoutExecutionView> page = new PageImpl<>(List.of(view()), PageRequest.of(0, 20), 1);
            when(listExecutions.execute(any())).thenReturn(Result.success(page));

            // When / Then
            mockMvc.perform(get("/workout-executions")
                    .param("studentId", STUDENT_ID.toString())
                    .param("trainingId", TRAINING_ID.toString())
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(EXECUTION_ID.toString()));

            // Then — params + headers mapped, ordering fixed to startedAt DESC
            var captor = ArgumentCaptor.forClass(ListWorkoutExecutionsQuery.class);
            verify(listExecutions).execute(captor.capture());
            assertThat(captor.getValue().studentId()).isEqualTo(STUDENT_ID);
            assertThat(captor.getValue().trainingId()).isEqualTo(TRAINING_ID);
            assertThat(captor.getValue().actorId()).isEqualTo(STUDENT_ID);
            assertThat(captor.getValue().actorRole()).isEqualTo("STUDENT");
            assertThat(captor.getValue().pageable().getSort().getOrderFor("startedAt").getDirection().isDescending())
                .isTrue();
        }

        @Test
        void givenNoResults_whenListing_thenReturns200EmptyPage() throws Exception {
            // Given
            when(listExecutions.execute(any())).thenReturn(Result.success(Page.empty()));

            // When / Then
            mockMvc.perform(get("/workout-executions")
                    .param("studentId", STUDENT_ID.toString())
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void givenAnotherStudent_whenListing_thenReturns403() throws Exception {
            // Given
            when(listExecutions.execute(any())).thenReturn(Result.failWith(ErrorCode.EXECUTION_NOT_OWNED));

            // When / Then
            mockMvc.perform(get("/workout-executions")
                    .param("studentId", UUID.randomUUID().toString())
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
        }

        @Test
        void givenSizeAboveMax_whenListing_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — RNF-002: size capped at 100
            // When / Then
            mockMvc.perform(get("/workout-executions")
                    .param("studentId", STUDENT_ID.toString())
                    .param("size", "101")
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(listExecutions);
        }
    }

    @Nested
    class GetDetail {

        @Test
        void givenExistingOwnedExecution_whenGetById_thenReturns200WithDetail() throws Exception {
            // Given
            when(getExecution.execute(any())).thenReturn(Result.success(view()));

            // When / Then
            mockMvc.perform(get("/workout-executions/{id}", EXECUTION_ID)
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EXECUTION_ID.toString()))
                .andExpect(jsonPath("$.items[0].exerciseId").value(EXERCISE_ID.toString()));

            var captor = ArgumentCaptor.forClass(GetWorkoutExecutionQuery.class);
            verify(getExecution).execute(captor.capture());
            assertThat(captor.getValue().executionId().value()).isEqualTo(EXECUTION_ID);
            assertThat(captor.getValue().actorId()).isEqualTo(STUDENT_ID);
        }

        @Test
        void givenUnknownExecution_whenGetById_thenReturns404() throws Exception {
            // Given
            when(getExecution.execute(any())).thenReturn(Result.failWith(ErrorCode.EXECUTION_NOT_FOUND));

            // When / Then
            mockMvc.perform(get("/workout-executions/{id}", EXECUTION_ID)
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNotFound());
        }

        @Test
        void givenExecutionOfAnotherStudent_whenGetById_thenReturns403() throws Exception {
            // Given
            when(getExecution.execute(any())).thenReturn(Result.failWith(ErrorCode.EXECUTION_NOT_OWNED));

            // When / Then
            mockMvc.perform(get("/workout-executions/{id}", EXECUTION_ID)
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
        }
    }
}
