package br.com.gym.flow.coaching.application.usecase;

import br.com.gym.flow.coaching.domain.spi.ManagedStudentView;
import br.com.gym.flow.evolution.domain.spi.StudentActivitySummary;
import br.com.gym.flow.evolution.domain.spi.StudentProgressDirectory;
import br.com.gym.flow.shared.application.QueryUseCase;
import br.com.gym.flow.shared.domain.Result;
import br.com.gym.flow.users.domain.spi.TeacherStudentDirectory;
import br.com.gym.flow.users.domain.spi.UserDirectory;
import br.com.gym.flow.users.domain.spi.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListManagedStudentsUseCase implements QueryUseCase<ListManagedStudentsQuery, Page<ManagedStudentView>> {

    private static final String ADMINISTRATOR = "ADMINISTRATOR";

    // Most recent activity first; students without executions go last, then by name.
    private static final Comparator<ManagedStudentView> BY_LAST_ACTIVITY_THEN_NAME =
        Comparator.comparing(ManagedStudentView::lastExecutionAt,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(ManagedStudentView::name, Comparator.nullsLast(Comparator.naturalOrder()));

    private final UserDirectory users;
    private final TeacherStudentDirectory bonds;
    private final StudentProgressDirectory progress;

    @Override
    public Result<Page<ManagedStudentView>> execute(final ListManagedStudentsQuery query) {
        List<UserView> students = resolveStudents(query);
        if (students.isEmpty()) {
            return Result.success(new PageImpl<>(List.of(), query.pageable(), 0));
        }

        Map<UUID, StudentActivitySummary> summaries = progress
            .summariesOf(students.stream().map(UserView::id).toList()).stream()
            .collect(Collectors.toMap(StudentActivitySummary::studentId, Function.identity()));

        List<ManagedStudentView> all = students.stream()
            .map(student -> toView(student, summaries.get(student.id())))
            .sorted(BY_LAST_ACTIVITY_THEN_NAME)
            .toList();

        return Result.success(paginate(all, query.pageable()));
    }

    private List<UserView> resolveStudents(final ListManagedStudentsQuery query) {
        if (ADMINISTRATOR.equals(query.actorRole())) {
            return users.findStudents();
        }
        List<UUID> studentIds = bonds.activeStudentIdsOf(query.instructorId());
        return studentIds.isEmpty() ? List.of() : users.findByIds(studentIds);
    }

    private static ManagedStudentView toView(final UserView student, final StudentActivitySummary summary) {
        Instant lastExecutionAt = summary != null ? summary.lastExecutionAt() : null;
        long total = summary != null ? summary.totalExecutions() : 0L;
        return new ManagedStudentView(student.id(), student.name(), student.status(), lastExecutionAt, total);
    }

    private static Page<ManagedStudentView> paginate(final List<ManagedStudentView> all, final Pageable pageable) {
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }
}
