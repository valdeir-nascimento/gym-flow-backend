package br.com.gym.flow.users.presentation;

import br.com.gym.flow.api.security.JwtAuthenticationFilter;
import br.com.gym.flow.api.support.ratelimit.RateLimitFilter;
import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.shared.observability.CustomerContextFilter;
import br.com.gym.flow.shared.observability.MdcRequestFilter;
import br.com.gym.flow.users.application.usecase.*;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.domain.spi.UserView;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static br.com.gym.flow.users.presentation.RegisterStudentRequestBuilder.aRegisterStudentRequest;
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

@WebMvcTest(controllers = UserController.class, excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = {JwtAuthenticationFilter.class, RateLimitFilter.class, MdcRequestFilter.class, CustomerContextFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterStudentUseCase registerStudent;
    @MockitoBean
    private RegisterInstructorUseCase registerInstructor;
    @MockitoBean
    private ListUsersUseCase listUsers;
    @MockitoBean
    private GetUserUseCase getUser;
    @MockitoBean
    private ChangeUserStatusUseCase changeUserStatus;
    @MockitoBean
    private UpdateOwnProfileUseCase updateOwnProfile;

    private static UserView studentView() {
        return new UserView(
            USER_ID, "Maria Silva", "maria@example.com", "+5511912345678",
            LocalDate.of(2000, 1, 1), "STUDENT", "ACTIVE", ADMIN_ID,
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Nested
    class RegisterStudent {

        @Test
        void givenValidRequest_whenRegistering_thenReturns201BodyAndMapsCommand() throws Exception {
            // Given
            when(registerStudent.execute(any())).thenReturn(Result.success(studentView()));
            var request = aRegisterStudentRequest()
                .withName("Maria Silva")
                .withEmail("maria@example.com")
                .withCreatedBy(ADMIN_ID)
                .withCreatedByRole("ADMINISTRATOR")
                .build();

            // When / Then — status + every relevant response field (ResultBodyAdvice unwraps Success -> UserView)
            mockMvc.perform(post("/users/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/users/" + USER_ID))
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

            // Then — the controller mapped the DTO into the command, field by field
            var captor = ArgumentCaptor.forClass(RegisterStudentCommand.class);
            verify(registerStudent).execute(captor.capture());
            var command = captor.getValue();
            assertThat(command.name()).isEqualTo("Maria Silva");
            assertThat(command.email()).isEqualTo("maria@example.com");
            assertThat(command.phone()).isEqualTo("+5511912345678");
            assertThat(command.birthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
            assertThat(command.createdBy()).isEqualTo(UserId.of(ADMIN_ID));
            assertThat(command.createdByRole()).isEqualTo(Role.ADMINISTRATOR);
            verifyNoMoreInteractions(registerStudent);
        }

        @Test
        void givenEmailAlreadyTaken_whenRegistering_thenReturns409() throws Exception {
            // Given — valid payload, but the use case rejects it as a business conflict
            when(registerStudent.execute(any())).thenReturn(Result.failWith(ErrorCode.USER_EMAIL_TAKEN));
            var request = aRegisterStudentRequest().build();

            // When / Then — CONFLICT category -> 409
            mockMvc.perform(post("/users/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        }

        @Test
        void givenBlankName_whenRegistering_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — @NotBlank name violated (representable by the DTO, so build it)
            var request = aRegisterStudentRequest().withName("").build();

            // When / Then
            mockMvc.perform(post("/users/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());

            // Then — validation rejected it before the use case
            verifyNoInteractions(registerStudent);
        }

        @Test
        void givenInvalidCreatedByUuid_whenRegistering_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — a UUID field carrying a non-UUID string: no DTO represents it, so inline JSON (rule 2 exception)
            var malformed = """
                {"name":"Maria Silva","email":"maria@example.com","phone":"+5511912345678",
                 "birthDate":"2000-01-01","createdBy":"not-a-uuid"}
                """;

            // When / Then — Jackson fails to bind -> 400
            mockMvc.perform(post("/users/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformed))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(registerStudent);
        }

        @Test
        void givenMalformedJson_whenRegistering_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — syntactically broken body (rule 2 exception: nothing to serialize)
            // When / Then
            mockMvc.perform(post("/users/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{"))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(registerStudent);
        }

        @Test
        void givenInvalidCreatedByRole_whenRegistering_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — a role outside the enum; the controller must reject it as a validation error, not 500
            var request = aRegisterStudentRequest()
                .withCreatedBy(ADMIN_ID)
                .withCreatedByRole("WIZARD")
                .build();

            // When / Then — VALIDATION category -> 400
            mockMvc.perform(post("/users/students")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(registerStudent);
        }
    }

    @Nested
    class GetById {

        @Test
        void givenExistingUser_whenGetById_thenReturns200WithFullBody() throws Exception {
            // Given
            when(getUser.execute(any())).thenReturn(Result.success(studentView()));

            // When / Then — status + every relevant field
            mockMvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

            // Then — the path id was mapped into the query
            var captor = ArgumentCaptor.forClass(GetUserQuery.class);
            verify(getUser).execute(captor.capture());
            assertThat(captor.getValue().userId()).isEqualTo(UserId.of(USER_ID));
            verifyNoMoreInteractions(getUser);
        }

        @Test
        void givenUnknownUser_whenGetById_thenReturns404() throws Exception {
            // Given
            when(getUser.execute(any())).thenReturn(Result.failWith(ErrorCode.USER_NOT_FOUND));

            // When / Then — NOT_FOUND category -> 404
            mockMvc.perform(get("/users/{id}", USER_ID))
                .andExpect(status().isNotFound());
        }

        @Test
        void givenMalformedUuid_whenGetById_thenReturns400() throws Exception {
            // Given / When / Then — a path segment that is not a UUID is rejected at binding
            mockMvc.perform(get("/users/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(getUser);
        }
    }

    @Nested
    class ChangeStatus {

        @Test
        void givenValidStatus_whenChanging_thenReturns200AndMapsCommand() throws Exception {
            // Given
            when(changeUserStatus.execute(any())).thenReturn(Result.success(studentView()));
            var request = new ChangeStatusRequest("ACTIVE");

            // When / Then
            mockMvc.perform(patch("/users/{id}/status", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

            // Then — path id + body status mapped into the command
            var captor = ArgumentCaptor.forClass(ChangeUserStatusCommand.class);
            verify(changeUserStatus).execute(captor.capture());
            assertThat(captor.getValue().userId()).isEqualTo(UserId.of(USER_ID));
            assertThat(captor.getValue().targetStatus()).isEqualTo(UserStatus.ACTIVE);
            verifyNoMoreInteractions(changeUserStatus);
        }

        @Test
        void givenForbiddenTransition_whenChanging_thenReturns422() throws Exception {
            // Given
            when(changeUserStatus.execute(any()))
                .thenReturn(Result.failWith(ErrorCode.INVALID_USER_STATUS_TRANSITION));
            var request = new ChangeStatusRequest("BLOCKED");

            // When / Then — BUSINESS_RULE category -> 422
            mockMvc.perform(patch("/users/{id}/status", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
        }

        @Test
        void givenBlankStatus_whenChanging_thenReturns400() throws Exception {
            // Given — @NotBlank status violated
            var request = new ChangeStatusRequest("");

            // When / Then
            mockMvc.perform(patch("/users/{id}/status", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(changeUserStatus);
        }

        @Test
        void givenStatusOutsideEnum_whenChanging_thenReturns400AndSkipsUseCase() throws Exception {
            // Given — a status value the enum does not define; must be 400 (validation), not 500
            var request = new ChangeStatusRequest("BOGUS");

            // When / Then — VALIDATION category -> 400
            mockMvc.perform(patch("/users/{id}/status", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(changeUserStatus);
        }
    }

    @Nested
    class Me {

        @Test
        void givenUserIdHeader_whenGetMe_thenResolvesCurrentUserFromHeader() throws Exception {
            // Given
            when(getUser.execute(any())).thenReturn(Result.success(studentView()));

            // When / Then
            mockMvc.perform(get("/users/me").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()));

            // Then — the header was mapped into the query
            var captor = ArgumentCaptor.forClass(GetUserQuery.class);
            verify(getUser).execute(captor.capture());
            assertThat(captor.getValue().userId()).isEqualTo(UserId.of(USER_ID));
            verifyNoMoreInteractions(getUser);
        }
    }

    @Nested
    class ListUsers {

        @Test
        void givenFilterParams_whenListing_thenBindsThemIntoTheQueryAndReturns200() throws Exception {
            // Given
            var page = new PageImpl<>(List.of(studentView()), PageRequest.of(0, 20), 1);
            when(listUsers.execute(any())).thenReturn(Result.success(page));

            // When / Then — status + page content
            mockMvc.perform(get("/users")
                    .param("role", "STUDENT")
                    .param("status", "ACTIVE")
                    .param("search", "ma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(USER_ID.toString()));

            // Then — request params bound into the query
            var captor = ArgumentCaptor.forClass(ListUsersQuery.class);
            verify(listUsers).execute(captor.capture());
            assertThat(captor.getValue().role()).isEqualTo(Role.STUDENT);
            assertThat(captor.getValue().status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(captor.getValue().search()).isEqualTo("ma");
            verifyNoMoreInteractions(listUsers);
        }
    }
}
