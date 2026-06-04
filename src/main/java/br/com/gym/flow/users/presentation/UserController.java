package br.com.gym.flow.users.presentation;

import br.com.gym.flow.shared.domain.ErrorCode;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.usecase.ChangeUserRoleCommand;
import br.com.gym.flow.users.application.usecase.ChangeUserRoleUseCase;
import br.com.gym.flow.users.application.usecase.ChangeUserStatusCommand;
import br.com.gym.flow.users.application.usecase.ChangeUserStatusUseCase;
import br.com.gym.flow.users.application.usecase.GetUserQuery;
import br.com.gym.flow.users.application.usecase.GetUserUseCase;
import br.com.gym.flow.users.application.usecase.ListUsersQuery;
import br.com.gym.flow.users.application.usecase.ListUsersUseCase;
import br.com.gym.flow.users.application.usecase.RegisterInstructorCommand;
import br.com.gym.flow.users.application.usecase.RegisterInstructorUseCase;
import br.com.gym.flow.users.application.usecase.RegisterStudentCommand;
import br.com.gym.flow.users.application.usecase.RegisterStudentUseCase;
import br.com.gym.flow.users.application.usecase.UpdateOwnProfileCommand;
import br.com.gym.flow.users.application.usecase.UpdateOwnProfileUseCase;
import br.com.gym.flow.users.domain.Role;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.UserStatus;
import br.com.gym.flow.users.domain.spi.UserView;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
class UserController {

    private final RegisterStudentUseCase registerStudent;
    private final RegisterInstructorUseCase registerInstructor;
    private final ListUsersUseCase listUsers;
    private final GetUserUseCase getUser;
    private final ChangeUserStatusUseCase changeUserStatus;
    private final ChangeUserRoleUseCase changeUserRole;
    private final UpdateOwnProfileUseCase updateOwnProfile;

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    Result<UserView> registerStudent(@Valid @RequestBody RegisterStudentRequest req, HttpServletResponse response) {
        final Role role;
        try {
            role = req.createdByRole() == null ? null : Role.valueOf(req.createdByRole());
        } catch (IllegalArgumentException ex) {
            return Result.failWith(ErrorCode.INVALID_INPUT, "createdByRole inválido: " + req.createdByRole());
        }
        UserId createdBy = req.createdBy() == null ? null : UserId.of(req.createdBy());
        Result<UserView> result = registerStudent.execute(new RegisterStudentCommand(
            req.name(), req.email(), req.phone(), req.birthDate(), createdBy, role));
        setLocationIfCreated(response, result);
        return result;
    }

    @PostMapping("/instructors")
    @ResponseStatus(HttpStatus.CREATED)
    Result<UserView> registerInstructor(@Valid @RequestBody RegisterInstructorRequest req, HttpServletResponse response) {
        UserId createdBy = req.createdBy() == null ? null : UserId.of(req.createdBy());
        Result<UserView> result = registerInstructor.execute(new RegisterInstructorCommand(
            req.name(), req.email(), req.phone(), req.birthDate(), createdBy));
        setLocationIfCreated(response, result);
        return result;
    }

    // On a successful create, point Location at the new resource (REST 201 contract).
    private static void setLocationIfCreated(HttpServletResponse response, Result<UserView> result) {
        if (result.isSuccess()) {
            response.setHeader(HttpHeaders.LOCATION, "/users/" + result.getOrThrow().id());
        }
    }

    @GetMapping
    Result<Page<UserView>> list(@RequestParam(required = false) Role role,
                                @RequestParam(required = false) UserStatus status,
                                @RequestParam(required = false) String search,
                                Pageable pageable) {
        return listUsers.execute(new ListUsersQuery(role, status, search, pageable));
    }

    @GetMapping("/{id}")
    Result<UserView> getById(@PathVariable UUID id) {
        return getUser.execute(new GetUserQuery(UserId.of(id)));
    }

    @PatchMapping("/{id}/status")
    Result<UserView> changeStatus(@PathVariable UUID id,
                                  @Valid @RequestBody ChangeStatusRequest req,
                                  @RequestHeader("X-User-Id") UUID actorId) {
        final UserStatus target;
        try {
            target = UserStatus.valueOf(req.status());
        } catch (IllegalArgumentException ex) {
            return Result.failWith(ErrorCode.INVALID_INPUT, "status inválido: " + req.status());
        }
        return changeUserStatus.execute(new ChangeUserStatusCommand(UserId.of(id), target, UserId.of(actorId)));
    }

    @PatchMapping("/{id}/role")
    Result<UserView> changeRole(@PathVariable UUID id,
                                @Valid @RequestBody ChangeRoleRequest req,
                                @RequestHeader("X-User-Id") UUID actorId) {
        final Role target;
        try {
            target = Role.valueOf(req.role());
        } catch (IllegalArgumentException ex) {
            return Result.failWith(ErrorCode.INVALID_INPUT, "perfil inválido: " + req.role());
        }
        return changeUserRole.execute(new ChangeUserRoleCommand(UserId.of(id), target, UserId.of(actorId)));
    }

    @GetMapping("/me")
    Result<UserView> me(@RequestHeader("X-User-Id") UUID currentUserId) {
        return getUser.execute(new GetUserQuery(UserId.of(currentUserId)));
    }

    @PatchMapping("/me")
    Result<UserView> updateMe(@RequestHeader("X-User-Id") UUID currentUserId,
                              @Valid @RequestBody UpdateOwnProfileRequest req) {
        return updateOwnProfile.execute(new UpdateOwnProfileCommand(
            UserId.of(currentUserId), req.name(), req.phone(), req.birthDate()));
    }
}
