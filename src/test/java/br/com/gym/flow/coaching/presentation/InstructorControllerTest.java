package br.com.gym.flow.coaching.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.coaching.application.usecase.GetManagedStudentQuery;
import br.com.gym.flow.coaching.application.usecase.GetManagedStudentUseCase;
import br.com.gym.flow.coaching.application.usecase.ListManagedStudentsQuery;
import br.com.gym.flow.coaching.application.usecase.ListManagedStudentsUseCase;
import br.com.gym.flow.coaching.domain.spi.ManagedStudentView;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(controllers = InstructorController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class InstructorControllerTest {

    private static final UUID INSTRUCTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListManagedStudentsUseCase listManagedStudents;
    @MockitoBean
    private GetManagedStudentUseCase getManagedStudent;

    @Nested
    class ListStudents {

        @Test
        void givenInstructor_whenListing_thenReturns200PagedAndMapsQuery() throws Exception {
            // Given
            var view = new ManagedStudentView(STUDENT_ID, "Ana", "ACTIVE",
                Instant.parse("2026-06-20T10:00:00Z"), 12);
            var page = new PageImpl<>(List.of(view), PageRequest.of(0, 20), 1);
            when(listManagedStudents.execute(any())).thenReturn(Result.success(page));

            // When / Then
            mockMvc.perform(get("/instructor/students")
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studentId").value(STUDENT_ID.toString()))
                .andExpect(jsonPath("$.content[0].totalExecutions").value(12));

            var captor = ArgumentCaptor.forClass(ListManagedStudentsQuery.class);
            verify(listManagedStudents).execute(captor.capture());
            assertThat(captor.getValue().instructorId()).isEqualTo(INSTRUCTOR_ID);
            assertThat(captor.getValue().actorRole()).isEqualTo("INSTRUCTOR");
        }

        @Test
        void givenSizeAboveMax_whenListing_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — size capped at 100
            // When / Then
            mockMvc.perform(get("/instructor/students")
                    .param("size", "101")
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(listManagedStudents);
        }
    }

    @Nested
    class GetStudent {

        @Test
        void givenManagedStudent_whenGetById_thenReturns200WithIndicators() throws Exception {
            // Given
            var report = new EvolutionReport(STUDENT_ID, "WEEKLY", Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-06-30T00:00:00Z"), List.of(), List.of(), List.of(), List.of());
            when(getManagedStudent.execute(any())).thenReturn(Result.success(report));

            // When / Then
            mockMvc.perform(get("/instructor/students/{studentId}", STUDENT_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID.toString()))
                .andExpect(jsonPath("$.frequency").isArray());

            var captor = ArgumentCaptor.forClass(GetManagedStudentQuery.class);
            verify(getManagedStudent).execute(captor.capture());
            assertThat(captor.getValue().studentId()).isEqualTo(STUDENT_ID);
            assertThat(captor.getValue().instructorId()).isEqualTo(INSTRUCTOR_ID);
        }

        @Test
        void givenUnmanagedStudent_whenGetById_thenReturns403() throws Exception {
            // Given
            when(getManagedStudent.execute(any())).thenReturn(Result.failWith(ErrorCode.STUDENT_NOT_MANAGED));

            // When / Then
            mockMvc.perform(get("/instructor/students/{studentId}", UUID.randomUUID())
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isForbidden());
        }
    }
}
