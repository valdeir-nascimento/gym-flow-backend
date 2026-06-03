package br.com.gym.flow.evolution.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.evolution.application.usecase.GetStudentEvolutionQuery;
import br.com.gym.flow.evolution.application.usecase.GetStudentEvolutionUseCase;
import br.com.gym.flow.evolution.domain.Granularity;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.observability.CustomerContextFilter;
import br.com.gym.flow.shared.observability.MdcRequestFilter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
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

@WebMvcTest(controllers = EvolutionController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class EvolutionControllerTest {

    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetStudentEvolutionUseCase getEvolution;

    private static EvolutionReport emptyReport() {
        return new EvolutionReport(STUDENT_ID, "WEEKLY", Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-06-30T00:00:00Z"), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void givenOwnEvolution_whenGetting_thenReturns200AndMapsQuery() throws Exception {
        // Given
        when(getEvolution.execute(any())).thenReturn(Result.success(emptyReport()));

        // When / Then
        mockMvc.perform(get("/evolution")
                .param("studentId", STUDENT_ID.toString())
                .param("granularity", "MONTHLY")
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.studentId").value(STUDENT_ID.toString()))
            .andExpect(jsonPath("$.frequency").isArray());

        // Then — params + headers mapped into the query
        var captor = ArgumentCaptor.forClass(GetStudentEvolutionQuery.class);
        verify(getEvolution).execute(captor.capture());
        assertThat(captor.getValue().studentId()).isEqualTo(STUDENT_ID);
        assertThat(captor.getValue().granularity()).isEqualTo(Granularity.MONTHLY);
        assertThat(captor.getValue().actorId()).isEqualTo(STUDENT_ID);
        assertThat(captor.getValue().actorRole()).isEqualTo("STUDENT");
    }

    @Test
    void givenNoGranularityParam_whenGetting_thenDefaultsToWeekly() throws Exception {
        // Given
        when(getEvolution.execute(any())).thenReturn(Result.success(emptyReport()));

        // When / Then
        mockMvc.perform(get("/evolution")
                .param("studentId", STUDENT_ID.toString())
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT"))
            .andExpect(status().isOk());

        var captor = ArgumentCaptor.forClass(GetStudentEvolutionQuery.class);
        verify(getEvolution).execute(captor.capture());
        assertThat(captor.getValue().granularity()).isEqualTo(Granularity.WEEKLY);
    }

    @Test
    void givenAnotherStudent_whenGetting_thenReturns403() throws Exception {
        // Given
        when(getEvolution.execute(any())).thenReturn(Result.failWith(ErrorCode.EVOLUTION_NOT_OWNED));

        // When / Then
        mockMvc.perform(get("/evolution")
                .param("studentId", UUID.randomUUID().toString())
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT"))
            .andExpect(status().isForbidden());
    }

    @Test
    void givenPeriodLongerThan24Months_whenGetting_thenReturns422() throws Exception {
        // Given — BUSINESS_RULE category -> 422
        when(getEvolution.execute(any())).thenReturn(Result.failWith(ErrorCode.EVOLUTION_PERIOD_TOO_LONG));

        // When / Then
        mockMvc.perform(get("/evolution")
                .param("studentId", STUDENT_ID.toString())
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void givenInvalidGranularity_whenGetting_thenReturns400AndSkipsUseCase() throws Exception {
        // Given — unknown enum value -> type mismatch -> 400
        // When / Then
        mockMvc.perform(get("/evolution")
                .param("studentId", STUDENT_ID.toString())
                .param("granularity", "DAILY")
                .header("X-User-Id", STUDENT_ID)
                .header("X-User-Role", "STUDENT"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(getEvolution);
    }
}
