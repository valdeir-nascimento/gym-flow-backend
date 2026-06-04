package br.com.gym.flow.users.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.observability.CustomerContextFilter;
import br.com.gym.flow.shared.observability.MdcRequestFilter;
import br.com.gym.flow.users.application.usecase.GetLatestAnamnesisUseCase;
import br.com.gym.flow.users.application.usecase.GrantHealthConsentUseCase;
import br.com.gym.flow.users.application.usecase.ListAnamnesisHistoryUseCase;
import br.com.gym.flow.users.application.usecase.RegisterAnamnesisCommand;
import br.com.gym.flow.users.application.usecase.RegisterAnamnesisUseCase;
import br.com.gym.flow.users.domain.spi.AnamnesisView;
import br.com.gym.flow.users.domain.spi.HealthConsentView;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnamnesisController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AnamnesisControllerTest {

    private static final UUID ANAMNESIS_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a9");
    private static final UUID STUDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID INSTRUCTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EXERCISE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterAnamnesisUseCase registerAnamnesis;
    @MockitoBean
    private GetLatestAnamnesisUseCase getLatestAnamnesis;
    @MockitoBean
    private ListAnamnesisHistoryUseCase listAnamnesisHistory;
    @MockitoBean
    private GrantHealthConsentUseCase grantHealthConsent;

    private static AnamnesisView view() {
        return new AnamnesisView(ANAMNESIS_ID, STUDENT_ID, 1, new BigDecimal("80.00"), 175,
            "Hipertrofia", "iniciante", List.of("lesão"), List.of("hipertensão"), List.of(EXERCISE_ID),
            "obs", INSTRUCTOR_ID, Instant.parse("2026-06-01T12:00:00Z"));
    }

    private static RegisterAnamnesisRequest request(final BigDecimal weight, final String objectives) {
        return new RegisterAnamnesisRequest(weight, 175, objectives, "iniciante",
            List.of("lesão"), List.of("hipertensão"), List.of(EXERCISE_ID), "obs");
    }

    private String json(final Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Nested
    class Register {

        @Test
        void givenValidRequest_whenRegistering_thenReturns201BodyHeaderAndMapsCommand() throws Exception {
            // Given
            when(registerAnamnesis.execute(any())).thenReturn(Result.success(view()));

            // When / Then
            mockMvc.perform(post("/users/{studentId}/anamnesis", STUDENT_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(new BigDecimal("80.00"), "Hipertrofia"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/users/" + STUDENT_ID + "/anamnesis/" + ANAMNESIS_ID))
                .andExpect(jsonPath("$.id").value(ANAMNESIS_ID.toString()))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.contraindications[0]").value(EXERCISE_ID.toString()));

            // Then — path student + headers + body mapped into the command
            var captor = ArgumentCaptor.forClass(RegisterAnamnesisCommand.class);
            verify(registerAnamnesis).execute(captor.capture());
            assertThat(captor.getValue().studentId()).isEqualTo(STUDENT_ID);
            assertThat(captor.getValue().actorId()).isEqualTo(INSTRUCTOR_ID);
            assertThat(captor.getValue().actorRole()).isEqualTo("INSTRUCTOR");
            assertThat(captor.getValue().weightKg()).isEqualByComparingTo("80.00");
        }

        @Test
        void givenNoConsent_whenRegistering_thenReturns403() throws Exception {
            // Given
            when(registerAnamnesis.execute(any())).thenReturn(Result.failWith(ErrorCode.LGPD_CONSENT_REQUIRED));

            // When / Then
            mockMvc.perform(post("/users/{studentId}/anamnesis", STUDENT_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(new BigDecimal("80.00"), "Hipertrofia"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errors[0].code").value("LGPD_CONSENT_REQUIRED"));
        }

        @Test
        void givenImplausibleWeight_whenRegistering_thenReturns422() throws Exception {
            // Given — BUSINESS_RULE -> 422
            when(registerAnamnesis.execute(any())).thenReturn(Result.failWith(ErrorCode.ANAMNESIS_IMPLAUSIBLE_WEIGHT));

            // When / Then
            mockMvc.perform(post("/users/{studentId}/anamnesis", STUDENT_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(new BigDecimal("80.00"), "Hipertrofia"))))
                .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void givenNotBonded_whenRegistering_thenReturns403() throws Exception {
            // Given
            when(registerAnamnesis.execute(any())).thenReturn(Result.failWith(ErrorCode.STUDENT_NOT_MANAGED));

            // When / Then
            mockMvc.perform(post("/users/{studentId}/anamnesis", STUDENT_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(new BigDecimal("80.00"), "Hipertrofia"))))
                .andExpect(status().isForbidden());
        }

        @Test
        void givenBlankObjectives_whenRegistering_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — @NotBlank objectives violated
            // When / Then
            mockMvc.perform(post("/users/{studentId}/anamnesis", STUDENT_ID)
                    .header("X-User-Id", INSTRUCTOR_ID)
                    .header("X-User-Role", "INSTRUCTOR")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request(new BigDecimal("80.00"), "  "))))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(registerAnamnesis);
        }
    }

    @Nested
    class Read {

        @Test
        void givenExisting_whenGetLatest_thenReturns200() throws Exception {
            // Given
            when(getLatestAnamnesis.execute(any())).thenReturn(Result.success(view()));

            // When / Then
            mockMvc.perform(get("/users/{studentId}/anamnesis", STUDENT_ID)
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ANAMNESIS_ID.toString()));
        }

        @Test
        void givenNone_whenGetLatest_thenReturns404() throws Exception {
            // Given
            when(getLatestAnamnesis.execute(any())).thenReturn(Result.failWith(ErrorCode.ANAMNESIS_NOT_FOUND));

            // When / Then
            mockMvc.perform(get("/users/{studentId}/anamnesis", STUDENT_ID)
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNotFound());
        }

        @Test
        void givenOtherStudent_whenGetHistory_thenReturns403() throws Exception {
            // Given
            when(listAnamnesisHistory.execute(any())).thenReturn(Result.failWith(ErrorCode.ANAMNESIS_NOT_OWNED));

            // When / Then
            mockMvc.perform(get("/users/{studentId}/anamnesis/history", STUDENT_ID)
                    .header("X-User-Id", UUID.randomUUID())
                    .header("X-User-Role", "INSTRUCTOR"))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    class GrantConsent {

        @Test
        void givenStudent_whenGrantingConsent_thenReturns201() throws Exception {
            // Given
            var consent = new HealthConsentView(UUID.randomUUID(), STUDENT_ID, STUDENT_ID,
                Instant.parse("2026-06-01T12:00:00Z"));
            when(grantHealthConsent.execute(any())).thenReturn(Result.success(consent));

            // When / Then
            mockMvc.perform(post("/users/{studentId}/health-consent", STUDENT_ID)
                    .header("X-User-Id", STUDENT_ID)
                    .header("X-User-Role", "STUDENT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID.toString()));
        }
    }
}
