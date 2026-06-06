package br.com.gym.flow.users.presentation;

import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.application.usecase.AssignStudentToInstructorCommand;
import br.com.gym.flow.users.application.usecase.AssignStudentToInstructorUseCase;
import br.com.gym.flow.users.application.usecase.ListBondsQuery;
import br.com.gym.flow.users.application.usecase.ListBondsUseCase;
import br.com.gym.flow.users.application.usecase.RemoveBondCommand;
import br.com.gym.flow.users.application.usecase.RemoveBondUseCase;
import br.com.gym.flow.users.application.usecase.TransferBondCommand;
import br.com.gym.flow.users.application.usecase.TransferBondUseCase;
import br.com.gym.flow.users.domain.UserId;
import br.com.gym.flow.users.domain.bond.BondId;
import br.com.gym.flow.users.domain.spi.BondView;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/bonds")
@RequiredArgsConstructor
class BondController implements BondApi {

    private final AssignStudentToInstructorUseCase assignStudentToInstructor;
    private final TransferBondUseCase transferBond;
    private final RemoveBondUseCase removeBond;
    private final ListBondsUseCase listBonds;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public Result<BondView> assign(@Valid @RequestBody AssignBondRequest req, HttpServletResponse response) {
        Result<BondView> result = assignStudentToInstructor.execute(new AssignStudentToInstructorCommand(
            UserId.of(req.studentId()), UserId.of(req.instructorId()), UserId.of(req.createdBy())));
        if (result.isSuccess()) {
            response.setHeader(HttpHeaders.LOCATION, "/users/bonds/" + result.getOrThrow().id());
        }
        return result;
    }

    @PutMapping("/transfer")
    @Override
    public Result<BondView> transfer(@Valid @RequestBody TransferBondRequest req) {
        return transferBond.execute(new TransferBondCommand(
            UserId.of(req.studentId()), UserId.of(req.newInstructorId()), UserId.of(req.actor())));
    }

    @DeleteMapping("/{id}")
    @Override
    public Result<BondView> remove(@PathVariable UUID id,
                            @RequestHeader("X-User-Id") UUID actorId,
                            @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
        boolean restrict = "INSTRUCTOR".equals(actorRole);
        return removeBond.execute(new RemoveBondCommand(BondId.of(id), UserId.of(actorId), restrict));
    }

    @GetMapping
    @Override
    public Result<Page<BondView>> list(@RequestParam(required = false) UUID instructorId,
                                @RequestParam(required = false) UUID studentId,
                                Pageable pageable) {
        UserId instructor = instructorId == null ? null : UserId.of(instructorId);
        UserId student = studentId == null ? null : UserId.of(studentId);
        return listBonds.execute(new ListBondsQuery(instructor, student, pageable));
    }
}
