package br.com.gym.flow.coaching.presentation;

import br.com.gym.flow.coaching.application.usecase.GetManagedStudentQuery;
import br.com.gym.flow.coaching.application.usecase.GetManagedStudentUseCase;
import br.com.gym.flow.coaching.application.usecase.ListManagedStudentsQuery;
import br.com.gym.flow.coaching.application.usecase.ListManagedStudentsUseCase;
import br.com.gym.flow.coaching.domain.spi.ManagedStudentView;
import br.com.gym.flow.evolution.domain.spi.EvolutionReport;
import br.com.gym.flow.shared.domain.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Professor's accompaniment of their students (RF-010). The list is ordered by
 * the use case (most recent activity first, then name), so the client controls
 * only page/size (size capped at 100). Authorization is by student↔instructor
 * bond; the acting professor comes from the {@code X-User-Id} header.
 */
@RestController
@RequestMapping("/instructor/students")
@RequiredArgsConstructor
class InstructorController {

    private final ListManagedStudentsUseCase listManagedStudents;
    private final GetManagedStudentUseCase getManagedStudent;

    @GetMapping
    Result<Page<ManagedStudentView>> list(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestHeader("X-User-Id") UUID instructorId,
        @RequestHeader("X-User-Role") String actorRole
    ) {
        return listManagedStudents.execute(
            new ListManagedStudentsQuery(instructorId, actorRole, PageRequest.of(page, size)));
    }

    @GetMapping("/{studentId}")
    Result<EvolutionReport> getOne(
        @PathVariable UUID studentId,
        @RequestHeader("X-User-Id") UUID instructorId,
        @RequestHeader("X-User-Role") String actorRole
    ) {
        return getManagedStudent.execute(new GetManagedStudentQuery(studentId, instructorId, actorRole));
    }
}
